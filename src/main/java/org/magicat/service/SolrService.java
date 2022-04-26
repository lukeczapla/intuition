package org.magicat.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.joda.time.DateTime;
import org.magicat.util.SolrClientTool;

import java.util.List;

public interface SolrService {

    void updateSolrArticles(int pageNumber, int pageSize);
    void updateSolrArticles();
    void updateSolrFullTextArticles();
    void updateSolrSupportingInformation(int pageNumber, int pageSize);
    void updateSolrSupportingInformation();
    SolrClientTool getSolrClientTool();

    void addArticle(String pmid);

    String getText(SolrDocument item);
    SolrDocument findArticle(String pmId);
    boolean deleteArticle(String pmId);

    @Getter
    @Setter
    @AllArgsConstructor
    class SearchResult {
        SolrDocumentList docs;
        List<String> pmIds;
        List<Float> scores;
    }

    SearchResult searchSolr(int limit, String searchTerm, DateTime after, boolean rank);
    SearchResult searchSolr(int limit, List<String> genes, List<List<String>> geneSynonyms, List<String> mutations, List<List<String>> mutationSynonyms,
                                List<String> drugs, List<List<String>> drugSynonyms, List<String> cancers, List<List<String>> cancerSynonyms, List<String> keywords,
                                String authors, DateTime after);

}
