package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.email.EmailSendAttempt;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IEmailSendAttemptRepository extends MongoRepository<EmailSendAttempt, ObjectId> {
}
