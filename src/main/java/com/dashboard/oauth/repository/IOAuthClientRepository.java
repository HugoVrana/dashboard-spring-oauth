package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.OAuthClient;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IOAuthClientRepository extends MongoRepository<OAuthClient, ObjectId> {
    Optional<OAuthClient> findByClientIdAndAudit_DeletedAtIsNull(String clientId);
}
