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
@Document(collection = "MutationMaps")
public class MutationMap {

    public MutationMap(String symbol, Integer[] pmIds) {
        this.symbol = symbol;
        this.pmIds = pmIds;
    }

    @Id
    private String symbol;

    private Integer[] pmIds;

    private String synonyms;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
        if (pmIds == null) return null;
        return new HashSet<>(Arrays.asList(getPmIds()));
    }

    public void setListAsSet(Set<Integer> set) {
        pmIds = set.toArray(pmIds);
    }

    @Override
    public String toString() {
        return "MutationMap{" +
                "symbol='" + symbol + '\'' +
                ", pmIds=" + Arrays.toString(pmIds) +
                '}';
    }

}
