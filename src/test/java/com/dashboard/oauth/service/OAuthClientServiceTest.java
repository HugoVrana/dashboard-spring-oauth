package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.mapper.interfaces.IOAuthClientMapper;
import com.dashboard.oauth.model.entities.OAuthClient;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OAuthClient Service")
@ExtendWith(MockitoExtension.class)
class OAuthClientServiceTest {

    @Mock
    private IOauthClientRepository oauthClientRepository;

    @Mock
    private IOAuthClientMapper oAuthClientMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IActivityFeedService activityFeedService;

    @InjectMocks
    private OAuthClientService oAuthClientService;

    private ObjectId testClientId;
    private OAuthClient testClient;

    @BeforeEach
    void setUp() {
        testClientId = new ObjectId();
        testClient = createTestClient();
    }

    private OAuthClient createTestClient() {
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        audit.setUpdatedAt(Instant.now());

        return OAuthClient.builder()
                ._id(testClientId)
                .clientSecret("hashed-secret")
                .redirectUris(List.of("https://app.example.com/callback"))
                .allowedHosts(List.of("https://app.example.com"))
                .allowedScopes(List.of("openid", "profile"))
                .audit(audit)
                .build();
    }

    @Test
    @DisplayName("getClient returns DTO when client exists")
    void getClient_shouldReturnRead_whenClientExists() {
        OAuthClientRead expectedRead = new OAuthClientRead();
        expectedRead.setId(testClientId.toHexString());

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));
        when(oAuthClientMapper.toRead(testClient)).thenReturn(expectedRead);

        OAuthClientRead result = oAuthClientService.getClient(testClientId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testClientId.toHexString());
    }

    @Test
    @DisplayName("getClient throws ResourceNotFoundException when client not found")
    void getClient_shouldThrow_whenClientNotFound() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthClientService.getClient(testClientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createClient saves client with hashed secret and returns raw secret once")
    void createClient_shouldReturnCreatedWithRawSecret() {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of("openid", "profile"));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-secret");
        when(oauthClientRepository.save(any())).thenReturn(testClient);

        OAuthClientCreated result = oAuthClientService.createClient(request);

        assertThat(result).isNotNull();
        assertThat(result.getClientSecret()).isNotNull().isNotBlank();

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(oauthClientRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("hashed-secret");
        assertThat(captor.getValue().getRedirectUris()).containsExactly("https://app.example.com/callback");
        assertThat(captor.getValue().getAllowedHosts()).containsExactly("https://app.example.com");
    }

    @Test
    @DisplayName("createClient raw secret differs from stored hash")
    void createClient_rawSecretShouldDifferFromStoredHash() {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of("https://app.example.com"));
        request.setAllowedScopes(List.of("openid"));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-secret");
        when(oauthClientRepository.save(any())).thenReturn(testClient);

        OAuthClientCreated result = oAuthClientService.createClient(request);

        assertThat(result.getClientSecret()).isNotEqualTo("hashed-secret");
    }

    @Test
    @DisplayName("createClient throws when redirect URI host is missing from allowed hosts")
    void createClient_shouldThrow_whenRedirectHostNotAllowed() {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedHosts(List.of("https://other.example.com"));
        request.setAllowedScopes(List.of("openid"));

        assertThatThrownBy(() -> oAuthClientService.createClient(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("redirect URI host");
    }

    @Test
    @DisplayName("deleteClient soft deletes client when found")
    void deleteClient_shouldSoftDelete_whenClientFound() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));

        oAuthClientService.deleteClient(testClientId);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(oauthClientRepository).save(captor.capture());
        assertThat(captor.getValue().getAudit().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteClient throws ResourceNotFoundException when client not found")
    void deleteClient_shouldThrow_whenClientNotFound() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthClientService.deleteClient(testClientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("rotateSecret returns new raw secret and saves hashed version")
    void rotateSecret_shouldReturnNewRawSecret() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hashed-secret");
        when(oauthClientRepository.save(any())).thenReturn(testClient);

        OAuthClientCreated result = oAuthClientService.rotateSecret(testClientId);

        assertThat(result.getClientSecret()).isNotNull().isNotBlank();
        assertThat(result.getClientSecret()).isNotEqualTo("new-hashed-secret");

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(oauthClientRepository).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("new-hashed-secret");
    }

    @Test
    @DisplayName("rotateSecret throws ResourceNotFoundException when client not found")
    void rotateSecret_shouldThrow_whenClientNotFound() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthClientService.rotateSecret(testClientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("validateClientCredentials returns true when clientId and secret are valid")
    void validateClientCredentials_shouldReturnTrue_whenValid() {
        String raw = "correct-secret";
        String header = "Basic " + Base64.getEncoder()
                .encodeToString((testClientId.toHexString() + ":" + raw).getBytes(StandardCharsets.UTF_8));

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches(raw, testClient.getClientSecret())).thenReturn(true);

        assertThat(oAuthClientService.validateClientCredentials(header)).isTrue();
    }

    @Test
    @DisplayName("validateClientCredentials returns false when secret is wrong")
    void validateClientCredentials_shouldReturnFalse_whenSecretWrong() {
        String header = "Basic " + Base64.getEncoder()
                .encodeToString((testClientId.toHexString() + ":wrong-secret").getBytes(StandardCharsets.UTF_8));

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThat(oAuthClientService.validateClientCredentials(header)).isFalse();
    }

    @Test
    @DisplayName("validateClientCredentials returns false when client not found")
    void validateClientCredentials_shouldReturnFalse_whenClientNotFound() {
        String header = "Basic " + Base64.getEncoder()
                .encodeToString((testClientId.toHexString() + ":secret").getBytes(StandardCharsets.UTF_8));

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.empty());

        assertThat(oAuthClientService.validateClientCredentials(header)).isFalse();
    }

    @Test
    @DisplayName("validateClientCredentials returns false when header is null or malformed")
    void validateClientCredentials_shouldReturnFalse_whenHeaderInvalid() {
        assertThat(oAuthClientService.validateClientCredentials(null)).isFalse();
        assertThat(oAuthClientService.validateClientCredentials("Bearer token")).isFalse();
        assertThat(oAuthClientService.validateClientCredentials("Basic !!!not-base64!!!")).isFalse();
    }

    @Test
    @DisplayName("isRegisteredClient returns true when client exists")
    void isRegisteredClient_shouldReturnTrue_whenClientExists() {
        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));

        assertThat(oAuthClientService.isRegisteredClient(testClientId.toHexString())).isTrue();
    }

    @Test
    @DisplayName("isAllowedHost returns true when Origin matches allowed host")
    void isAllowedHost_shouldReturnTrue_whenOriginMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://app.example.com");

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));

        assertThat(oAuthClientService.isAllowedHost(testClientId.toHexString(), request)).isTrue();
    }

    @Test
    @DisplayName("isAllowedHost returns true when Referer matches allowed host")
    void isAllowedHost_shouldReturnTrue_whenRefererMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://app.example.com/login");

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));

        assertThat(oAuthClientService.isAllowedHost(testClientId.toHexString(), request)).isTrue();
    }

    @Test
    @DisplayName("isAllowedHost returns false when host is not allowed")
    void isAllowedHost_shouldReturnFalse_whenOriginDoesNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example.com");

        when(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(testClientId))
                .thenReturn(Optional.of(testClient));

        assertThat(oAuthClientService.isAllowedHost(testClientId.toHexString(), request)).isFalse();
    }
}
