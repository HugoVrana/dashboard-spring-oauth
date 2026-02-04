package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.ITokenService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

@Service
public class TokenService implements ITokenService {

    @Autowired
    private IUserRepository userRepository;

    public String createEmailVerificationToken(User user) {
        VerificationToken token = new VerificationToken();
        token.set_id(new ObjectId());
        token.setExpiryDate(Instant.now().plus(Duration.ofDays(1)));
        token.setCreatedAt(Instant.now());
        token.setUsed(false);
        user.setEmailVerificationToken(token);
        userRepository.save(user);
        Optional<User> optionalUser = userRepository.findById(user.get_id());
        if (optionalUser.isEmpty()) {
            return null;
        }
        User u = optionalUser.get();
        return u.getEmailVerificationToken().get_id().toHexString();
    }

    public String createPasswordResetToken(User user) {
        ObjectId id = ObjectId.get();

        Instant now = Instant.now();
        TemporalAmount hour = Duration.ofHours(1);

        VerificationToken token = new VerificationToken();
        token.set_id(id);
        token.setExpiryDate(now.plus(hour)); // Shorter for security
        token.setCreatedAt(now);
        token.setUsed(false);

        user.setPasswordResetToken(token);
        userRepository.save(user);

        Optional<User> optionalUser = userRepository.findById(user.get_id());
        if (optionalUser.isEmpty()) {
            return null;
        }
        User u = optionalUser.get();
        return u.getPasswordResetToken().get_id().toHexString();
    }

    public void verifyEmail(String tokenValue) {
        if (!ObjectId.isValid(tokenValue)) {
            throw new RuntimeException("Invalid verification token");
        }
        User user = userRepository.getUserByEmailVerificationToken__idAndAudit_DeletedAtIsNull(new ObjectId(tokenValue))
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
        if (!ObjectId.isValid(tokenValue)) {
            throw new RuntimeException("Invalid reset token");
        }
        User user = userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(new ObjectId(tokenValue))
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