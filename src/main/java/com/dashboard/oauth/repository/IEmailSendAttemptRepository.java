package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.EmailSendAttempt;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IEmailSendAttemptRepository extends MongoRepository<EmailSendAttempt, ObjectId> {
}
