package org.magicat.service;

import org.magicat.model.xml.UpdateConfig;
import org.magicat.model.xml.UpdateItems;

public interface ArticleIndexService {
    void indexAllArticles();
    void reindexAllArticles();

    @Deprecated
    void indexNewArticles(UpdateConfig uc);
    @Deprecated
    void indexNewItems(UpdateItems ui);
}
