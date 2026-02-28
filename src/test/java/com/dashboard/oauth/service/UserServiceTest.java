package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IUserRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("User Service")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserInfoMapper userInfoMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        userService = new UserService(userRepository, userInfoMapper, passwordEncoder);
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

    @Test
    @DisplayName("Get self returns user info")
    void getSelf_shouldReturnUserSelfRead() {
        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(testEmail);

        when(userInfoMapper.toSelfRead(testUser)).thenReturn(expectedResponse);

        UserSelfRead result = userService.getSelf(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId.toHexString());
        assertThat(result.getEmail()).isEqualTo(testEmail);
        verify(userInfoMapper).toSelfRead(testUser);
    }

    @Test
    @DisplayName("Update self with new email")
    void updateSelf_shouldUpdateEmail() {
        String newEmail = faker.internet().emailAddress();
        UserSelfUpdate update = new UserSelfUpdate();
        update.setEmail(newEmail);

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(newEmail);

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(newEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);

        UserSelfRead result = userService.updateSelf(testUser, update);

        assertThat(result.getEmail()).isEqualTo(newEmail);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update self with new password")
    void updateSelf_shouldUpdatePassword() {
        String newPassword = "newSecurePassword123";
        UserSelfUpdate update = new UserSelfUpdate();
        update.setPassword(newPassword);

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        userService.updateSelf(testUser, update);

        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update self throws conflict when email in use by another user")
    void updateSelf_shouldThrowConflict_whenEmailInUse() {
        String existingEmail = faker.internet().emailAddress();
        UserSelfUpdate update = new UserSelfUpdate();
        update.setEmail(existingEmail);

        User otherUser = createTestUser();
        otherUser.set_id(new ObjectId());
        otherUser.setEmail(existingEmail);

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(existingEmail))
                .thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateSelf(testUser, update))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Update self allows same email for same user")
    void updateSelf_shouldAllowSameEmail() {
        UserSelfUpdate update = new UserSelfUpdate();
        update.setEmail(testEmail);

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(testEmail);

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);

        UserSelfRead result = userService.updateSelf(testUser, update);

        assertThat(result.getEmail()).isEqualTo(testEmail);
        verify(userRepository).save(testUser);
    }
}
