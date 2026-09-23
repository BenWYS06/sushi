package com.sushishop.ai;

import com.sushishop.ai.dto.AiAnswerResponse;
import com.sushishop.ai.dto.AiIndexResponse;
import com.sushishop.ai.dto.AiQuestionRequest;
import com.sushishop.ai.dto.AiSourceResponse;
import com.sushishop.shared.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@Profile("ai")
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
@Tag(name = "AI RAG", description = "Build and test the grounded shop knowledge index")
@SecurityRequirement(name = "bearerAuth")
public class AiRagController {

    private final AiIndexingService indexingService;
    private final AiRagService ragService;

    @PostMapping("/index")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rebuild the RAG index from current shop data")
    public ResponseEntity<AiIndexResponse> rebuildIndex() {
        return ResponseEntity.ok(indexingService.rebuildIndex());
    }

    @GetMapping("/search")
    @RateLimit(value = 10)
    @Operation(summary = "Inspect documents retrieved for a question")
    public ResponseEntity<List<AiSourceResponse>> search(
            @RequestParam
            @NotBlank(message = "Query is required")
            @Size(max = 500, message = "Query must be at most 500 characters")
            String query
    ) {
        return ResponseEntity.ok(ragService.search(query));
    }
}
