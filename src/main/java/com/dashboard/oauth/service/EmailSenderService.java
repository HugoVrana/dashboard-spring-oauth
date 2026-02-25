package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.service.interfaces.IEmailSenderService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderService implements IEmailSenderService {

    private final Resend resend;
    private final EmailProperties emailProperties;

    @Override
    public String sendEmail(String to, String subject, String content) throws ResendException {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(emailProperties.getFromAddress())
                .to(to)
                .subject(subject)
                .html(content)
                .build();

        CreateEmailResponse response = resend.emails().send(params);
        return response.getId();
    }
}