package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.model.entities.Grant;
import org.bson.types.ObjectId;
import java.util.Optional;

public interface IGrantService {
    Optional<Grant> getGrantByName(String name);

    Optional<Grant> getGrantById(ObjectId id);

    GrantRead createGrant(GrantCreate grant);
}
