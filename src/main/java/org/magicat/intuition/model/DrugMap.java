package org.mskcc.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "DrugMaps")
public class DrugMap {

    public DrugMap(String drug, Integer[] pmIds) {
        this.drug = drug;
        this.pmIds = pmIds;
    }

    @Id
    private String drug;

    private Integer[] pmIds;

    private String synonyms;

    private Boolean cancerDrug;

    public String getDrug() {
        return drug;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public Integer[] getPmIds() {
        return pmIds;
    }

    public void setPmIds(Integer[] pmIds) {
        this.pmIds = pmIds;
    }

    public Set<Integer> getListAsSet() {
        if (pmIds == null) return null;
        return new HashSet<>(Arrays.asList(getPmIds()));
    }

    public void setListAsSet(Set<Integer> set) {
        pmIds = set.toArray(pmIds);
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public Boolean getCancerDrug() {
        return cancerDrug;
    }

    public void setCancerDrug(Boolean cancerDrug) {
        this.cancerDrug = cancerDrug;
    }

    @Override
    public String toString() {
        return "DrugMap{" +
                "symbol='" + drug + '\'' +
                ", pmIds=" + Arrays.toString(pmIds) +
                '}';
    }
}
