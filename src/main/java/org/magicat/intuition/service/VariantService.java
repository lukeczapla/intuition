package org.magicat.intuition.service;

import org.magicat.intuition.model.Variant;

import java.util.List;
import java.util.Set;


public interface VariantService {

    Set<String> missingFullTextArticles(List<Variant> variants);

    void rescoreArticlesHongxin(Variant variant);
    void rescoreArticlesHongxin(List<Variant> variants);

    void rescoreArticles(Variant variant);
    void rescoreArticles(List<Variant> variants);

    byte[] createVariantSpreadsheet(List<Variant> variants);

    void getHumanCuratedPDFs(List<Variant> variants);

    void getArticlePDFs(int limit, String searchTerm);

}
