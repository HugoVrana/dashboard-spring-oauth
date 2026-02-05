package com.dashboard.oauth.service.authentication;

import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.AuthenticationService;
import com.dashboard.oauth.service.interfaces.IJwtService;
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
import org.springframework.test.util.ReflectionTestUtils;

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
    protected PasswordEncoder passwordEncoder;

    @Mock
    protected IJwtService jwtService;

    @Mock
    protected AuthenticationManager authenticationManager;

    @Mock
    protected IUserInfoMapper userInfoMapper;

    @Mock
    protected IRoleMapper roleMapper;

    @Mock
    protected IGrantMapper grantMapper;

    @Mock
    protected IRoleService roleService;

    protected AuthenticationService authenticationService;

    protected static final Long JWT_EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setVerificationTokenExpirationMs(86400000L);
        emailProperties.setPasswordResetTokenExpirationMs(3600000L);

        authenticationService = new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                userInfoMapper,
                roleMapper,
                grantMapper,
                emailProperties,
                roleService
        );

        ReflectionTestUtils.setField(authenticationService, "jwtExpiration", JWT_EXPIRATION);
    }
}
