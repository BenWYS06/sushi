package com.sushishop.product;

import com.sushishop.file.S3StorageService;
import com.sushishop.shared.exception.core.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductRepository productRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void addImage(Long productId, MultipartFile file) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        String url = s3StorageService.upload(file);
        if (url == null) {
            log.warn("Skipping empty image for product: {}", productId);
            return;
        }

        int nextOrder = product.getProductImages().stream()
                .mapToInt(ProductImage::getSortOrder)
                .max()
                .orElse(-1) + 1;

        product.getProductImages().add(ProductImage.builder()
                .url(url)
                .sortOrder(nextOrder)
                .product(product)
                .build());
        productRepository.save(product);
        log.info("Image added to product: {}", productId);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteImage(Long productId, Long imageId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        var image = product.getProductImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId));

        s3StorageService.deleteByUrl(image.getUrl());
        product.getProductImages().remove(image);
        productRepository.save(product);
        log.info("Image {} deleted from product: {}", imageId, productId);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void reorderImages(Long productId, List<Long> imageIds) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        for (int i = 0; i < imageIds.size(); i++) {
            final int newOrder = i;
            product.getProductImages().stream()
                    .filter(img -> img.getId().equals(imageIds.get(newOrder)))
                    .findFirst()
                    .ifPresent(img -> img.setSortOrder(newOrder));
        }
        productRepository.save(product);
        log.info("Images reordered for product: {}", productId);
    }

    public void addImagesToProduct(Product product, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;

        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String url = s3StorageService.upload(images.get(i));
            if (url != null) {
                productImages.add(ProductImage.builder()
                        .url(url)
                        .sortOrder(i)
                        .product(product)
                        .build());
            }
        }
        product.setProductImages(productImages);
    }
}
