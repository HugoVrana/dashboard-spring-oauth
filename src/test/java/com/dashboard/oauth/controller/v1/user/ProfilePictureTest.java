package com.dashboard.oauth.controller.v1.user;

import com.dashboard.oauth.environment.R2Properties;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Profile Picture")
@Story("Set Profile Picture")
class ProfilePictureTest extends BaseUserControllerTest {

    @Test
    @DisplayName("Set profile picture successfully")
    @Description("Should upload profile picture and return the public URL")
    void setProfilePicture_shouldReturnPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        String expectedUrl = "https://test-account.r2.cloudflarestorage.com/test-bucket/" + testUserId.toHexString() + "/uuid_profile.png";
        String expectedKey = testUserId.toHexString() + "/uuid_profile.png";
        String expectedImageId = new ObjectId().toHexString();

        when(r2Service.uploadFile(any(), any())).thenReturn(new String[]{expectedUrl, expectedKey, expectedImageId});
        when(userService.saveUser(any())).thenReturn(testUser);

        mockMvc.perform(multipart("/api/v1/user/profilePicture")
                        .file(file)
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedUrl));

        verify(r2Service).uploadFile(any(), eq(testUserId));
        verify(userService).saveUser(any());
    }

    @Test
    @DisplayName("Set profile picture deletes old image")
    @Description("Should delete old profile picture from R2 before uploading new one")
    void setProfilePicture_shouldDeleteOldImage() throws Exception {
        ObjectId oldImageId = new ObjectId();
        testUser.setProfileImageId(oldImageId);
        String oldR2Key = R2Properties.buildR2Key(testUserId, oldImageId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new-profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "new image content".getBytes()
        );

        String newUrl = "https://test-account.r2.cloudflarestorage.com/test-bucket/new_image.png";
        String newKey = testUserId.toHexString() + "/new_image.png";
        String newImageId = new ObjectId().toHexString();

        when(r2Service.uploadFile(any(), any())).thenReturn(new String[]{newUrl, newKey, newImageId});
        when(userService.saveUser(any())).thenReturn(testUser);

        mockMvc.perform(multipart("/api/v1/user/profilePicture")
                        .file(file)
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isCreated());

        verify(r2Service).deleteFile(oldR2Key);
        verify(r2Service).uploadFile(any(), eq(testUserId));
    }

    @Test
    @DisplayName("Set profile picture without existing image")
    @Description("Should not attempt to delete when user has no existing profile picture")
    void setProfilePicture_shouldNotDeleteWhenNoExistingImage() throws Exception {
        testUser.setProfileImageId(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "test content".getBytes()
        );

        String expectedUrl = "https://test.r2.cloudflarestorage.com/bucket/image.png";
        String expectedKey = "userId/image.png";
        String expectedImageId = new ObjectId().toHexString();

        when(r2Service.uploadFile(any(), any())).thenReturn(new String[]{expectedUrl, expectedKey, expectedImageId});
        when(userService.saveUser(any())).thenReturn(testUser);

        mockMvc.perform(multipart("/api/v1/user/profilePicture")
                        .file(file)
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isCreated());

        verify(r2Service, never()).deleteFile(any());
        verify(r2Service).uploadFile(any(), eq(testUserId));
    }
}
