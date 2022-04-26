package org.magicat.model;

import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@ToString
@Document(collection = "ArticleIndex")
public class ArticleIndex {

    @Id
    private String pmId;

    private String topics;

    public String getPmId() {
        return pmId;
    }

    public void setPmId(String pmId) {
        this.pmId = pmId;
    }


    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }
}
