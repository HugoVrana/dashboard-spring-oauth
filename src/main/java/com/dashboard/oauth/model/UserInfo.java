package com.dashboard.oauth.model;

import com.dashboard.oauth.model.entities.Role;
import lombok.Data;
import org.bson.types.ObjectId;
import java.util.List;

@Data
public class UserInfo {
    private ObjectId id;
    private String email;
    private List<Role> role;
}
