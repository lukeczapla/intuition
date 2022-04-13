package org.magicat.intuition.model;

import lombok.ToString;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@ToString
@Document(collection = "FullText")
public class FullText {

    @Id
    private String pmId;

    private String[] resourceIds;
    private String[] fileNames;

    //@TextIndexed   had to delete because can exceed 4 MB
    private String textEntry;
    private String HTMLEntry;

    private String documentResourceId;

    public String getPmId() {
        return pmId;
    }

    public void setPmId(String pmId) {
        this.pmId = pmId;
    }

    public String[] getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String[] resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String[] getFileNames() {
        return fileNames;
    }

    public void setFileNames(String[] fileNames) {
        this.fileNames = fileNames;
    }

    public String getTextEntry() {
        return textEntry;
    }

    public void setTextEntry(String textEntry) {
        this.textEntry = textEntry;
    }

    public String getHTMLEntry() {
        return HTMLEntry;
    }

    public void setHTMLEntry(String HTMLEntry) {
        this.HTMLEntry = HTMLEntry;
    }

    public String getDocumentResourceId() {
        return documentResourceId;
    }

    public void setDocumentResourceId(String documentResourceId) {
        this.documentResourceId = documentResourceId;
    }
}
