package com.dashboard.oauth.environment;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "r2")
public final class R2Properties {

    private String accessKeyId;

    private String secretAccessKey;

    private String accountId;

    private String bucketName;

    private String publicUrl;

    public static String buildR2Key(ObjectId userId, ObjectId imageId) {
        return String.format("%s_%s", userId, imageId);
    }

    public String buildPublicUrl(ObjectId userId, ObjectId imageId) {
        return String.format("%s/%s_%s", getPublicUrl(), userId, imageId);
    }
}
