package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.user.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IUserRepository extends MongoRepository<User, ObjectId> {
    List<User> findAllByAudit_DeletedAtIsNull();

    @Query("{ 'audit.deletedAt': null, 'email': { $regex: ?0, $options: 'i' } }")
    List<User> searchByEmail(String emailPattern);

    Optional<User> findByEmailAndAudit_DeletedAtIsNull(String email);

    Optional<User> getUserBy_idAndAudit_DeletedAtIsNull(ObjectId id);

    Optional<User> getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(ObjectId passwordResetTokenToken);

    Optional<User> getUserByEmailVerificationToken__idAndAudit_DeletedAtIsNull(ObjectId emailVerificationTokenToken);

    Boolean existsByEmail(String email);

    @Query("{ 'audit.deletedAt': null, 'emailVerificationToken': { $ne: null }, 'emailVerificationToken.emailSentAt': null }")
    List<User> findUsersWithUnsentVerificationEmail();

    @Query("{ 'audit.deletedAt': null, 'passwordResetToken': { $ne: null }, 'passwordResetToken.emailSentAt': null }")
    List<User> findUsersWithUnsentPasswordResetEmail();

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'failedLoginAttempts': 1 }, '$set': { 'audit.updatedAt': ?1 } }")
    void incrementFailedLoginAttempts(ObjectId userId, Instant updatedAt);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'locked': true, 'audit.updatedAt': ?1 } }")
    void lockUser(ObjectId userId, Instant updatedAt);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'failedLoginAttempts': 0 } }")
    void resetFailedLoginAttempts(ObjectId userId);
}
