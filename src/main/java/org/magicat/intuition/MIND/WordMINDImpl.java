package org.mskcc.knowledge.MIND;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mskcc.knowledge.service.SolrService;
import org.mskcc.knowledge.util.SolrClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Service
public class WordMINDImpl implements WordMIND, Serializable {

    @Serial
    private static final long serialVersionUID = 1234567891L;
    private static final Logger log = LoggerFactory.getLogger(WordMIND.class);

    transient private final SolrService solrService;

    private final Map<Long, String> articleTexts = new HashMap<>();

    @Autowired
    public WordMINDImpl(SolrService solrService) {
        this.solrService = solrService;
    }

    public WordMIND load() {
        SolrDocumentList documents;
        try {
            solrService.getSolrClientTool().setDefaultField("text");
            if (System.getenv("MIND") != null) {
                documents = solrService.getSolrClientTool().deepPage("*", 10000);
            }
            else {
                log.info("Too big for non-HPC job, set the MIND environment variable if on HPC");
                documents = solrService.getSolrClientTool().find("knowledge", "*", "id,pmid,pmid_supporting,text", 10000, null);
            }
            for (SolrDocument doc : documents) {

            }

        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
        }
        //SolrDocumentList documentList = solrService.searchSolr(9000, "*", null, false);
        return this;
    }

    @Override
    public Map<Long, String> getArticleTexts() {
        return articleTexts;
    }
}
