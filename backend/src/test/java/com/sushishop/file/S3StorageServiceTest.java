package com.sushishop.file;

import com.sushishop.shared.exception.core.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageServiceTest {

    private S3StorageService s3StorageService;
    private S3Client s3Client;

    @BeforeEach
    void setUp() throws Exception {
        s3StorageService = new S3StorageService();
        s3Client = mock(S3Client.class);
        setField("bucket", "sushi-images");
        setField("region", "us-east-1");
        setField("keyPrefix", "products");
        setField("publicUrl", "https://cdn.example.com");
        setField("allowedTypes", Set.of("image/jpeg", "image/png", "image/webp"));
        setField("maxFileSize", 5_242_880L);
        setField("s3Client", s3Client);
    }

    @Test
    void uploadsImageAndReturnsItsPublicUrl() {
        var image = new MockMultipartFile("image", "maki.jpg", "image/jpeg", "image-data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = s3StorageService.upload(image);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("sushi-images");
        assertThat(request.getValue().key()).startsWith("products/").endsWith(".jpg");
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(url).startsWith("https://cdn.example.com/products/").endsWith(".jpg");
    }

    @Test
    void returnsNullForAnEmptyImage() {
        var image = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThat(s3StorageService.upload(image)).isNull();
    }

    @Test
    void rejectsAnUnsupportedImageType() {
        var image = new MockMultipartFile("image", "file.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> s3StorageService.upload(image))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void rejectsAnImageLargerThanTheLimit() {
        var image = new MockMultipartFile("image", "large.jpg", "image/jpeg", new byte[5_242_881]);

        assertThatThrownBy(() -> s3StorageService.upload(image))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File size exceeds maximum allowed size");
    }

    @Test
    void deletesTheObjectForAnApplicationImageUrl() {
        s3StorageService.deleteByUrl("https://cdn.example.com/products/maki.jpg");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("sushi-images");
        assertThat(request.getValue().key()).isEqualTo("products/maki.jpg");
    }

    private void setField(String name, Object value) throws Exception {
        Field field = S3StorageService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(s3StorageService, value);
    }
}
