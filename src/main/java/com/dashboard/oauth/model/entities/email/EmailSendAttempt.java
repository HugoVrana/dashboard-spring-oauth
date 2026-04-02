package com.dashboard.oauth.model.entities.email;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @Indexed
    private ObjectId userId;

    @NotNull
    private EmailType emailType;

    @NotBlank
    private String tokenId;

    @Email
    @NotBlank
    private String recipientEmail;

    private Instant attemptedAt;

    private Instant sentAt;

    @Builder.Default
    private EmailSendStatus status = EmailSendStatus.QUEUED;

    private String resendMessageId;

    private String errorMessage;

    private Audit audit;
}
