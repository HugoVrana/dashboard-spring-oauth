package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public final class UserInfoMapper implements IUserInfoMapper {
    private final RoleMapper roleMapper;
    private final GrantMapper grantMapper;
    private final R2Properties r2Properties;

    @Override
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

    @Override
    public UserSelfRead toSelfRead(final User userInfo) {
        UserSelfRead userSelfRead = new UserSelfRead();
        userSelfRead.setId(userInfo.get_id().toHexString());
        userSelfRead.setEmail(userInfo.getEmail());
        userSelfRead.setEmailVerified(userInfo.isEmailVerified());
        userSelfRead.setEmailVerifiedAt(userInfo.getEmailVerifiedAt());
        if (userInfo.getProfileImageId() != null) {
            String imageUrl = r2Properties.buildPublicUrl(userInfo.get_id(), userInfo.getProfileImageId());
            userSelfRead.setProfileImageUrl(imageUrl);
        }
        userSelfRead.setLocked(userInfo.isLocked());
        return userSelfRead;
    }

    @Override
    public UserAdminRead toAdminRead(final User user) {
        UserAdminRead read = new UserAdminRead();
        read.setId(user.get_id().toHexString());
        read.setEmail(user.getEmail());
        read.setEmailVerified(user.isEmailVerified());
        read.setEmailVerifiedAt(user.getEmailVerifiedAt());
        read.setLocked(user.isLocked());
        read.setFailedLoginAttempts(user.getFailedLoginAttempts());
        if (user.getProfileImageId() != null) {
            read.setProfileImageUrl(r2Properties.buildPublicUrl(user.get_id(), user.getProfileImageId()));
        }
        if (user.getRoles() != null) {
            List<RoleRead> roleReads = new ArrayList<>();
            for (Role role : user.getRoles()) {
                RoleRead roleRead = roleMapper.toRead(role);
                List<GrantRead> grantReads = new ArrayList<>();
                if (role.getGrants() != null) {
                    for (Grant grant : role.getGrants()) {
                        grantReads.add(grantMapper.toRead(grant));
                    }
                }
                roleRead.setGrants(grantReads);
                roleReads.add(roleRead);
            }
            read.setRoles(roleReads);
        }
        if (user.getAudit() != null) {
            read.setCreatedAt(user.getAudit().getCreatedAt());
            read.setDeletedAt(user.getAudit().getDeletedAt());
        }
        return read;
    }
}