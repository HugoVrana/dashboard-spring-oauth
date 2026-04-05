package com.dashboard.oauth.service.authentication;

import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.environment.JWTProperties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.AuthenticationService;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.ILoginAttemptService;
import com.dashboard.oauth.service.interfaces.IRoleService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@Epic("Authentication")
@Feature("Authentication Service")
@Tag("service-authentication")
@ExtendWith(MockitoExtension.class)
public abstract class BaseAuthenticationServiceTest {

    @Mock
    protected IUserRepository userRepository;

    @Mock
    protected IRefreshTokenRepository refreshTokenRepository;

    @Mock
    protected IOauthClientRepository oauthClientRepository;

    @Mock
    protected PasswordEncoder passwordEncoder;

    @Mock
    protected IJwtService jwtService;

    @Mock
    protected AuthenticationManager authenticationManager;

    @Mock
    protected IUserInfoMapper userInfoMapper;

    @Mock
    protected IRoleService roleService;

    @Mock
    protected ILoginAttemptService loginAttemptService;

    @Mock
    protected IActivityFeedService activityFeedService;

    protected AuthenticationService authenticationService;
    protected JWTProperties jwtProperties;
    protected EmailProperties emailProperties;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setVerificationTokenExpirationMs(86400000L);
        emailProperties.setPasswordResetTokenExpirationMs(3600000L);

        jwtProperties = new JWTProperties();
        jwtProperties.setExpiration(86400000L);

        authenticationService = new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                oauthClientRepository,
                roleService,
                loginAttemptService,
                jwtService,
                passwordEncoder,
                authenticationManager,
                userInfoMapper,
                emailProperties,
                jwtProperties,
                activityFeedService
        );
    }
}
