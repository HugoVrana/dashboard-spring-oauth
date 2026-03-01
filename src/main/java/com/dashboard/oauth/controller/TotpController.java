package com.dashboard.oauth.controller;

import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;
import com.dashboard.oauth.dataTransferObject.totp.TotpVerifyRequest;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
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

@RestController
@CrossOrigin
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TotpController {

    private final ITotpService totpService;

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        String userId = user.get_id().toHexString();

        TotpSetupResponse response = totpService.setupTotp(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyTotp(
            Authentication authentication,
            @Valid @RequestBody TotpVerifyRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        String userId = user.get_id().toHexString();

        boolean isValid = totpService.verifyTotp(userId, request.getCode());
        if (isValid) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
