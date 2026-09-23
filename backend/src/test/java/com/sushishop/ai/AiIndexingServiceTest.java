package com.sushishop.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiIndexingServiceTest {

    @Mock
    private AiDocumentService documentService;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void clearsOldSnapshotsBeforeAddingCurrentDocuments() {
        var document = Document.builder()
                .text("search_document: current catalog")
                .metadata(Map.of("collection", AiDocumentService.COLLECTION))
                .build();
        when(documentService.loadDocuments()).thenReturn(List.of(document));
        var service = new AiIndexingService(documentService, vectorStore, jdbcTemplate);

        var response = service.rebuildIndex();

        InOrder order = inOrder(jdbcTemplate, vectorStore);
        order.verify(jdbcTemplate).update(
                "DELETE FROM vector_store WHERE metadata ->> 'collection' = ?",
                AiDocumentService.COLLECTION
        );
        order.verify(vectorStore).add(List.of(document));
        assertThat(response.documentCount()).isEqualTo(1);
    }
}
