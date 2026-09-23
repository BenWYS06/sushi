package com.sushishop.ai;

import com.sushishop.ai.dto.AiChatMessageResponse;
import com.sushishop.ai.dto.AiChatSessionResponse;
import com.sushishop.ai.dto.AiChatTurnResponse;
import com.sushishop.ai.dto.AiSourceResponse;
import com.sushishop.order.dto.response.OrderResponse;
import com.sushishop.shared.exception.core.AiServiceUnavailableException;
import com.sushishop.shared.exception.core.NotFoundException;
import com.sushishop.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("ai")
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            You are the Sushi Shop shopping assistant.
            Use the supplied shop knowledge for general questions and use tools for live catalog,
            promotion and authenticated order data. Never invent prices, availability, ingredients,
            allergens, discounts, delivery times or shop policies.

            Important safety rules:
            - An order draft is not a real order and is not a payment.
            - Never say an order is placed until the customer calls the separate confirm endpoint.
            - You have no payment tool and cannot charge a customer.
            - Ask for missing delivery details instead of guessing them.
            - Keep answers brief and practical
            """;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiOrderDraftService draftService;
    private final AiRagService ragService;
    private final AiTools aiTools;
    private final ChatClient chatClient;
    private final int historyLimit;

    public AiChatService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            UserRepository userRepository,
            AiOrderDraftService draftService,
            AiRagService ragService,
            AiTools aiTools,
            ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.history-limit:20}") int historyLimit
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.draftService = draftService;
        this.ragService = ragService;
        this.aiTools = aiTools;
        this.chatClient = chatClientBuilder.build();
        this.historyLimit = historyLimit;
    }

    public AiChatSessionResponse createSession(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        var session = sessionRepository.save(ChatSession.builder().user(user).build());
        return new AiChatSessionResponse(session.getId(), session.getCreatedAt(), List.of(), null);
    }

    public AiChatSessionResponse getSession(UUID sessionId, String email) {
        var session = requireOwnedSession(sessionId, email);
        var messages = recentMessages(sessionId, 50).stream()
                .map(this::toMessageResponse)
                .toList();
        var draft = draftService.findBySession(sessionId).orElse(null);
        return new AiChatSessionResponse(session.getId(), session.getCreatedAt(), messages, draft);
    }

    public AiChatTurnResponse sendMessage(UUID sessionId, String email, String text) {
        var session = requireOwnedSession(sessionId, email);
        var previousMessages = recentMessages(sessionId, historyLimit);
        saveMessage(session, ChatRole.USER, text);

        try {
            List<AiSourceResponse> sources = ragService.search(text);
            String answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserPrompt(previousMessages, sources, text))
                    .tools(aiTools)
                    .toolContext(Map.of(
                            AiTools.USER_EMAIL, email,
                            AiTools.SESSION_ID, sessionId))
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("AI model returned an empty answer");
            }

            saveMessage(session, ChatRole.ASSISTANT, answer);
            return new AiChatTurnResponse(
                    sessionId, answer, sources, draftService.findBySession(sessionId).orElse(null));
        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("AI agent failed for session {}", sessionId, ex);
            throw new AiServiceUnavailableException(
                    "AI assistant is unavailable. Check Ollama and try again.");
        }
    }

    public OrderResponse confirmOrder(UUID sessionId, String email) {
        requireOwnedSession(sessionId, email);
        return draftService.confirm(sessionId, email);
    }

    private ChatSession requireOwnedSession(UUID sessionId, String email) {
        return sessionRepository.findByIdAndUserEmail(sessionId, email)
                .orElseThrow(() -> new NotFoundException("AI chat session not found"));
    }

    private ChatMessage saveMessage(ChatSession session, ChatRole role, String content) {
        return messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .build());
    }

    private List<ChatMessage> recentMessages(UUID sessionId, int limit) {
        var newestFirst = new ArrayList<>(messageRepository.findBySessionIdOrderByIdDesc(
                sessionId, PageRequest.of(0, limit)));
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    private String buildUserPrompt(List<ChatMessage> history,
                                   List<AiSourceResponse> sources,
                                   String currentMessage) {
        String conversation = history.isEmpty()
                ? "No previous messages."
                : history.stream()
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));

        String knowledge = sources.isEmpty()
                ? "No matching knowledge documents were found. Use a live tool when appropriate."
                : sources.stream()
                .map(source -> "[%s] %s\n%s".formatted(
                        source.type(), source.title(), source.content()))
                .collect(Collectors.joining("\n\n---\n\n"));

        return """
                SHOP KNOWLEDGE:
                %s

                CONVERSATION HISTORY:
                %s

                CURRENT USER MESSAGE:
                %s
                """.formatted(knowledge, conversation, currentMessage);
    }

    private AiChatMessageResponse toMessageResponse(ChatMessage message) {
        return new AiChatMessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
