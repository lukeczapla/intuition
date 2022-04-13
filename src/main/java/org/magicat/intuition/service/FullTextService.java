package org.mskcc.knowledge.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.mskcc.knowledge.model.Article;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public interface FullTextService {

    boolean addArticle(String pmId);
    boolean addArticle(Article article);
    void addSupplementary();
    boolean addSupplementary(String pmId);

    void addHTMLTextAll();

    byte[] getMainPDF(String pmId);
    byte[] getSupportingPDF(String pmIdS);

    void addAllArticles() throws IOException, SolrServerException;

}
