package com.sushishop.ai;

import com.sushishop.ai.dto.AiAnswerResponse;
import com.sushishop.ai.dto.AiSourceResponse;
import com.sushishop.shared.exception.core.AiServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("ai")
public class AiRagService {

    private static final String SYSTEM_PROMPT = """
            You are the Sushi Shop knowledge assistant.
            Answer only from the supplied context. If the context does not contain the answer,
            say that the information is not configured and suggest contacting the shop.
            Never invent prices, discounts, allergens, ingredients, delivery times or policies.
            Customer reviews are opinions, not verified product facts.
            Keep the answer brief and practical.
            """;

    private final VectorStore vectorStore;
    private final AiDocumentService documentService;
    private final ChatClient chatClient;
    private final int topK;
    private final double similarityThreshold;

    public AiRagService(
            VectorStore vectorStore,
            AiDocumentService documentService,
            ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.top-k:6}") int topK,
            @Value("${app.ai.similarity-threshold:0.4}") double similarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.documentService = documentService;
        this.chatClient = chatClientBuilder.build();
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<AiSourceResponse> search(String question) {
        try {
            return findCurrentDocuments(question).stream()
                    .map(this::toSource)
                    .toList();
        } catch (RuntimeException ex) {
            log.error("AI knowledge search failed", ex);
            throw new AiServiceUnavailableException(
                    "AI search is unavailable. Check Ollama and pgvector, then try again."
            );
        }
    }


    private List<RetrievedDocument> findCurrentDocuments(String question) {
        var request = SearchRequest.builder()
                // nomic-embed-text uses different prefixes for indexed documents and queries.
                .query("search_query: " + question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(indexed -> documentService.refresh(indexed)
                        .map(current -> new RetrievedDocument(current, indexed.getScore())))
                .flatMap(java.util.Optional::stream)  // remove empty Optional 
                .toList();
    }

    private AiSourceResponse toSource(RetrievedDocument retrieved) {
        Document document = retrieved.document();
        var metadata = document.getMetadata();
        return new AiSourceResponse(
                String.valueOf(metadata.get("type")),
                String.valueOf(metadata.get("sourceId")),
                String.valueOf(metadata.get("title")),
                String.valueOf(metadata.get("url")),
                retrieved.score(),
                cleanText(document)
        );
    }

    private String cleanText(Document document) {
        String text = document.getText();
        if (text == null) {
            return "";
        }
        return text.startsWith("search_document: ")
                ? text.substring("search_document: ".length())
                : text;
    }

    private record RetrievedDocument(Document document, Double score) {
    }
}
