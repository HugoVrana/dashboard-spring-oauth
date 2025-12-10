package com.dashboard.oauth.dataTransferObject.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddGrantToRoleRequest {
    @NotBlank(message = "Grant ID is required")
    private String grantId;

    @NotBlank(message = "RoleId is required")
    private String roleId;
}
