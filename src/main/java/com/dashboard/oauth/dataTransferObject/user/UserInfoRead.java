package com.dashboard.oauth.dataTransferObject.user;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import lombok.Data;

@Data
public class UserInfoRead {
    private String id;
    private String email;
    private RoleRead[] roleReads;
}
