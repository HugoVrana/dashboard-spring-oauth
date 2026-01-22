package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.User;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("User Service")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final Faker faker = new Faker();

    private ObjectId testUserId;
    private String testEmail;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        testEmail = faker.internet().emailAddress();
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail(testEmail);
        user.setPassword(faker.internet().password());
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    @Test
    @DisplayName("Save user")
    void saveUser_shouldReturnSavedUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.saveUser(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testEmail);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Get user by id")
    void getUserById_shouldReturnUser_whenUserExists() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserById(testUserId);

        assertThat(result).isPresent();
        assertThat(result.get().get_id()).isEqualTo(testUserId);
        verify(userRepository).getUserBy_idAndAudit_DeletedAtIsNull(testUserId);
    }

    @Test
    @DisplayName("Get nonexistent user by id")
    void getUserById_shouldReturnEmpty_whenUserNotFound() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(testUserId);

        assertThat(result).isEmpty();
        verify(userRepository).getUserBy_idAndAudit_DeletedAtIsNull(testUserId);
    }

    @Test
    @DisplayName("Get user by email")
    void getUserByEmail_shouldReturnUser_whenUserExists() {
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserByEmail(testEmail);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testEmail);
        verify(userRepository).findByEmailAndAudit_DeletedAtIsNull(testEmail);
    }

    @Test
    @DisplayName("Get nonexistent user by email")
    void getUserByEmail_shouldReturnEmpty_whenUserNotFound() {
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.empty());

        Optional<User> result = userService.getUserByEmail(testEmail);

        assertThat(result).isEmpty();
        verify(userRepository).findByEmailAndAudit_DeletedAtIsNull(testEmail);
    }
}
