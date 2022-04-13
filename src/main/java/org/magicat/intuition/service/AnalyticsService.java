package org.magicat.intuition.service;

import io.swagger.annotations.Api;
import org.magicat.intuition.model.ProjectList;
import org.magicat.intuition.model.Variant;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This Service provides a lot of the functionality for the processing of spreadsheets and other data for
 * Variants in the lists of Variants by keys
 */
@Api("Tools for processing spreadsheets and filling in data")
public interface AnalyticsService {

    byte[] processSpreadsheet(String filename);
    byte[] processVariants(List<Variant> variants);

    void assignTiers(Variant v, Map<String, Integer> pageCounts, List<String> geneSynonyms);
    void assignTiers(Variant v);
    String processCodes(String code);

    void scoreKeywords(Variant variant, List<ProjectList> pl, boolean useSupporting, boolean useCount);
    void scoreKeywords(List<Variant> variants, List<ProjectList> pl, boolean useSupporting, boolean useCount);

    Set<String> missingFullTextArticles(String filename);

    String getKey();
    void setKey(String key);

    void setThread(String id);
    void setThreadStop(String id);

}
