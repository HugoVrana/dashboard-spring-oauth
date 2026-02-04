package com.dashboard.oauth.controller;

import com.dashboard.oauth.service.interfaces.IEmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final IEmailTemplateService emailTemplateService;

    @GetMapping("/verification")
    public ResponseEntity<String> getTemplateString() {
        return ResponseEntity.ok(emailTemplateService.renderVerificationEmail("test.com", 50));
    }

    @GetMapping("/reset")
    public ResponseEntity<String> resetPassword() {
        return ResponseEntity.ok(emailTemplateService.renderPasswordResetEmail("test.com", 50));
    }
}
