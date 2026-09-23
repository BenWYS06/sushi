package com.sushishop.ai;

import com.sushishop.product.Product;
import com.sushishop.product.ProductRepository;
import com.sushishop.promotion.Promotion;
import com.sushishop.promotion.PromotionRepository;
import com.sushishop.review.Review;
import com.sushishop.review.ReviewRepository;
import com.sushishop.review.ReviewReply;
import com.sushishop.shared.enums.Category;
import com.sushishop.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDocumentServiceTest {
        
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @InjectMocks
    private AiDocumentService documentService;

    @Test
    void createsStableIdsAndOmitsUnavailableProducts() {
        Product available = product(1L, true);
        when(productRepository.findAll()).thenReturn(List.of(available, product(2L, false)));

        var first = documentService.loadDocuments();
        var second = documentService.loadDocuments();
        var products = first.stream().filter(doc -> "product".equals(doc.getMetadata().get("type"))).toList();

        assertThat(products).hasSize(1);
        assertThat(first).extracting(Document::getId).containsExactlyElementsOf(
                second.stream().map(Document::getId).toList());
        assertThat(UUID.fromString(products.getFirst().getId())).isNotNull();
        assertThat(products.getFirst().getMetadata()).containsEntry("sourceId", "1")
                .containsEntry("url", "/products/salmon-roll");
        assertThat(products.getFirst().getText()).contains("199.00 UAH", "not verified");
    }

    @Test
    void includesReviewTextAndAtMostThreeRepliesWithoutAuthorDetails() {
        User author = User.builder().id(10L).name("Private Name").email("private@example.com")
                .phone("+380123456789").password("private-password").build();
        var replies = List.of(
                ReviewReply.builder().id(1L).user(author).message("Reply one").build(),
                ReviewReply.builder().id(2L).user(author).message("Reply two").build(),
                ReviewReply.builder().id(3L).user(author).message("Reply three").build(),
                ReviewReply.builder().id(4L).user(author).message("Reply four").build());
        var review = Review.builder().id(20L).user(author).product(product(1L, true))
                .rating(5).comment("Fresh and tasty").replies(replies).build();
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        Document result = documentService.loadDocuments().stream()
                .filter(doc -> "review".equals(doc.getMetadata().get("type"))).findFirst().orElseThrow();

        assertThat(result.getText()).contains("Fresh and tasty", "Reply one","Reply two", "Reply three")
                .doesNotContain("Reply four", "Private Name", "private@example.com", "+380123456789", "private-password");
        assertThat(result.getMetadata()).doesNotContainKeys("userId", "email", "author");
        assertThat(result.getText()).hasSizeLessThanOrEqualTo(1820);
    }

    @Test
    void refreshesPriceFromDatabaseAndDropsUnavailableOrDeletedProducts() {
        Product product = product(1L, true);
        product.setPrice(new BigDecimal("250.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Document indexed = indexed("product", "1");

        assertThat(documentService.refresh(indexed).orElseThrow().getText()).contains("250.00 UAH");

        product.setAvailable(false);
        assertThat(documentService.refresh(indexed)).isEmpty();

        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThat(documentService.refresh(indexed)).isEmpty();
    }

    @Test
    void excludesExpiredPromotionsAndKeepsCheckoutPriceCaveat() {
        var promotion = Promotion.builder().id(1L).slug("weekend").title("Weekend")
                .discountPercent(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(1)).build();
        when(promotionRepository.findActiveAt(any())).thenReturn(List.of(promotion));
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        Document indexed = documentService.loadDocuments().stream()
                .filter(doc -> "promotion".equals(doc.getMetadata().get("type"))).findFirst().orElseThrow();
        assertThat(indexed.getText()).contains("10.00%", "does not automatically apply");

        promotion.setEndDate(LocalDateTime.now().minusMinutes(1));
        assertThat(documentService.refresh(indexed)).isEmpty();
    }

    @Test
    void dropsReviewsForUnavailableProducts() {
        var review = Review.builder().id(20L).product(product(1L, false)).rating(5).build();
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        assertThat(documentService.refresh(indexed("review", "20"))).isEmpty();
    }

    @Test
    void onlyLoadsAllowlistedFaqAndDoesNotInventPolicies() {
        var faq = documentService.refresh(indexed("faq", "shop-policies.md")).orElseThrow();

        assertThat(faq.getText()).contains("have not been configured", "Contact the shop");
        assertThat(documentService.refresh(indexed("faq", "../../application.yaml"))).isEmpty();
        assertThat(documentService.refresh(indexed("product", "invalid-id"))).isEmpty();
        var foreign = Document.builder().text("Unrelated content")
                .metadata(Map.of("collection", "another-app", "type", "product", "sourceId", "1")).build();
        assertThat(documentService.refresh(foreign)).isEmpty();
    }

    private Product product(Long id, boolean available) {
        return Product.builder().id(id).slug("salmon-roll").name("Salmon roll")
                .description("Rice and salmon").price(new BigDecimal("199.00"))
                .category(Category.ROLL).weight(250).pieces(8).available(available).build();
    }

    private Document indexed(String type, String sourceId) {
        return Document.builder().text("Old index snapshot")
                .metadata(Map.of("collection", AiDocumentService.COLLECTION, "type", type, "sourceId", sourceId))
                .build();
    }
}
