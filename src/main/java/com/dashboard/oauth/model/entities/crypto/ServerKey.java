package com.dashboard.oauth.model.entities.crypto;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "server_keys")
public class ServerKey {

    @Id
    private ObjectId id;

    private byte[] privateKeyDer;
    private byte[] publicKeyDer;

    @Indexed(unique = true)
    private String kid;
}
