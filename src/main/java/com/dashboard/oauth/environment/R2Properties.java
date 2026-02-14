package com.dashboard.oauth.environment;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "r2")
public class R2Properties {

    private String accessKeyId;

    private String secretAccessKey;

    private String accountId;

    private String bucketName;

    public String getPublicUrl() {
        return String.format("https://%s.r2.cloudflarestorage.com/%s", accountId, bucketName);
    }

    public static String buildR2Key(ObjectId userId, ObjectId imageId) {
        return String.format("%s_%s", userId, imageId);
    }

    public String buildPublicUrl(ObjectId userId, ObjectId imageId) {
        return String.format("%s/%s_%s", getPublicUrl(), userId, imageId);
    }
}
