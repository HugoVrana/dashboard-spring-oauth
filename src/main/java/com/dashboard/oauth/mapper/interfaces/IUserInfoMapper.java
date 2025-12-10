package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;

public interface IUserInfoMapper {
    UserInfoRead toRead(UserInfo userInfo);
}
