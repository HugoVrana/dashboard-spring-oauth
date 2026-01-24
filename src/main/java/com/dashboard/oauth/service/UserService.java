package com.dashboard.oauth.service;

import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository userRepository;

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(ObjectId id) {
        return userRepository.getUserBy_idAndAudit_DeletedAtIsNull(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email){
        return userRepository.findByEmailAndAudit_DeletedAtIsNull(email);
    }


}