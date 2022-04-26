package org.magicat.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode
@ToString
@Document(collection = "CancerTypes")
public class CancerType {

    @Id
    private String id;

    private String mainType;
    private String code;
    private String subType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMainType() {
        return mainType;
    }

    public void setMainType(String mainType) {
        this.mainType = mainType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }
}
