package org.mskcc.knowledge.model;

import io.swagger.annotations.Api;
import lombok.*;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Api("Variant objects have the gene, alterations (maybe also drugs, cancer types) and automation results for different provided items")
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Variants")
public class Variant {

    @Id
    private ObjectId id;

    private String gene;
    private String mutation;

    private String key;
    @Indexed(unique = true)
    private String descriptor;

    private String drugs;
    private String cancerTypes;

    private String oncogenicity;
    private String mutationEffect;

    private String curatedPMIds;
    private String automatedPMIds;
    private String consensusPMIds;
    private String articleURLs;
    private String articlePages;

    private List<String> articlesTier1;
    private List<String> articlesTier2;

    private List<Integer> scores1;
    private List<Integer> scores2;

    private List<String> scoreCode1;
    private List<String> scoreCode2;

    private List<Integer> keywordScores;

    private String excludedPMIds;
    private String notes;

    private Integer total;

    private List<NewScore> newScores;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private DateTime lastUpdate;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public String getMutation() {
        return mutation;
    }

    public void setMutation(String mutation) {
        this.mutation = mutation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getOncogenicity() {
        return oncogenicity;
    }

    public void setOncogenicity(String oncogenicity) {
        this.oncogenicity = oncogenicity;
    }

    public String getMutationEffect() {
        return mutationEffect;
    }

    public void setMutationEffect(String mutationEffect) {
        this.mutationEffect = mutationEffect;
    }

    public String getCuratedPMIds() {
        return curatedPMIds;
    }

    public void setCuratedPMIds(String curatedPMIds) {
        this.curatedPMIds = curatedPMIds;
    }

    public String getAutomatedPMIds() {
        return automatedPMIds;
    }

    public void setAutomatedPMIds(String automatedPMIds) {
        this.automatedPMIds = automatedPMIds;
    }

    public String getConsensusPMIds() {
        return consensusPMIds;
    }

    public void setConsensusPMIds(String consensusPMIds) {
        this.consensusPMIds = consensusPMIds;
    }

    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getDrugs() {
        return drugs;
    }

    public void setDrugs(String drugs) {
        this.drugs = drugs;
    }

    public String getCancerTypes() {
        return cancerTypes;
    }

    public void setCancerTypes(String cancerTypes) {
        this.cancerTypes = cancerTypes;
    }

    public String getArticleURLs() {
        return articleURLs;
    }

    public void setArticleURLs(String articleURLs) {
        this.articleURLs = articleURLs;
    }

    public String getArticlePages() {
        return articlePages;
    }

    public void setArticlePages(String articlePages) {
        this.articlePages = articlePages;
    }

    public String getExcludedPMIds() {
        return excludedPMIds;
    }

    public void setExcludedPMIds(String excludedPMIds) {
        this.excludedPMIds = excludedPMIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getArticlesTier1() {
        return articlesTier1;
    }

    public void setArticlesTier1(List<String> articlesTier1) {
        this.articlesTier1 = articlesTier1;
    }

    public List<String> getArticlesTier2() {
        return articlesTier2;
    }

    public void setArticlesTier2(List<String> articlesTier2) {
        this.articlesTier2 = articlesTier2;
    }

    public List<Integer> getScores1() {
        return scores1;
    }

    public void setScores1(List<Integer> scores1) {
        this.scores1 = scores1;
    }

    public List<Integer> getScores2() {
        return scores2;
    }

    public void setScores2(List<Integer> scores2) {
        this.scores2 = scores2;
    }

    public List<String> getScoreCode1() {
        return scoreCode1;
    }

    public void setScoreCode1(List<String> scoreCode1) {
        this.scoreCode1 = scoreCode1;
    }

    public List<String> getScoreCode2() {
        return scoreCode2;
    }

    public void setScoreCode2(List<String> scoreCode2) {
        this.scoreCode2 = scoreCode2;
    }

    public List<Integer> getKeywordScores() {
        return keywordScores;
    }

    public void setKeywordScores(List<Integer> keywordScores) {
        this.keywordScores = keywordScores;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<NewScore> getNewScores() {
        return newScores;
    }

    public void setNewScores(List<NewScore> newScores) {
        this.newScores = newScores;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class NewScore implements Comparable<NewScore> {
        private Integer total;
        private Integer altScore;
        private Integer fScore;

        private String pmId;

        public int compareTo(NewScore other) {
            return this.total-other.total;
        }
    }

}