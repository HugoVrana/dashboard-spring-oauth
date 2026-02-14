package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.springframework.stereotype.Service;

@Service
public class UserInfoMapper implements IUserInfoMapper {
    private final RoleMapper roleMapper;
    private final R2Properties r2Properties;

    public UserInfoMapper(RoleMapper roleMapper, R2Properties r2Properties) {
        this.roleMapper = roleMapper;
        this.r2Properties = r2Properties;
    }

    public UserInfo toUserInfo(User user) {
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
    public UserInfoRead toRead(UserInfo userInfo) {
        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setId(userInfo.getId().toHexString());
        userInfoRead.setEmail(userInfo.getEmail());
        RoleRead[] roleReadList = new RoleRead[]{};
        for (Role r : userInfo.getRole()) {
            RoleRead rr = roleMapper.toRead(r);
            roleReadList = java.util.Arrays.copyOf(roleReadList, roleReadList.length + 1);
            roleReadList[roleReadList.length - 1] = rr;
        }
        userInfoRead.setRoleReads(roleReadList);
        userInfoRead.setProfileImageUrl(userInfo.getProfileImageUrl());
        return userInfoRead;
    }
}
