package org.magicat.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Document(collection = "Sentences")
public class Sentences {

    @Id
    private String id;

    private String[] text;

    private String pmId;

    private Integer index;

    private String[] questions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getText() {
        return text;
    }

    public void setText(String[] text) {
        this.text = text;
    }

    public String getPmId() {
        return pmId;
    }

    public void setPmId(String pmId) {
        this.pmId = pmId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String[] getQuestions() {
        return questions;
    }

    public void setQuestions(String[] questions) {
        this.questions = questions;
    }
}
