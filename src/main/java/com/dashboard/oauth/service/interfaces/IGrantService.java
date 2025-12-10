package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.Grant;
import java.util.Optional;

public interface IGrantService {
    Optional<Grant> getGrantByName(String name);
    Grant createGrant(Grant grant);
}
