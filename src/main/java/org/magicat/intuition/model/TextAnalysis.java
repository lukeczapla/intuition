package org.mskcc.knowledge.model;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TextAnalysis {

    private String pmId;
    private String term;
    private String variant; // the descriptor of the Variant

    private Map<String, Integer> sentenceIslands;
    private List<String> keywordSentences;
    private List<Integer> keywordPageNumbers;
    private List<Integer> pageNumbers;
    private List<String> paragraphs;
    private String[][] islands;

    public String getPmId() {
        return pmId;
    }

    public void setPmId(String pmId) {
        this.pmId = pmId;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Map<String, Integer> getSentenceIslands() {
        return sentenceIslands;
    }

    public void setSentenceIslands(Map<String, Integer> sentenceIslands) {
        this.sentenceIslands = sentenceIslands;
        if (sentenceIslands == null) return;
        islands = new String[sentenceIslands.keySet().size()][2];
        int i = 0;
        for (String s: sentenceIslands.keySet()) {
            this.sentenceIslands.put(s, sentenceIslands.get(s));
            islands[i][0] = s;
            islands[i][1] = sentenceIslands.get(s) + "";
            i++;
        }
    }

    public List<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(List<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public List<String> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<String> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public String[][] getIslands() {
        return islands;
    }

    public List<String> getKeywordSentences() {
        return keywordSentences;
    }

    public void setKeywordSentences(List<String> keywordSentences) {
        this.keywordSentences = keywordSentences;
    }

    public List<Integer> getKeywordPageNumbers() {
        return keywordPageNumbers;
    }

    public void setKeywordPageNumbers(List<Integer> keywordPageNumbers) {
        this.keywordPageNumbers = keywordPageNumbers;
    }
}
