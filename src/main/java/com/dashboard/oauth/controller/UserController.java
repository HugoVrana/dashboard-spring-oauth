package com.dashboard.oauth.controller;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IR2Service;
import com.dashboard.oauth.service.interfaces.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final IR2Service r2Service;
    private final R2Properties r2Properties;

    @GetMapping("me")
    public ResponseEntity<UserSelfRead> getMe(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        assert userDetails != null;
        User user = userDetails.getUser();
        return ResponseEntity.ok(userService.getSelf(user));
    }

    @PutMapping("me")
    public ResponseEntity<UserSelfRead> updateMe(
            Authentication authentication,
            @Valid @RequestBody UserSelfUpdate userSelfUpdate) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        assert userDetails != null;
        User user = userDetails.getUser();
        return ResponseEntity.ok(userService.updateSelf(user, userSelfUpdate));
    }

    @GetMapping("/{id}/profilePicture")
    public ResponseEntity<String> getProfilePicture(@PathVariable("id") String userId) {
        if (!ObjectId.isValid(userId)) {
            throw new ResourceNotFoundException("Image for user " + userId + " not found");
        }

        ObjectId userIdObject = new ObjectId(userId);
        Optional<User> optionalUser = userService.getUserById(userIdObject);
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("Image for user " + userId + " not found");
        }

        User user = optionalUser.get();
        if (user.getAudit().getDeletedAt() != null) {
            throw new ResourceNotFoundException("Image for user " + userId + " not found");
        }

        if (user.getProfileImageId() == null) {
            throw new ResourceNotFoundException("Image for user " + userId + " not found");
        }

        String url = r2Properties.buildPublicUrl(user.get_id(), user.getProfileImageId());
        return ResponseEntity.ok(url);
    }

    @PostMapping("profilePicture")
    public ResponseEntity<String> setProfilePicture(Authentication authentication,
                                                        @RequestParam("file") MultipartFile file) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        assert userDetails != null;
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