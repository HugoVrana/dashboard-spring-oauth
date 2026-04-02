package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.crypto.ServerKey;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IServerKeyRepository extends MongoRepository<ServerKey, ObjectId> {
}
