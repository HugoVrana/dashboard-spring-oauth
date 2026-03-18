package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.AuthorizationCode;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IAuthorizationCodeRepository extends MongoRepository<AuthorizationCode, ObjectId> {
    Optional<AuthorizationCode> findByCodeAndUsedFalseAndAudit_DeletedAtIsNull(String code);
}
