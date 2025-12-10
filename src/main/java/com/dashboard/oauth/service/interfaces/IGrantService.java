package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.Grant;
import org.bson.types.ObjectId;

import java.util.Optional;

public interface IGrantService {
    Optional<Grant> getGrantByName(String name);
    Optional<Grant> getGrantById(ObjectId id);
    Grant createGrant(Grant grant);
}
