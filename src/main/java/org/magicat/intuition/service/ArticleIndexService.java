package org.mskcc.knowledge.service;

import org.mskcc.knowledge.model.xml.UpdateConfig;
import org.mskcc.knowledge.model.xml.UpdateItems;

public interface ArticleIndexService {
    void indexAllArticles();
    void reindexAllArticles();

    @Deprecated
    void indexNewArticles(UpdateConfig uc);
    @Deprecated
    void indexNewItems(UpdateItems ui);
}
