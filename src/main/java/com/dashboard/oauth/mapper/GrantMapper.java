package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.model.entities.Grant;
import org.springframework.stereotype.Service;

@Service
public class GrantMapper implements IGrantMapper {
    @Override
    public GrantRead toRead(Grant grant) {
        GrantRead grantRead = new GrantRead();
        grantRead.setId(grant.get_id().toHexString());
        grantRead.setName(grant.getName());
        grantRead.setDescription(grant.getDescription());
        return grantRead;
    }
}
