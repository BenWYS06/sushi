package com.sushishop.ai;

import com.sushishop.product.Product;
import com.sushishop.product.ProductRepository;
import com.sushishop.promotion.Promotion;
import com.sushishop.promotion.PromotionRepository;
import com.sushishop.review.Review;
import com.sushishop.review.ReviewRepository;
import com.sushishop.review.ReviewReply;
import com.sushishop.shared.exception.core.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("ai")
@RequiredArgsConstructor
public class AiDocumentService {

    public static final String COLLECTION = "sushi-shop";

    private static final List<String> FAQ_FILES = List.of(
            "ordering.md", "payment.md", "shop-policies.md");

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<Document> loadDocuments() {
        var documents = new ArrayList<Document>();

        productRepository.findAll().stream()
                .filter(Product::isAvailable)
                .map(this::productDocument)
                .forEach(documents::add);

        promotionRepository.findActiveAt(LocalDateTime.now()).stream()
                .filter(Promotion::isCurrentlyActive)
                .map(this::promotionDocument)
                .forEach(documents::add);

        reviewRepository.findAll().stream()
                .filter(review -> review.getProduct().isAvailable())
                .map(this::reviewDocument)
                .forEach(documents::add);

        FAQ_FILES.stream().map(this::faqDocument).forEach(documents::add);
        return documents;
    }

    /** Search uses indexed text; the answer uses these freshly loaded facts. */
    @Transactional(readOnly = true)
    public Optional<Document> refresh(Document indexed) {
        var metadata = indexed.getMetadata();
        if (!COLLECTION.equals(metadata.get("collection"))) {
            return Optional.empty();
        }       

        String type = String.valueOf(metadata.get("type"));
        String sourceId = String.valueOf(metadata.get("sourceId"));
        if ("faq".equals(type)) {
            return FAQ_FILES.contains(sourceId)
                    ? Optional.of(faqDocument(sourceId)) : Optional.empty();
        }

        long id;
        try {
            id = Long.parseLong(sourceId);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }


        return switch (type) {
            case "product" -> productRepository.findById(id)
                    .filter(Product::isAvailable)
                    .map(this::productDocument);
            case "promotion" -> promotionRepository.findById(id)
                    .filter(Promotion::isCurrentlyActive)
                    .map(this::promotionDocument);
            case "review" -> reviewRepository.findById(id)
                    .filter(review -> review.getProduct().isAvailable())
                    .map(this::reviewDocument);
            default -> Optional.empty();
        };
    }

    private Document productDocument(Product product) {
        String text = """
                Product: %s
                Category: %s
                Description: %s
                Current base price: %s UAH
                Available to order: yes
                Weight: %s g
                Pieces: %s
                Ingredients, allergens and dietary suitability are not verified by this catalog.
                The checkout price is authoritative; do not calculate an automatic promotion discount.
                """.formatted(product.getName(), product.getCategory(), valueOrUnknown(product.getDescription()),
                product.getPrice().toPlainString(), product.getWeight(), valueOrUnknown(product.getPieces()));

System.out.println(product.getPrice().toPlainString());

        return document("product", product.getId().toString(), product.getName(),
                "/products/" + product.getSlug(), text);
    }

    private Document promotionDocument(Promotion promotion) {
        String products = promotion.getProducts().stream()
                .filter(Product::isAvailable)
                .sorted(Comparator.comparing(Product::getId))
                .limit(10)
                .map(Product::getName)
                .collect(Collectors.joining(", "));

        String text = """
                Active promotion: %s
                Description: %s
                Advertised discount: %s%%
                Starts: %s
                Ends: %s
                Linked available products (up to 10): %s
                This is descriptive promotion information. The current checkout uses product base prices
                and does not automatically apply promotion percentages. Confirm the amount at checkout.
                """.formatted(promotion.getTitle(), valueOrUnknown(promotion.getDescription()),
                promotion.getDiscountPercent().toPlainString(), promotion.getStartDate(), promotion.getEndDate(),
                products.isBlank() ? "No available products listed" : products);

        return document("promotion", promotion.getId().toString(), promotion.getTitle(),
                "/promotions/" + promotion.getSlug(), text);
    }

    private Document reviewDocument(Review review) {
        Product product = review.getProduct();
        String replies = review.getReplies().stream()
                .sorted(Comparator.comparing(ReviewReply::getId))
                .limit(3)
                .map(reply -> "Shop reply: " + reply.getMessage())
                .collect(Collectors.joining("\n"));

                System.out.println("Sorted+limited replies: " + replies);

        // Public text only: never copy author names, emails or other user fields.
        String text = """
                Public review of: %s
                Rating: %s / 5
                Customer comment: %s
                %s
                This review is one customer's opinion, not a verified product fact or shop policy.
                """.formatted(product.getName(), review.getRating(), valueOrUnknown(review.getComment()), replies);

        return document("review", review.getId().toString(), "Review of " + product.getName(),
                "/products/" + product.getSlug(), text);
    }

    private Document faqDocument(String filename) {
        try {
            var resource = new ClassPathResource("ai/knowledge/" + filename);
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            String title = text.lines().findFirst().orElse(filename).replaceFirst("^#\\s*", "");
            return document("faq", filename, title, "", text);
        } catch (IOException ex) {
            log.error("Cannot read AI knowledge file {}", filename, ex);
            throw new InternalServerException("The shop knowledge files could not be loaded.");
        }
    }

    private Document document(String type, String sourceId, String title, String url, String text) {
        String id = UUID.nameUUIDFromBytes(
                (COLLECTION + ":" + type + ":" + sourceId).getBytes(StandardCharsets.UTF_8)).toString();

        // One compact document per entity is enough for this small catalog.
        return Document.builder()
                .id(id)
                .text("search_document: " + (text.length() > 1800 ? text.substring(0, 1800) : text))
                .metadata(Map.of("collection", COLLECTION, "type", type,
                        "sourceId", sourceId, "title", title, "url", url))
                .build();
    }

    private String valueOrUnknown(Object value) {
        return value == null || value.toString().isBlank() ? "Not provided" : value.toString();
    }
}
