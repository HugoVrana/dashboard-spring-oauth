package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "grants")
public class Grant {
    @Id
    private ObjectId _id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private Audit audit;
}
