package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.Role;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IRoleRepository extends MongoRepository<Role, ObjectId> {
}
