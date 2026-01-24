package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.ITokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService implements ITokenService {

    @Autowired
    private IUserRepository userRepository;

    public String createEmailVerificationToken(User user) {
        String tokenValue = UUID.randomUUID().toString();

        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);

        token.setExpiryDate(Instant.now().plus(Duration.ofDays(1)));
        token.setCreatedAt(Instant.now());
        token.setUsed(false);
        user.setEmailVerificationToken(token);
        userRepository.save(user);

        return tokenValue;
    }

    public String createPasswordResetToken(User user) {
        String tokenValue = UUID.randomUUID().toString();

        Instant now = Instant.now();
        TemporalAmount hour = Duration.ofHours(1);

        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);
        token.setExpiryDate(now.plus(hour)); // Shorter for security
        token.setCreatedAt(now);
        token.setUsed(false);

        user.setPasswordResetToken(token);
        userRepository.save(user);

        return tokenValue;
    }

    public void verifyEmail(String tokenValue) {
        User user = userRepository.getUserByPasswordResetToken_TokenAndAudit_DeletedAtIsNull(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        VerificationToken token = user.getEmailVerificationToken();
        if (token == null || !token.isValid()) {
            throw new RuntimeException("Token expired or already used");
        }

        // Update permanent state
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        // Mark token as used
        token.setUsed(true);
        token.setUsedAt(Instant.now());

        Audit a = user.getAudit();
        a.setUpdatedAt(Instant.now());
        user.setAudit(a);

        userRepository.save(user);
    }

    public void resetPassword(String tokenValue, String newPassword) {
        User user = userRepository.getUserByPasswordResetToken_TokenAndAudit_DeletedAtIsNull(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        VerificationToken token = user.getPasswordResetToken();
        if (token == null || !token.isValid()) {
            throw new RuntimeException("Token expired or already used");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Mark token as used
        token.setUsed(true);
        token.setUsedAt(Instant.now());

        user.setPasswordResetToken(token);

        Audit a = user.getAudit();
        a.setUpdatedAt(Instant.now());
        user.setAudit(a);

        userRepository.save(user);
    }
}