package com.dashboard.oauth.controller;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IR2Service;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final IR2Service r2Service;

    @PostMapping("profilePicture")
    public ResponseEntity<String> setUserProfilePicture(Authentication authentication,
                                                        @RequestParam("file") MultipartFile file) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Delete old profile picture from R2 if exists
        if (user.getProfileImageId() != null) {
            String oldR2Key = R2Properties.buildR2Key(user.get_id(), user.getProfileImageId());
            r2Service.deleteFile(oldR2Key);
        }

        // Upload new profile picture
        String[] result = r2Service.uploadFile(file, user.get_id());
        if (result.length < 3) {
            throw new ResourceNotFoundException("Image not found");
        }

        String publicUrl = result[0];
        String r2Key = result[1];
        String imageObjectId = result[2];

        if (imageObjectId == null || imageObjectId.isEmpty() || !ObjectId.isValid(imageObjectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        ObjectId objectId = new ObjectId(imageObjectId);
        user.setProfileImageId(objectId);
        userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(publicUrl);
    }
}