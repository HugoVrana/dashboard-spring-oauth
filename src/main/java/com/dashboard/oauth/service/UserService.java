package com.dashboard.oauth.service;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final IUserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailProperties emailProperties;

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(ObjectId id) {
        return userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmailAndAudit_DeletedAtIsNull(email);
    }

    @Override
    public UserSelfRead getSelf(User user) {
        return userInfoMapper.toSelfRead(user);
    }

    @Override
    public UserSelfRead updateSelf(User user, UserSelfUpdate update) {
        if (update.getEmail() != null && !update.getEmail().isBlank()) {
            Optional<User> existingUser = getUserByEmail(update.getEmail());
            if (existingUser.isPresent() && !existingUser.get().get_id().equals(user.get_id())) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(update.getEmail());
        }

        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(update.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return userInfoMapper.toSelfRead(savedUser);
    }

    @Override
    public List<UserAdminRead> getUsers() {
        return userRepository.findAllByAudit_DeletedAtIsNull().stream()
                .map(userInfoMapper::toAdminRead)
                .toList();
    }

    @Override
    public List<UserAdminRead> searchUsers(String query) {
        return userRepository.searchByEmail(query).stream()
                .map(userInfoMapper::toAdminRead)
                .toList();
    }

    @Override
    public UserAdminRead getUserAdminReadById(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userInfoMapper.toAdminRead(user);
    }

    @Override
    public UserAdminRead updateUser(ObjectId id, UserAdminUpdate update) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (update.getEmail() != null && !update.getEmail().isBlank()) {
            Optional<User> existing = getUserByEmail(update.getEmail());
            if (existing.isPresent() && !existing.get().get_id().equals(id)) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(update.getEmail());
        }

        if (user.getAudit() != null) {
            user.getAudit().setUpdatedAt(Instant.now());
        }

        return userInfoMapper.toAdminRead(userRepository.save(user));
    }

    @Override
    public void deleteUser(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAudit() == null) {
            throw new InvalidRequestException("User audit is missing");
        }

        user.getAudit().setDeletedAt(Instant.now());
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(id.toHexString());
    }

    @Override
    public void blockUser(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new ConflictException("User is already blocked");
        }

        userRepository.lockUser(id, Instant.now());
    }

    @Override
    public void unblockUser(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getLocked())) {
            throw new ConflictException("User is not blocked");
        }

        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        if (user.getAudit() != null) {
            user.getAudit().setUpdatedAt(Instant.now());
        }
        userRepository.save(user);
    }

    @Override
    public void resendVerificationEmail(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ConflictException("User email is already verified");
        }

        VerificationToken token = new VerificationToken();
        token.set_id(new ObjectId());
        token.setCreatedAt(Instant.now());
        token.setExpiryDate(Instant.now().plusMillis(emailProperties.getVerificationTokenExpirationMs()));
        token.setUsed(false);

        user.setEmailVerificationToken(token);
        userRepository.save(user);
    }

    @Override
    public void triggerPasswordReset(ObjectId id) {
        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(new ObjectId());
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiryDate(Instant.now().plusMillis(emailProperties.getPasswordResetTokenExpirationMs()));
        resetToken.setUsed(false);

        user.setPasswordResetToken(resetToken);
        userRepository.save(user);
    }
}
