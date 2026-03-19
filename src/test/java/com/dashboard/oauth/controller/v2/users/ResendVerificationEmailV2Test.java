package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/user/{id}/resend-verification")
class ResendVerificationEmailV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 204 when verification email resent successfully")
    void shouldReturn204_whenResent() throws Exception {
        doNothing().when(userService).resendVerificationEmail(any());

        mockMvc.perform(post("/api/v2/user/{id}/resend-verification", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 409 when email is already verified")
    void shouldReturn409_whenAlreadyVerified() throws Exception {
        doThrow(new ConflictException("User email is already verified"))
                .when(userService).resendVerificationEmail(any());

        mockMvc.perform(post("/api/v2/user/{id}/resend-verification", testUserId))
                .andExpect(status().isConflict());
    }
}
