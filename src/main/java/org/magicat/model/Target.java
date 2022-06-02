package org.magicat.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "Targets")
public class Target {
    @Id
    private ObjectId id;

    private String UniprotID;
    private String UniprotFunction;

    @TextIndexed(weight = 2)
    private String symbol;
    @TextIndexed
    private String name;
    private String synonyms;

    private String family;
    private String familyDetails;

    private Integer antibodyCount;
    private Double pubmedScore;

    private String PDB_IDs;

    private String AntibodypediaURL;
    private String NCBIGeneURL;
    private Boolean cBioPortal;

    private String refSeq;
    private Long startPosition, endPosition;
    private Boolean forward;

    private Integer monoclonalCount, polyclonalCount, polyclonalAntigenPurifiedCount, recombinantCount, otherCount;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUniprotID() {
        return UniprotID;
    }

    public void setUniprotID(String uniprotID) {
        UniprotID = uniprotID;
    }

    public String getUniprotFunction() {
        return UniprotFunction;
    }

    public void setUniprotFunction(String uniprotFunction) {
        UniprotFunction = uniprotFunction;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getFamilyDetails() {
        return familyDetails;
    }

    public void setFamilyDetails(String familyDetails) {
        this.familyDetails = familyDetails;
    }

    public Integer getAntibodyCount() {
        return antibodyCount;
    }

    public void setAntibodyCount(Integer antibodyCount) {
        this.antibodyCount = antibodyCount;
    }

    public Double getPubmedScore() {
        return pubmedScore;
    }

    public void setPubmedScore(Double pubmedScore) {
        this.pubmedScore = pubmedScore;
    }

    public String getPDB_IDs() {
        return PDB_IDs;
    }

    public void setPDB_IDs(String PDB_IDs) {
        this.PDB_IDs = PDB_IDs;
    }

    public String getAntibodypediaURL() {
        return AntibodypediaURL;
    }

    public void setAntibodypediaURL(String antibodypediaURL) {
        AntibodypediaURL = antibodypediaURL;
    }

    public String getNCBIGeneURL() {
        return NCBIGeneURL;
    }

    public void setNCBIGeneURL(String NCBIGeneURL) {
        this.NCBIGeneURL = NCBIGeneURL;
    }

    public Boolean getcBioPortal() {
        return cBioPortal;
    }

    public void setcBioPortal(Boolean cBioPortal) {
        this.cBioPortal = cBioPortal;
    }

    public Integer getMonoclonalCount() {
        return monoclonalCount;
    }

    public void setMonoclonalCount(Integer monoclonalCount) {
        this.monoclonalCount = monoclonalCount;
    }

    public Integer getPolyclonalCount() {
        return polyclonalCount;
    }

    public void setPolyclonalCount(Integer polyclonalCount) {
        this.polyclonalCount = polyclonalCount;
    }

    public Integer getPolyclonalAntigenPurifiedCount() {
        return polyclonalAntigenPurifiedCount;
    }

    public void setPolyclonalAntigenPurifiedCount(Integer polyclonalAntigenPurifiedCount) {
        this.polyclonalAntigenPurifiedCount = polyclonalAntigenPurifiedCount;
    }

    public Integer getRecombinantCount() {
        return recombinantCount;
    }

    public void setRecombinantCount(Integer recombinantCount) {
        this.recombinantCount = recombinantCount;
    }

    public Integer getOtherCount() {
        return otherCount;
    }

    public void setOtherCount(Integer otherCount) {
        this.otherCount = otherCount;
    }

    public String getRefSeq() {
        return refSeq;
    }

    public void setRefSeq(String refSeq) {
        this.refSeq = refSeq;
    }

    public Long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Long startPosition) {
        this.startPosition = startPosition;
    }

    public Long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Long endPosition) {
        this.endPosition = endPosition;
    }

    public Boolean getForward() {
        return forward;
    }

    public void setForward(Boolean forward) {
        this.forward = forward;
    }

    public String toString() {
        return symbol + "\t" + name + "\t" + UniprotID + "\t" + UniprotFunction + "\t" + family + "\t"
                + familyDetails + "\t" + PDB_IDs + "\t" + pubmedScore + "\t" + antibodyCount;
    }
}
