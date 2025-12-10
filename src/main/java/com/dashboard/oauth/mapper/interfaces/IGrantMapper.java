package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.model.entities.Grant;

public interface IGrantMapper {
    GrantRead toRead(Grant grant);
}
