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
@Document(collection = "CancerMaps")
public class CancerMap {

    @Id
    private String cancerType;

    private Integer[] pmIds;

    private String synonyms = null;

    public String getCancerType() {
        return cancerType;
    }

    public void setCancerType(String cancerType) {
        this.cancerType = cancerType;
    }

    public Integer[] getPmIds() {
        return pmIds;
    }

    public void setPmIds(Integer[] pmIds) {
        this.pmIds = pmIds;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public Set<Integer> getListAsSet() {
        if (getPmIds() == null) return null;
        return new HashSet<>(Arrays.asList(getPmIds()));
    }

    public void setListAsSet(Set<Integer> set) {
        pmIds = set.toArray(pmIds);
    }

    @Override
    public String toString() {
        return "CancerMap{" +
                "symbol='" + cancerType + '\'' +
                ", pmIds=" + Arrays.toString(pmIds) +
                '}';
    }

}
