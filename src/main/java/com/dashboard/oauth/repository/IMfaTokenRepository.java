package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.MfaToken;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IMfaTokenRepository extends MongoRepository<MfaToken, ObjectId> {
    Optional<MfaToken> findByTokenAndUsedFalseAndAudit_DeletedAtIsNull(String token);
}
