package org.magicat.intuition.model;

import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@ToString
@Document(collection = "Antibodies")
public class Antibody {

    @Id
    private String id;

    private String targetName;
    private String targetUniprotID;
    @TextIndexed(weight = 2)
    private String targetSymbol;

    @TextIndexed
    private String productName;  // contains unconjugated or conjugated
    @TextIndexed
    private String description;

    @TextIndexed
    private String RRID;  // can be used to pull up the Antibody Registry URL (antibodyregistry.org/AB_xxxxxxx)

    @TextIndexed
    private String vendorName;
    @TextIndexed
    private String vendorCode;  // catalog number

    private String reactivity;  // species where the antibody works on their variant of target
    private String isoType;
    private Boolean conjugated;
    private String conjugateType;
    private String antigen;
    private String antigenSequence;
    private String epitope;

    private String hostSpecies;  // species whose immune cells make the antibody

    private Integer antibodyType; // 0 = mono  1 = poly  2 = recombinant, 3 = poly(antigen purified), 4 = other
    private String cloneId;  // for monoclonal antibodies

    private String citations;
    private String size;
    private String concentration;
    private String storage;

    private Boolean hasComments;
    @Indexed(direction = IndexDirection.DESCENDING)
    private Integer commentCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetUniprotID() {
        return targetUniprotID;
    }

    public void setTargetUniprotID(String targetUniprotID) {
        this.targetUniprotID = targetUniprotID;
    }

    public String getTargetSymbol() {
        return targetSymbol;
    }

    public void setTargetSymbol(String targetSymbol) {
        this.targetSymbol = targetSymbol;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRRID() {
        return RRID;
    }

    public void setRRID(String RRID) {
        this.RRID = RRID;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getReactivity() {
        return reactivity;
    }

    public void setReactivity(String reactivity) {
        this.reactivity = reactivity;
    }

    public String getIsoType() {
        return isoType;
    }

    public void setIsoType(String isoType) {
        this.isoType = isoType;
    }

    public Boolean getConjugated() {
        return conjugated;
    }

    public void setConjugated(Boolean conjugated) {
        this.conjugated = conjugated;
    }

    public String getConjugateType() {
        return conjugateType;
    }

    public void setConjugateType(String conjugateType) {
        this.conjugateType = conjugateType;
    }

    public String getHostSpecies() {
        return hostSpecies;
    }

    public void setHostSpecies(String hostSpecies) {
        this.hostSpecies = hostSpecies;
    }

    public Integer getAntibodyType() {
        return antibodyType;
    }

    public void setAntibodyType(Integer antibodyType) {
        this.antibodyType = antibodyType;
    }

    public String getCloneId() {
        return cloneId;
    }

    public void setCloneId(String cloneId) {
        this.cloneId = cloneId;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public String getAntigen() {
        return antigen;
    }

    public void setAntigen(String antigen) {
        this.antigen = antigen;
    }

    public String getAntigenSequence() {
        return antigenSequence;
    }

    public void setAntigenSequence(String antigenSequence) {
        this.antigenSequence = antigenSequence;
    }

    public String getEpitope() {
        return epitope;
    }

    public void setEpitope(String epitope) {
        this.epitope = epitope;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getConcentration() {
        return concentration;
    }

    public void setConcentration(String concentration) {
        this.concentration = concentration;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public Boolean getHasComments() {
        return hasComments;
    }

    public void setHasComments(Boolean hasComments) {
        this.hasComments = hasComments;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

}
