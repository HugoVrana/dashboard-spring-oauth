package com.dashboard.oauth.controller.v2.oauthclients;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/oauthclients/")
class CreateClientV2Test extends BaseV2OAuthClientControllerTest {

    @Test
    @DisplayName("Should return 200 with client and secret on creation")
    void shouldReturn200WithClientAndSecret() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of("openid", "profile"));

        when(oAuthClientService.createClient(any())).thenReturn(testClientCreated);

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId))
                .andExpect(jsonPath("$.clientSecret").value(testClientSecret));
    }

    @Test
    @DisplayName("Should return 400 when redirectUris is empty")
    void shouldReturn400_whenRedirectUrisEmpty() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of());
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of("openid"));

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when allowedScopes is empty")
    void shouldReturn400_whenAllowedScopesEmpty() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of());

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when redirectUri is not a valid http/https URI")
    void shouldReturn400_whenRedirectUriInvalid() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("not-a-valid-uri"));
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of("openid"));

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when allowedHosts is empty")
    void shouldReturn400_whenAllowedHostsEmpty() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of());
        request.setAllowedScopes(List.of("openid"));

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
