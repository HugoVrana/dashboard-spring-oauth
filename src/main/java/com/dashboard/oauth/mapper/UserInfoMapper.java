package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public final class UserInfoMapper implements IUserInfoMapper {
    private final RoleMapper roleMapper;
    private final GrantMapper grantMapper;
    private final R2Properties r2Properties;

    public UserInfoMapper(RoleMapper roleMapper, GrantMapper grantMapper, R2Properties r2Properties) {
        this.roleMapper = roleMapper;
        this.grantMapper = grantMapper;
        this.r2Properties = r2Properties;
    }

    public UserInfo toUserInfo(final User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.get_id());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRoles());
        if (user.getProfileImageId() != null) {
            userInfo.setProfileImageUrl(r2Properties.buildPublicUrl(user.get_id(), user.getProfileImageId()));
        }
        return userInfo;
    }

    @Override
    public UserInfoRead toRead(final UserInfo userInfo) {
        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setId(userInfo.getId().toHexString());
        userInfoRead.setEmail(userInfo.getEmail());

        List<RoleRead> roleReadList = new ArrayList<>();
        for (Role role : userInfo.getRole()) {
            RoleRead roleRead = roleMapper.toRead(role);
            List<GrantRead> grantReads = new ArrayList<>();
            for (Grant grant : role.getGrants()) {
                grantReads.add(grantMapper.toRead(grant));
            }
            roleRead.setGrants(grantReads);
            roleReadList.add(roleRead);
        }
        userInfoRead.setRoleReads(roleReadList.toArray(new RoleRead[0]));
        userInfoRead.setProfileImageUrl(userInfo.getProfileImageUrl());
        return userInfoRead;
    }
}