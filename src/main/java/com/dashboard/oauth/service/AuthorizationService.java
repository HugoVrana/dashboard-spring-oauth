package com.dashboard.oauth.service;

import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.v2.IntrospectionResponse;
import com.dashboard.oauth.dataTransferObject.v2.SubmitAuthorizeResult;
import com.dashboard.oauth.environment.JWTProperties;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.AuthorizationCode;
import com.dashboard.oauth.model.entities.AuthorizationRequest;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.MfaToken;
import com.dashboard.oauth.model.entities.OAuthClient;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IAuthorizationCodeRepository;
import com.dashboard.oauth.repository.IAuthorizationRequestRepository;
import com.dashboard.oauth.repository.IMfaTokenRepository;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IAuthorizationService;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.ITotpService;
import com.dashboard.oauth.service.interfaces.IUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.BadCredentialsException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService implements IAuthorizationService {

    private static final long AUTH_REQUEST_TTL_SECONDS = 600;  // 10 minutes
    private static final long AUTH_CODE_TTL_SECONDS = 600;      // 10 minutes
    private static final long MFA_TOKEN_TTL_SECONDS = 300;      // 5 minutes

    private final IOauthClientRepository clientRepository;
    private final IAuthorizationRequestRepository authRequestRepository;
    private final IAuthorizationCodeRepository authCodeRepository;
    private final IMfaTokenRepository mfaTokenRepository;
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final IJwtService jwtService;
    private final ITotpService totpService;
    private final IUserInfoMapper userInfoMapper;
    private final JWTProperties jwtProperties;
    private final IAuthenticationService authenticationService;
    private final IUserService userService;
    private final IGrantService grantService;
    private final Oauth2Properties oauth2Properties;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public AuthorizationRequest createAuthorizationRequest(
            String clientId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String scope,
            String state,
            String nonce) {

        OAuthClient client = clientRepository.findBy_idAndAudit_DeletedAtIsNull(new org.bson.types.ObjectId(clientId))
                .orElseThrow(() -> new InvalidRequestException("Unknown client_id"));

        if (!client.getRedirectUris().contains(redirectUri)) {
            throw new InvalidRequestException("redirect_uri is not registered for this client");
        }

        if (!"S256".equals(codeChallengeMethod)) {
            throw new InvalidRequestException("Only S256 code_challenge_method is supported");
        }

        if (codeChallenge == null || codeChallenge.isBlank()) {
            throw new InvalidRequestException("code_challenge is required");
        }

        if (scope != null && !scope.isBlank()) {
            List<String> allowedScopes = client.getAllowedScopes();
            for (String requested : scope.split(" ")) {
                if (!allowedScopes.contains(requested)) {
                    throw new InvalidRequestException("Scope not allowed: " + requested);
                }
            }
        }

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());

        AuthorizationRequest request = AuthorizationRequest.builder()
                .clientId(clientId)
                .redirectUri(redirectUri)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .scope(scope)
                .state(state)
                .nonce(nonce)
                .used(false)
                .expiryDate(Instant.now().plusSeconds(AUTH_REQUEST_TTL_SECONDS))
                .audit(audit)
                .build();

        return authRequestRepository.save(request);
    }

    @Override
    public AuthorizationRequest getAuthorizationRequest(String requestId) {
        if (!ObjectId.isValid(requestId)) {
            throw new InvalidRequestException("Invalid request_id");
        }
        return authRequestRepository.findByIdAndUsedFalseAndAudit_DeletedAtIsNull(new ObjectId(requestId))
                .orElseThrow(() -> new InvalidRequestException("Authorization request not found or already used"));
    }

    @Override
    public AuthorizationCode createAuthorizationCode(AuthorizationRequest request, String userId) {
        request.setUsed(true);
        authRequestRepository.save(request);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());

        AuthorizationCode code = AuthorizationCode.builder()
                .code(UUID.randomUUID().toString())
                .clientId(request.getClientId())
                .userId(userId)
                .redirectUri(request.getRedirectUri())
                .codeChallenge(request.getCodeChallenge())
                .codeChallengeMethod(request.getCodeChallengeMethod())
                .scope(request.getScope())
                .state(request.getState())
                .nonce(request.getNonce())
                .used(false)
                .expiryDate(Instant.now().plusSeconds(AUTH_CODE_TTL_SECONDS))
                .audit(audit)
                .build();

        return authCodeRepository.save(code);
    }

    @Override
    public String createMfaToken(String userId, AuthorizationRequest request) {
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());

        MfaToken mfaToken = MfaToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .authorizationRequestId(request.getId())
                .used(false)
                .expiryDate(Instant.now().plusSeconds(MFA_TOKEN_TTL_SECONDS))
                .audit(audit)
                .build();

        return mfaTokenRepository.save(mfaToken).getToken();
    }

    @Override
    public AuthorizationCode exchangeMfaToken(String mfaToken, String totpCode) {
        MfaToken token = mfaTokenRepository.findByTokenAndUsedFalseAndAudit_DeletedAtIsNull(mfaToken)
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired MFA token"));

        // Verify TOTP before consuming the token so a wrong code allows a retry
        if (!totpService.verifyTotp(token.getUserId(), totpCode)) {
            return null;
        }

        token.setUsed(true);
        mfaTokenRepository.save(token);

        AuthorizationRequest request = authRequestRepository
                .findByIdAndUsedFalseAndAudit_DeletedAtIsNull(token.getAuthorizationRequestId())
                .orElseThrow(() -> new InvalidRequestException("Authorization request not found or already used"));

        return createAuthorizationCode(request, token.getUserId());
    }

    @Override
    public AuthResponse exchangeCode(String code, String codeVerifier, String clientId, String redirectUri, String clientSecret) {
        AuthorizationCode authCode = authCodeRepository.findByCodeAndUsedFalseAndAudit_DeletedAtIsNull(code)
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired authorization code"));

        if (!authCode.getClientId().equals(clientId)) {
            throw new InvalidRequestException("client_id mismatch");
        }

        if (!authCode.getRedirectUri().equals(redirectUri)) {
            throw new InvalidRequestException("redirect_uri mismatch");
        }

        if (org.bson.types.ObjectId.isValid(clientId)) {
            clientRepository.findBy_idAndAudit_DeletedAtIsNull(new org.bson.types.ObjectId(clientId))
                    .ifPresent(client -> {
                        if (client.getClientSecret() != null) {
                            if (clientSecret == null || !passwordEncoder.matches(clientSecret, client.getClientSecret())) {
                                throw new InvalidRequestException("Invalid client credentials");
                            }
                        }
                    });
        }

        if (!verifyPkce(codeVerifier, authCode.getCodeChallenge())) {
            throw new InvalidRequestException("PKCE verification failed");
        }

        authCode.setUsed(true);
        authCodeRepository.save(authCode);

        User user = userRepository.findById(new ObjectId(authCode.getUserId()))
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        UserInfo userInfo = userInfoMapper.toUserInfo(user);
        String accessToken = jwtService.generateToken(userInfo);

        refreshTokenRepository.deleteByUserId(authCode.getUserId());
        ObjectId refreshTokenId = new ObjectId();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(authCode.getUserId())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        String idToken = null;
        String scope = authCode.getScope();
        if (scope != null && List.of(scope.split(" ")).contains("openid")) {
            idToken = jwtService.generateIdToken(userInfo, authCode.getClientId(), authCode.getNonce());
        }

        AuthResponse response = new AuthResponse();
        response.setUser(userInfoMapper.toRead(userInfo));
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenId.toHexString());
        response.setExpiresIn(jwtProperties.getExpiration());
        response.setIdToken(idToken);
        return response;
    }

    @Override
    public SubmitAuthorizeResult submitAuthorize(AuthorizationRequest authRequest, String username, String password) {
        User user = userService.getUserByEmail(username)
                .filter(u -> u.getAudit().getDeletedAt() == null)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(username);
        loginRequest.setPassword(password);
        authenticationService.login(loginRequest);

        if (user.getTwoFactorConfig() != null && Boolean.TRUE.equals(user.getTwoFactorConfig().getEnabled())) {
            String mfaToken = createMfaToken(user.get_id().toHexString(), authRequest);
            return new SubmitAuthorizeResult(true, mfaToken, null);
        }

        AuthorizationCode authCode = createAuthorizationCode(authRequest, user.get_id().toHexString());
        return new SubmitAuthorizeResult(false, null, authCode);
    }

    @Override
    public IntrospectionResponse introspect(String token) {
        IntrospectionResponse inactive = new IntrospectionResponse();
        inactive.setActive(false);

        try {
            List<String> grantNames = jwtService.extractClaim(token, c ->
                    ((List<?>) c.get("grants")).stream().map(String::valueOf).toList());
            String email = jwtService.extractUsername(token);
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            Optional<User> optionalUser = userService.getUserByEmail(email);
            if (optionalUser.isEmpty() || optionalUser.get().getAudit().getDeletedAt() != null) {
                return inactive;
            }

            List<String> activeGrants = new ArrayList<>();
            for (String grantName : grantNames) {
                Optional<Grant> optional = grantService.getGrantByName(grantName);
                if (optional.isEmpty() || optional.get().getAudit().getDeletedAt() != null) {
                    return inactive;
                }
                activeGrants.add(grantName);
            }

            IntrospectionResponse response = new IntrospectionResponse();
            response.setActive(true);
            response.setSub(email);
            response.setExp(expiration.toInstant().getEpochSecond());
            response.setScope(String.join(" ", activeGrants));
            return response;

        } catch (JwtException e) {
            return inactive;
        }
    }

    @Override
    public boolean validateClientSecret(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)), StandardCharsets.UTF_8);
            String clientSecret = decoded.contains(":") ? decoded.substring(decoded.indexOf(':') + 1) : decoded;
            return MessageDigest.isEqual(
                    oauth2Properties.getSecret().getBytes(StandardCharsets.UTF_8),
                    clientSecret.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean verifyPkce(String codeVerifier, String expectedChallenge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    expectedChallenge.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
