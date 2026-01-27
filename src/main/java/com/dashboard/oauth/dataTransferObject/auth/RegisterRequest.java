package com.dashboard.oauth.dataTransferObject.auth;

import com.dashboard.oauth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
        @Email
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @StrongPassword
        private String password;

        @NotBlank(message = "Role is required")
        private String roleId;
}
