package org.mskcc.knowledge.model;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Resources")
public class Resource {

    @Id
    private String id;

    private Binary media;
    private String filename;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Binary getMedia() {
        return media;
    }

    public void setMedia(Binary media) {
        this.media = media;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
