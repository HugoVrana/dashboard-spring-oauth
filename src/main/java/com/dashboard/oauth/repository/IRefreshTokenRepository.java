package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.oauth.RefreshToken;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface IRefreshTokenRepository extends MongoRepository<RefreshToken, ObjectId> {
    Optional<RefreshToken> findByToken(ObjectId token);

    void deleteByUserId(ObjectId userId);
}
