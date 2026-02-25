package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.service.interfaces.IR2Service;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class R2Service implements IR2Service {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    @Override
    public String[] uploadFile(MultipartFile file, ObjectId userId) {
        ObjectId imageObjectId = ObjectId.get();
        String r2Key = String.format("%s_%s", userId, imageObjectId);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(r2Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to R2", e);
        }

        String publicUrl = String.format("%s/%s", r2Properties.getPublicUrl(), r2Key);
        return new String[]{publicUrl, r2Key, imageObjectId.toString()};
    }

    @Override
    public void deleteFile(String r2Key) {
        if (r2Key == null || r2Key.isEmpty()) {
            return;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(r2Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}
