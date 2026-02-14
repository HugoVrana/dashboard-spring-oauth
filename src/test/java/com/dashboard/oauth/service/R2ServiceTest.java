package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.R2Properties;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("R2 Service")
@ExtendWith(MockitoExtension.class)
class R2ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private R2Properties r2Properties;

    @InjectMocks
    private R2Service r2Service;

    private final Faker faker = new Faker();

    private ObjectId testUserId;
    private String testBucketName;
    private String testPublicUrl;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        testBucketName = "test-bucket";
        testPublicUrl = "https://test-account.r2.cloudflarestorage.com/test-bucket";
    }

    @Test
    @DisplayName("Upload file successfully")
    void uploadFile_shouldUploadAndReturnUrlAndKey() {
        String originalFilename = "test-image.png";
        byte[] content = faker.lorem().paragraph().getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                "image/png",
                content
        );

        when(r2Properties.getBucketName()).thenReturn(testBucketName);
        when(r2Properties.getPublicUrl()).thenReturn(testPublicUrl);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String[] result = r2Service.uploadFile(file, testUserId);

        assertThat(result).hasSize(3);
        assertThat(result[0]).startsWith(testPublicUrl);
        assertThat(result[0]).contains(testUserId.toHexString());
        assertThat(result[1]).contains(testUserId.toHexString());
        assertThat(ObjectId.isValid(result[2])).isTrue();

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(testBucketName);
        assertThat(capturedRequest.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Delete file successfully")
    void deleteFile_shouldDeleteFromS3() {
        String r2Key = testUserId.toHexString() + "/uuid_test-image.png";

        when(r2Properties.getBucketName()).thenReturn(testBucketName);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        r2Service.deleteFile(r2Key);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(testBucketName);
        assertThat(capturedRequest.key()).isEqualTo(r2Key);
    }

    @Test
    @DisplayName("Delete file with null key does nothing")
    void deleteFile_shouldDoNothingWhenKeyIsNull() {
        r2Service.deleteFile(null);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Delete file with empty key does nothing")
    void deleteFile_shouldDoNothingWhenKeyIsEmpty() {
        r2Service.deleteFile("");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Upload file generates unique keys")
    void uploadFile_shouldGenerateUniqueKeys() {
        byte[] content = "test".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", content);

        when(r2Properties.getBucketName()).thenReturn(testBucketName);
        when(r2Properties.getPublicUrl()).thenReturn(testPublicUrl);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String[] result1 = r2Service.uploadFile(file, testUserId);
        String[] result2 = r2Service.uploadFile(file, testUserId);

        assertThat(result1[1]).isNotEqualTo(result2[1]);
    }
}
