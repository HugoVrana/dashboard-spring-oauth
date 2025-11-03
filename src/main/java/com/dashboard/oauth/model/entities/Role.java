package com.dashboard.oauth.model.entities;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "roles")
public class Role {
    private ObjectId _id;
    private String name;
    private Audit audit;
    @DBRef
    private List<Grant> grants;
}
