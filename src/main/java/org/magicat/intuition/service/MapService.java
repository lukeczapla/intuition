package org.mskcc.knowledge.service;

import org.mskcc.knowledge.model.xml.UpdateConfig;
import org.mskcc.knowledge.model.xml.UpdateItems;

import java.util.List;

public interface MapService {

    int updateMapsAll(int count);
    void addOneTerm(String name, List<String> synonyms, CalculationType calculationType);
    void addManyTerms(UpdateItems updateItems);
    void addManyTerms(UpdateItems updateItems, int pageNumber);
    void updateMaps(UpdateConfig settings);
    void indexArticles();

    enum CalculationType {
        GENE, MUTATION, DRUG, CANCER
    }

}
