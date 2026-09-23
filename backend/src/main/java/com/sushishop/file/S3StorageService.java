package com.sushishop.file;

import com.sushishop.shared.exception.core.BadRequestException;
import com.sushishop.shared.exception.core.InternalServerException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.region}")
    private String region;

    @Value("${app.s3.key-prefix:products}")
    private String keyPrefix;

    @Value("${app.s3.public-url:}")
    private String publicUrl;

    @Value("${app.s3.allowed-types:image/jpeg,image/png,image/webp}")
    private Set<String> allowedTypes;

    @Value("${app.s3.max-size:5242880}")
    private long maxFileSize;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (bucket == null || bucket.isBlank()) {
            throw new InternalServerException("AWS_S3_BUCKET must be configured");
        }

        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    /** Uploads one image to S3 and returns the public URL saved in product_images.url. */
    public String upload(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        String contentType = validate(image);
        String key = buildKey(contentType);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(image.getSize())
                    .cacheControl("public, max-age=31536000")
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
            log.info("Uploaded product image to S3: {}", key);
            return publicUrlFor(key);
        } catch (IOException | SdkException e) {
            throw new InternalServerException("Failed to upload image to S3", e);
        }
    }

    /** Deletes the S3 object represented by a URL previously returned from upload. */
    public void deleteByUrl(String imageUrl) {
        String key = keyFromUrl(imageUrl);
        if (key == null) {
            log.warn("Cannot delete image because it is not an S3 URL for this application: {}", imageUrl);
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("Deleted product image from S3: {}", key);
        } catch (SdkException e) {
            log.error("Failed to delete S3 image: {}", key, e);
        }
    }

    private String validate(MultipartFile image) {
        if (image.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed size");
        }

        String contentType = image.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("File type not allowed: " + contentType);
        }
        return contentType;
    }

    private String buildKey(String contentType) {
        String prefix = keyPrefix == null ? "" : keyPrefix.replaceAll("^/+|/+$", "");
        String fileName = UUID.randomUUID() + extensionFor(contentType);
        return prefix.isBlank() ? fileName : prefix + "/" + fileName;
    }

    private String publicUrlFor(String key) {
        String baseUrl = publicUrl == null || publicUrl.isBlank()
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicUrl.replaceAll("/+$", "");
        return baseUrl + "/" + key;
    }

    private String keyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String baseUrl = publicUrl == null || publicUrl.isBlank()
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicUrl.replaceAll("/+$", "");
        return imageUrl.startsWith(baseUrl + "/") ? imageUrl.substring(baseUrl.length() + 1) : null;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png"; 
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
