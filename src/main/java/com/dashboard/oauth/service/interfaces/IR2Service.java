package com.dashboard.oauth.service.interfaces;

import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

public interface IR2Service {
    /**
     * Uploads a file to R2 and returns the public URL and R2 key.
     * @return String array: [0] = publicUrl, [1] = r2Key
     */
    String[] uploadFile(MultipartFile file, ObjectId userId);

    /**
     * Deletes a file from R2 by its key.
     */
    void deleteFile(String r2Key);
}
