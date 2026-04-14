package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IUserRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("User Details Service")
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IGrantRepository grantRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private final Faker faker = new Faker();

    private String testEmail;
    private User testUser;
    private Grant testGrant;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testEmail = faker.internet().emailAddress();

        testGrant = new Grant();
        testGrant.set_id(new ObjectId());
        testGrant.setName("test-grant");
        Audit grantAudit = new Audit();
        grantAudit.setCreatedAt(Instant.now());
        testGrant.setAudit(grantAudit);

        testRole = new Role();
        testRole.set_id(new ObjectId());
        testRole.setName("ROLE_TEST");
        testRole.setGrants(List.of(testGrant));
        Audit roleAudit = new Audit();
        roleAudit.setCreatedAt(Instant.now());
        testRole.setAudit(roleAudit);

        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(new ObjectId());
        user.setEmail(testEmail);
        user.setPassword(faker.internet().password());
        user.setRoles(List.of(testRole));

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    @Test
    @DisplayName("Get user by email returns UserDetails with grants loaded")
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.of(testUser));
        when(grantRepository.findAllById(List.of(testGrant.get_id())))
                .thenReturn(List.of(testGrant));

        UserDetails result = userDetailsService.loadUserByUsername(testEmail);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(testEmail);
        assertThat(result).isInstanceOf(UserDetailsImpl.class);
        verify(userRepository).findByEmailAndAudit_DeletedAtIsNull(testEmail);
        verify(grantRepository).findAllById(List.of(testGrant.get_id()));
    }

    @Test
    @DisplayName("Grants are batch-loaded and set on roles")
    void loadUserByUsername_shouldEagerLoadGrants() {
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.of(testUser));
        when(grantRepository.findAllById(List.of(testGrant.get_id())))
                .thenReturn(List.of(testGrant));

        UserDetails result = userDetailsService.loadUserByUsername(testEmail);

        User user = ((UserDetailsImpl) result).getUser();
        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().getFirst().getGrants())
                .hasSize(1)
                .extracting(Grant::getName)
                .containsExactly("test-grant");
    }

    @Test
    @DisplayName("Throw exception when user not found")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(testEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + testEmail);

        verify(userRepository).findByEmailAndAudit_DeletedAtIsNull(testEmail);
    }
}
