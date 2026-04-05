package com.dashboard.oauth.filter;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.service.JwtService;
import com.dashboard.oauth.service.UserDetailsImpl;
import io.qameta.allure.Story;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.Instant;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Story( "JWT Filter")
@DisplayName( "Jwt Auth Filter")
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private final Faker faker = new Faker();

    private String testEmail;
    private String testToken;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testEmail = faker.internet().emailAddress();
        testToken = faker.regexify("[a-zA-Z0-9]{32}");
        User testUser = createTestUser();
        userDetails = new UserDetailsImpl(testUser);
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(new ObjectId());
        user.setEmail(testEmail);
        user.setPassword(faker.internet().password());
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenAuthHeaderNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtService.extractUsername(testToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);
        when(jwtService.validateToken(testToken, userDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        // GrantsAuthentication uses email as the principal
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(testEmail);
    }

    @Test
    void doFilterInternal_shouldNotSetAuthentication_whenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtService.extractUsername(testToken)).thenReturn(testEmail);
        when(userDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);
        when(jwtService.validateToken(testToken, userDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenExceptionOccurs() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtService.extractUsername(testToken)).thenThrow(new RuntimeException("Token parsing failed"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldNotSetAuthentication_whenUsernameIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testToken);
        when(jwtService.extractUsername(testToken)).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
