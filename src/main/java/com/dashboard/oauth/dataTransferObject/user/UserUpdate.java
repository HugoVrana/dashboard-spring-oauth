package com.dashboard.oauth.dataTransferObject.user;

import org.springframework.web.multipart.MultipartFile;

public class UserUpdate {
    public String email;
    public String[] roles;
    public MultipartFile profilePicture;
}
