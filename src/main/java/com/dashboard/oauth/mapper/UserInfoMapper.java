package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import org.springframework.stereotype.Service;

@Service
public class UserInfoMapper implements IUserInfoMapper {
    @Override
    public UserInfoRead toRead(UserInfo userInfo) {
        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setId(userInfo.getId().toHexString());
        userInfoRead.setEmail(userInfo.getEmail());
        RoleRead[] roleReadList = new RoleRead[]{};
        userInfoRead.setRoleReads(roleReadList);
        return userInfoRead;
    }
}
