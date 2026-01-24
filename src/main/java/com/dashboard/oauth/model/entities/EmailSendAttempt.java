package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "email_send_attempts")
@Data
@Builder
public class EmailSendAttempt {
    @Id
    private ObjectId _id;

    @Indexed
    private ObjectId userId;

    private EmailType emailType;

    private String tokenId;

    private String recipientEmail;

    private Instant attemptedAt;

    private Instant sentAt;

    @Builder.Default
    private EmailSendStatus status = EmailSendStatus.QUEUED;

    private String resendMessageId;

    private String errorMessage;

    private Audit audit;
}
