package org.magicat.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "GeneMaps")
public class GeneMap {

    public GeneMap(String symbol, Integer[] pmIds) {
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

    public GeneMap dup() {
        GeneMap result = new GeneMap(this.symbol, null);
        result.pmIds = new Integer[this.pmIds.length];
        for (int i = 0; i < this.pmIds.length; i++) result.pmIds[i] = this.pmIds[i];
        return result;
    }

    @Override
    public String toString() {
        return "GeneMap{" +
                "symbol='" + symbol + '\'' +
                ", pmIds=" + Arrays.toString(pmIds) +
                ", synonyms=" + synonyms + '}';
    }
}
