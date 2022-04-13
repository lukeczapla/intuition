package org.magicat.intuition.service;

import org.magicat.intuition.model.xml.UpdateConfig;
import org.magicat.intuition.model.xml.UpdateItems;

public interface ArticleIndexService {
    void indexAllArticles();
    void reindexAllArticles();

    @Deprecated
    void indexNewArticles(UpdateConfig uc);
    @Deprecated
    void indexNewItems(UpdateItems ui);
}
