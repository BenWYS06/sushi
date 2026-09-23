package com.sushishop.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product image response")
public record ProductImageResponse(
        @Schema(description = "Image ID", example = "1")
        Long id,

        @Schema(description = "Public S3 or CloudFront image URL", example = "https://cdn.example.com/products/abc.jpg")
        String url
) {
}
