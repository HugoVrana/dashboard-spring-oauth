package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private ObjectId _id;

    @Indexed(unique = true)
    private String email;

    private String password;

    @DBRef
    private List<Role> roles;
    
    private Audit audit;
}
