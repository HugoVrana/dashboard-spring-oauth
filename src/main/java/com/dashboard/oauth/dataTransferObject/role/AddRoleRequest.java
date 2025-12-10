package com.dashboard.oauth.dataTransferObject.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddRoleRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Role ID is required")
    private String roleId;
}
