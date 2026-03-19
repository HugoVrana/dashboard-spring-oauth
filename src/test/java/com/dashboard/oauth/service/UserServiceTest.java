package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private IRefreshTokenRepository refreshTokenRepository;

    @Mock
    private IUserInfoMapper userInfoMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailProperties emailProperties;

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
        userService = new UserService(userRepository, refreshTokenRepository, userInfoMapper, passwordEncoder, emailProperties);
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

    @Test
    @DisplayName("Get all users returns list")
    void getUsers_shouldReturnAllUsers() {
        UserAdminRead adminRead = new UserAdminRead();
        adminRead.setEmail(testEmail);

        when(userRepository.findAllByAudit_DeletedAtIsNull()).thenReturn(List.of(testUser));
        when(userInfoMapper.toAdminRead(testUser)).thenReturn(adminRead);

        List<UserAdminRead> result = userService.getUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Search users returns matching users")
    void searchUsers_shouldReturnMatchingUsers() {
        UserAdminRead adminRead = new UserAdminRead();
        adminRead.setEmail(testEmail);

        when(userRepository.searchByEmail("test")).thenReturn(List.of(testUser));
        when(userInfoMapper.toAdminRead(testUser)).thenReturn(adminRead);

        List<UserAdminRead> result = userService.searchUsers("test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Get user admin read by id returns DTO when user exists")
    void getUserAdminReadById_shouldReturnAdminRead_whenExists() {
        UserAdminRead adminRead = new UserAdminRead();
        adminRead.setEmail(testEmail);

        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userInfoMapper.toAdminRead(testUser)).thenReturn(adminRead);

        UserAdminRead result = userService.getUserAdminReadById(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Get user admin read by id throws ResourceNotFoundException when not found")
    void getUserAdminReadById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserAdminReadById(testUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update user updates email when email is available")
    void updateUser_shouldUpdateEmail_whenEmailAvailable() {
        String newEmail = faker.internet().emailAddress();
        UserAdminUpdate update = new UserAdminUpdate();
        update.setEmail(newEmail);

        UserAdminRead expectedResponse = new UserAdminRead();
        expectedResponse.setEmail(newEmail);

        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(newEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toAdminRead(any(User.class))).thenReturn(expectedResponse);

        UserAdminRead result = userService.updateUser(testUserId, update);

        assertThat(result.getEmail()).isEqualTo(newEmail);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Update user throws ConflictException when email is taken by another user")
    void updateUser_shouldThrowConflict_whenEmailTakenByOtherUser() {
        String existingEmail = faker.internet().emailAddress();
        UserAdminUpdate update = new UserAdminUpdate();
        update.setEmail(existingEmail);

        User otherUser = createTestUser();
        otherUser.set_id(new ObjectId());
        otherUser.setEmail(existingEmail);

        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(existingEmail))
                .thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateUser(testUserId, update))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Delete user soft-deletes the user and revokes refresh tokens")
    void deleteUser_shouldSoftDeleteUser() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteUser(testUserId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAudit().getDeletedAt()).isNotNull();
        verify(refreshTokenRepository).deleteByUserId(testUserId.toHexString());
    }

    @Test
    @DisplayName("Delete user throws ResourceNotFoundException when not found")
    void deleteUser_shouldThrowResourceNotFoundException_whenNotFound() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(testUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Block user locks user when not already locked")
    void blockUser_shouldLockUser_whenNotAlreadyLocked() {
        testUser.setLocked(false);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        userService.blockUser(testUserId);

        verify(userRepository).lockUser(eq(testUserId), any(Instant.class));
    }

    @Test
    @DisplayName("Block user throws ConflictException when already locked")
    void blockUser_shouldThrowConflict_whenAlreadyLocked() {
        testUser.setLocked(true);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.blockUser(testUserId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Unblock user unlocks user when locked")
    void unblockUser_shouldUnlockUser_whenLocked() {
        testUser.setLocked(true);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.unblockUser(testUserId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getLocked()).isFalse();
    }

    @Test
    @DisplayName("Unblock user throws ConflictException when not locked")
    void unblockUser_shouldThrowConflict_whenNotLocked() {
        testUser.setLocked(false);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.unblockUser(testUserId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Resend verification email creates new token when email not verified")
    void resendVerificationEmail_shouldCreateNewToken_whenEmailNotVerified() {
        testUser.setEmailVerified(false);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(emailProperties.getVerificationTokenExpirationMs()).thenReturn(86400000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.resendVerificationEmail(testUserId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmailVerificationToken()).isNotNull();
        assertThat(saved.getEmailVerificationToken().getEmailSentAt()).isNull();
    }

    @Test
    @DisplayName("Resend verification email throws ConflictException when email already verified")
    void resendVerificationEmail_shouldThrowConflict_whenEmailAlreadyVerified() {
        testUser.setEmailVerified(true);
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.resendVerificationEmail(testUserId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Trigger password reset creates reset token")
    void triggerPasswordReset_shouldCreateResetToken() {
        when(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(emailProperties.getPasswordResetTokenExpirationMs()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.triggerPasswordReset(testUserId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordResetToken()).isNotNull();
    }
}
