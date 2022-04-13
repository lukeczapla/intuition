package org.magicat.intuition.service;

import org.magicat.intuition.model.Article;
import org.magicat.intuition.util.ProcessUtil;
import org.magicat.intuition.util.Tree;
import org.magicat.intuition.util.XMLParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.List;

public interface ArticleService {

    void updateArticlesPubmed();
    void updateArticlesPubmedRecent();

    boolean addArticlePDF(String pmId);

    boolean addArticlePMID(String pmId);

    void updateCitations(int pageNumber, int pageSize);
    void updateCitations();

    void addCitation(Article article, List<Article> articleList);

    void addCitationQueue(Article article, List<Article> articleList, int size);

    void updateMaps(String[] items);

    void runSearch(String term, int size, int count);
    void runSearches(List<String> terms, int size);

    boolean indexArticles();

    void addCitationRecords(int pageNumber, int pageSize);

    //String runSearchPython(String term, int size);

    //void runSearchComplete(String XMLFile);

    //void runSearchCompleteParallel(String XMLFile);

}
