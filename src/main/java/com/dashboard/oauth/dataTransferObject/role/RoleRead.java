package com.dashboard.oauth.dataTransferObject.role;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import lombok.Data;
import java.util.List;

@Data
public class RoleRead {
    private String id;
    private String name;
    private List<GrantRead> grants;
}
