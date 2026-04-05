package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;
import com.dashboard.oauth.dataTransferObject.totp.TotpVerifyRequest;
import com.dashboard.oauth.service.interfaces.ITotpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth/2fa")
public class TotpController {

    private final ITotpService totpService;

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        String userId = extractUserId(authentication);
        TotpSetupResponse response = totpService.setupTotp(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyTotp(
            Authentication authentication,
            @Valid @RequestBody TotpVerifyRequest request) {
        String userId = extractUserId(authentication);
        boolean isValid = totpService.verifyTotp(userId, request.getCode());
        if (isValid) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    private String extractUserId(Authentication authentication) {
        if (authentication instanceof GrantsAuthentication grantsAuth) {
            return grantsAuth.getUserId();
        }
        throw new IllegalStateException("Unexpected authentication type");
    }
}
