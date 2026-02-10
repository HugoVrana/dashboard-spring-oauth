package com.dashboard.oauth.controller;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.IR2Service;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final IR2Service r2Service;

    @PostMapping("{userId}/profilePicture")
    public ResponseEntity<String> setUserProfilePicture(@PathVariable("userId") String userId,
                                                        @RequestParam("file") MultipartFile file) {
        if (!ObjectId.isValid(userId)) {
            throw new ResourceNotFoundException("This id is invalid");
        }

        ObjectId objectId = new ObjectId(userId);
        Optional<User> optionalUser = userService.getUserById(objectId);
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("This id is invalid");
        }

        User user = optionalUser.get();

        // Delete old profile picture from R2 if exists
        if (user.getProfileImageR2Key() != null) {
            r2Service.deleteFile(user.getProfileImageR2Key());
        }

        // Upload new profile picture
        String[] result = r2Service.uploadFile(file, user.get_id());
        String publicUrl = result[0];
        String r2Key = result[1];

        user.setProfileImageUrl(publicUrl);
        user.setProfileImageR2Key(r2Key);
        userService.saveUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(publicUrl);
    }
}