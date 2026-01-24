package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface IUserRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByEmailAndAudit_DeletedAtIsNull(String email);
    Optional<User> getUserBy_idAndAudit_DeletedAtIsNull(ObjectId id);
    Optional<User> getUserByEmailVerificationToken_TokenAndAudit_DeletedAtIsNull(String emailVerificationTokenToken);
    Optional<User> getUserByPasswordResetToken_TokenAndAudit_DeletedAtIsNull(String passwordResetTokenToken);
    Boolean existsByEmail(String email);
}
