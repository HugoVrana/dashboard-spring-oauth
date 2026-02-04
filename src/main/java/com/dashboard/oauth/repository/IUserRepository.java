package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface IUserRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByEmailAndAudit_DeletedAtIsNull(String email);
    Optional<User> getUserBy_idAndAudit_DeletedAtIsNull(ObjectId id);
    Optional<User> getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(ObjectId passwordResetTokenToken);
    Optional<User> getUserByEmailVerificationToken__idAndAudit_DeletedAtIsNull(ObjectId emailVerificationTokenToken);
    Boolean existsByEmail(String email);
    @Query("{ 'audit.deletedAt': null, 'emailVerificationToken': { $ne: null }, 'emailVerificationToken.emailSentAt': null }")
    List<User> findUsersWithUnsentVerificationEmail();
    @Query("{ 'audit.deletedAt': null, 'passwordResetToken': { $ne: null }, 'passwordResetToken.emailSentAt': null }")
    List<User> findUsersWithUnsentPasswordResetEmail();
}
