package org.magicat.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.magicat.model.Article;

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
