package com.dashboard.oauth.service.interfaces;

import com.resend.core.exception.ResendException;

public interface IEmailSenderService {
    String sendEmail(String to, String subject, String content) throws ResendException;
}
