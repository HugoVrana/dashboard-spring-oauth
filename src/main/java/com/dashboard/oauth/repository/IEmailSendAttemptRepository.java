package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.EmailSendAttempt;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IEmailSendAttemptRepository extends MongoRepository<EmailSendAttempt, ObjectId> {

    List<EmailSendAttempt> findByUserIdOrderByAttemptedAtDesc(ObjectId userId);

    List<EmailSendAttempt> findByUserIdAndEmailTypeOrderByAttemptedAtDesc(ObjectId userId, EmailType emailType);

    List<EmailSendAttempt> findByStatus(EmailSendStatus status);

    List<EmailSendAttempt> findByResendMessageId(String resendMessageId);

    int countByUserIdAndEmailTypeAndTokenId(ObjectId userId, EmailType emailType, String tokenId);
}
