package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IUserRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
}
