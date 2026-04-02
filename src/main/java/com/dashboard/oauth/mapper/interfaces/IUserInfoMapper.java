package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.user.User;

public interface IUserInfoMapper {
    UserInfo toUserInfo(User user);
    UserInfoRead toRead(UserInfo userInfo);
    UserSelfRead toSelfRead(User userInfo);
    UserAdminRead toAdminRead(User user);
}
