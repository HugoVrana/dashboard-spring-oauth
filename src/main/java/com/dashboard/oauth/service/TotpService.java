package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;
import com.dashboard.oauth.model.entities.TotpConfig;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.ITotpService;
import com.dashboard.oauth.service.interfaces.IUserService;
import com.dashboard.common.model.exception.InvalidRequestException;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TotpService implements ITotpService {

    private static final String ISSUER = "Dashboard";
    private static final int SECRET_LENGTH = 32;

    private final IUserService userService;

    @Override
    public TotpSetupResponse setupTotp(String userId) {
        ObjectId objectId = new ObjectId(userId);
        User user = userService.getUserById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
        String secret = secretGenerator.generate();

        TotpConfig totpConfig = new TotpConfig();
        totpConfig.setSecret(secret);
        totpConfig.setEnabled(false);
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        totpConfig.setAudit(audit);

        user.setTwoFactorConfig(totpConfig);
        Audit a = user.getAudit();
        a.setUpdatedAt(Instant.now());
        user.setAudit(a);
        userService.saveUser(user);

        String qrCodeDataUri = generateQrCodeDataUri(user.getEmail(), secret);

        return new TotpSetupResponse(qrCodeDataUri, secret);
    }

    @Override
    public boolean verifyTotp(String userId, String code) {
        ObjectId objectId = new ObjectId(userId);
        User user = userService.getUserById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!(user.getTwoFactorConfig() instanceof TotpConfig totpConfig)) {
            throw new InvalidRequestException("TOTP not configured for this user");
        }

        String secret = totpConfig.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new InvalidRequestException("TOTP not configured for this user");
        }

        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
                new SystemTimeProvider()
        );

        boolean isValid = verifier.isValidCode(secret, code);

        if (isValid && !Boolean.TRUE.equals(totpConfig.getEnabled())) {
            totpConfig.setEnabled(true);
            Audit audit = totpConfig.getAudit();
            audit.setUpdatedAt(Instant.now());
            totpConfig.setAudit(audit);

            audit = user.getAudit();
            audit.setUpdatedAt(Instant.now());
            user.setAudit(audit);
            userService.saveUser(user);
        }

        return isValid;
    }

    private String generateQrCodeDataUri(String email, String secret) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = qrGenerator.generate(qrData);
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}
