package com.sushishop.ai;

import com.sushishop.ai.dto.AiChatMessageRequest;
import com.sushishop.ai.dto.AiChatSessionResponse;
import com.sushishop.ai.dto.AiChatTurnResponse;
import com.sushishop.order.dto.response.OrderResponse;
import com.sushishop.shared.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("ai")
@RequestMapping("/api/ai/chat/sessions")
@RequiredArgsConstructor
@Tag(name = "AI Agent", description = "Authenticated shopping assistant with safe order drafts")
@SecurityRequirement(name = "bearerAuth")
public class AiChatController {

    private final AiChatService chatService;

    @PostMapping
    @Operation(summary = "Create a chat session for the current user")
    public ResponseEntity<AiChatSessionResponse> createSession(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createSession(userDetails.getUsername()));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get owned chat history and current order draft")
    public ResponseEntity<AiChatSessionResponse> getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getSession(sessionId, userDetails.getUsername()));
    }

    @PostMapping("/{sessionId}/messages")
    @RateLimit(value = 10)
    @Operation(summary = "Send a message to the shopping agent")
    public ResponseEntity<AiChatTurnResponse> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AiChatMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.sendMessage(
                sessionId, userDetails.getUsername(), request.message()));
    }

    @PostMapping("/{sessionId}/confirm-order")
    @RateLimit(value = 5)
    @Operation(summary = "Confirm the stored draft after server-side re-validation")
    public ResponseEntity<OrderResponse> confirmOrder(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.confirmOrder(sessionId, userDetails.getUsername()));
    }
}
