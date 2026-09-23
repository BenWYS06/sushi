package com.sushishop.ai;

import com.sushishop.ai.dto.AiIndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("ai")
@RequiredArgsConstructor
public class AiIndexingService {

    private final AiDocumentService documentService;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public AiIndexResponse rebuildIndex() {
        var documents = documentService.loadDocuments();

        // Remove old snapshots, including data that was deleted or made unavailable.
        jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata ->> 'collection' = ?",
                AiDocumentService.COLLECTION
        );

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        log.info("AI index rebuilt with {} documents", documents.size());
        return new AiIndexResponse(documents.size(), "AI knowledge index rebuilt");
    }

    @Scheduled(cron = "${app.ai.indexing-cron:-}")
    @Transactional
    public void scheduledRebuild() {
        rebuildIndex();
    }
}
