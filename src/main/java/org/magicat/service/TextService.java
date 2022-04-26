package org.magicat.service;

import org.magicat.model.ProjectList;

import java.util.List;
import java.util.Map;

public interface TextService {

    Map<String, Integer> getSentences(String PMID, String mutation);

    void setupPageNumbers(String PMID, String mutation);
    List<String> getParagraphs(String PMID, String mutation);
    boolean validateText(String text, String gene, List<String> geneSynonyms, String mutation, int count);

    List<String> nameFinder(String text);

    List<Integer> getPageNumbers();
    Map<Integer, Integer> getPageCounts();
    int getReferencesPosition(String text, boolean html);
    int getReferencesPosition(String text);

    int findPageNumber(String HTMLEntry, String sentence);

    Map<String, Integer> findKeywordSentences(String mutation, List<ProjectList> keywords, String pmId, boolean includeSupporting);
    Map<String, Integer> findKeywordOnlySentences(String mutation, List<ProjectList> keywords, String pmId, boolean includeSupporting);


}
