package com.dashboard.oauth.service;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final IUserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;

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
}