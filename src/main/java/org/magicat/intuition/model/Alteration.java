package org.mskcc.knowledge.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * This class contains the potential alterations (things mentioned in the literature but not yet part of Variants that are official items)
 */
@Document(collection = "Alterations")
public class Alteration {

    @Id
    private ObjectId id;

    private String term; // "mutation" term
    private String gene; // hugo Symbol

    private List<String> contexts; // the textual cases encountered
    private List<String> pmIds;
    private List<String> snippets;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public void setContexts(List<String> contexts) {
        this.contexts = contexts;
    }

    public List<String> getPmIds() {
        return pmIds;
    }

    public void setPmIds(List<String> pmIds) {
        this.pmIds = pmIds;
    }

    public List<String> getSnippets() {
        return snippets;
    }

    public void setSnippets(List<String> snippets) {
        this.snippets = snippets;
    }
}
