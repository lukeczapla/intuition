package org.magicat.service;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.magicat.model.Article;
import org.magicat.model.FullText;
import org.magicat.model.xml.UpdateConfig;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.FullTextRepository;
import org.magicat.util.SolrClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.magicat.util.SolrClientTool.escape;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SolrServiceImpl implements SolrService {

    private static final Logger log = LoggerFactory.getLogger(SolrService.class);

    private final SolrClientTool solrClientTool;
    private final ArticleRepository articleRepository;
    private final FullTextRepository fullTextRepository;

    @Autowired
    public SolrServiceImpl(SolrClientTool solrClientTool, ArticleRepository articleRepository, FullTextRepository fullTextRepository) {
        this.solrClientTool = solrClientTool;
        this.solrClientTool.setCollection("knowledge");
        this.articleRepository = articleRepository;
        this.fullTextRepository = fullTextRepository;
    }

    @AllArgsConstructor
    static class Triple {
        private Map<String, List<String>> articleMap;
        private Map<String, List<String>> extraMap;
        private String newId;
    }

    @Override
    public void updateSolrArticles() {
        UpdateConfig updateConfig = readConfig();
        if (updateConfig != null) updateSolrArticles(updateConfig.getPage(), updateConfig.getPageSize());
    }

    /**
     * updateSolrArticles takes every single article, deletes existing records, and repopulates Solr with the current
     * text data and author / date fields, etc.
     */
    @Override
    public void updateSolrArticles(int pageNumber, int pageSize) {
        log.info("Running Solr Hashcode update with MongoDB solrId updates");
        Page<Article> page;
        int i = 0;
        List<Map<String, List<String>>> articleMaps = new ArrayList<>();
        List<Map<String, List<String>>> extraMaps = new ArrayList<>();
        List<Triple> values = Collections.synchronizedList(new ArrayList<>());
        List<String> newIds = new ArrayList<>();
        List<String> deletedIds = Collections.synchronizedList(new ArrayList<>());
        do {
            log.info("Iteration " + i + " on page " + (pageNumber+i) + " of page size " + pageSize);
            page = articleRepository.findAll(PageRequest.of(pageNumber+i, pageSize));
            List<Article> articles = page.getContent();
            log.info(articles.size() + " items");
            for (List<Article> group : Lists.partition(articles, 20000)) {
                log.info("Grouped into {} size", group.size());
                group.parallelStream().forEach(a -> {
                    SolrDocumentList sdl = null;
                    try {
                        sdl = solrClientTool.find("knowledge", "pmid:"+a.getPmId() + " AND -pmid_supporting:*");
                    } catch (SolrServerException | IOException e) {
                        log.error("solrClientTool.find: {}", e.getMessage());
                        e.printStackTrace();
                    }
                    if (sdl != null && sdl.size() > 0) {
                        if (sdl.size() == 1) return;
                        for (SolrDocument doc : sdl) {
                            deletedIds.add((String)doc.get("id"));
                            //solrClientTool.delete("knowledge", (String)doc.get("id"), true);
                        }
                    }
                    Map<String, List<String>> articleMap = new HashMap<>();
                    Map<String, List<String>> extraMap = new HashMap<>();
                    /*if (a.getHasFullText() == null || !a.getHasFullText()) {
                        FullText ft = fullTextRepository.findFullTextFor(a.getPmId());
                        if (ft != null) {
                            a.setHasFullText(true);
                            a.setFulltext(ft.getTextEntry());
                            articleRepository.save(a);
                        }
                        if (a.getHasFullText() == null) {
                            a.setHasFullText(false);
                            articleRepository.save(a);
                        }
                    }*/
                    String text = Article.toText(a);
                    articleMap.put("text", Collections.singletonList(text));
                    articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                    if (a.getPmId() == null) return;
                    articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                    a.setSolrId(Integer.toHexString(articleMap.hashCode()) + (a.getPmId().length() > 3 ? a.getPmId().substring(a.getPmId().length()-3) : a.getPmId()));
                    if (a.getPublicationDate() != null)
                        extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                    else if (a.getPubDate() != null)
                        extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                    if (a.getFulltext() != null && a.getFulltext().length() > 0)
                        extraMap.put("hasFullText", Collections.singletonList("true"));
                    else
                        extraMap.put("hasFullText", Collections.singletonList("false"));
                    values.add(new Triple(articleMap, extraMap, a.getSolrId()));

                });
                log.info("Sending batch to Solr addUpdateDeleteMany() with size {}", values.size());
                try {
                    if (values.size() > 0) {
                        values.forEach(t -> {
                            articleMaps.add(t.articleMap);
                            extraMaps.add(t.extraMap);
                            newIds.add(t.newId);
                        });
                        values.clear();
                        solrClientTool.addUpdateDeleteMany("knowledge", articleMaps, extraMaps, newIds, deletedIds);
                    } else if (deletedIds.size() > 0) {
                        log.info("Nothing to add but {} items to delete", deletedIds.size());
                        solrClientTool.deleteMany("knowledge", deletedIds);
                    }
                } catch (SolrServerException | IOException e) {
                    log.error("SolrClientTool.addUpdateDeleteMany/deleteMany: {}", e.getMessage());
                    e.printStackTrace();
                }
                //articleRepository.saveAll(articleList);
                newIds.clear();
                deletedIds.clear();
                articleMaps.clear();
                extraMaps.clear();
                //articleRepository.saveAll(group);
            }
            //solrClientTool.refreshCollection("knowledge");
            i++;
        } while (page.hasNext());
    }


    @Override
    public void updateSolrFullTextArticles() {
        log.info("Updating full-text on Solr instance");

        int pageNumber = 0;
        int pageSize = 5000;
        Page<FullText> fullTextList;
        do {
            fullTextList = fullTextRepository.findAll(PageRequest.of(pageNumber++, pageSize));
            List<FullText> fullTexts = fullTextList.getContent();
            log.info("{} main full-text items to look at in this iteration", fullTexts.size());

            List<String> pmIds = fullTexts.stream().map(FullText::getPmId).collect(Collectors.toList());
            List<Article> articles = new LinkedList<>();
            for (List<String> partition : Lists.partition(pmIds, 1000)) {
                articles.addAll(articleRepository.findAllByPmIdIn(partition));

                log.info(articles.size() + " items to set");
                articles.parallelStream().forEach(a -> {
                    fullTexts.stream().filter(f -> f.getPmId().equals(a.getPmId())).findFirst().ifPresent(f -> {
                        a.setFulltext(f.getTextEntry());
                        a.setHasFullText(true);
                    });
                    Map<String, List<String>> articleMap = new HashMap<>();
                    Map<String, List<String>> extraMap = new HashMap<>();
                    String text = Article.toText(a);
                    articleMap.put("text", Collections.singletonList(text));
                    articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                    articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                    a.setSolrId(Integer.toHexString(articleMap.hashCode()) + (a.getPmId().length() > 3 ? a.getPmId().substring(a.getPmId().length()-3) : a.getPmId()));
                    if (a.getPublicationDate() != null)
                        extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                    else if (a.getPubDate() != null)
                        extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                    extraMap.put("hasFullText", Collections.singletonList("true"));
                    try {
                        SolrDocumentList sdl = solrClientTool.find("knowledge", "pmid:\""+a.getPmId() + "\" AND -pmid_supporting:*");
                        if (sdl.size() > 0) {
                            for (SolrDocument doc : sdl) {
                                solrClientTool.delete("knowledge", (String)doc.get("id"));
                            }
                        }
                        solrClientTool.add("knowledge", articleMap, extraMap);
                    } catch (SolrServerException | IOException e) {
                        e.printStackTrace();
                    }
                    articleRepository.save(a);
                });
                articles.clear();
            }
            pmIds.clear();
            fullTexts.clear();
        } while (fullTextList.hasNext());
        log.info("Step 1 finished. Adding supporting information...");
        updateSolrSupportingInformation(0, 3000);
        solrClientTool.refreshCollection("knowledge");
        log.info("Done updating Solr collection");
    }

    @Override
    public void updateSolrSupportingInformation(int pageNumber, int pageSize) {
        Page<FullText> fullTextList;
        List<Map<String, List<String>>> articleMaps = new ArrayList<>();
        List<Map<String, List<String>>> extraMaps = new ArrayList<>();
        List<Triple> values = Collections.synchronizedList(new ArrayList<>());
        List<String> newIds = new ArrayList<>();
        List<String> deletedIds = Collections.synchronizedList(new ArrayList<>());
        do {
            fullTextList = fullTextRepository.findAllSupplementary(PageRequest.of(pageNumber++, pageSize));//fullTextRepository.findAll().stream().filter(f -> f.getPmId().contains("S")).sorted(Comparator.comparing(FullText::getPmId)).collect(Collectors.toList());
            List<FullText> fullTextsS = fullTextList.getContent();
            log.info("{} supporting text (PDF/DOC/DOCX) items to look at", fullTextsS.size());
            fullTextsS.parallelStream().forEach(ft -> {
                Article a = articleRepository.findByPmId(ft.getPmId().substring(0, ft.getPmId().indexOf("S")));
                boolean update = false;
                if (a.getSupportingText() != null && (a.getSupportingText().endsWith(ft.getPmId()) || a.getSupportingText().contains(ft.getPmId() + ","))) {
                    //log.info("{} already contained in article {} supportingText field", ft.getPmId(), a.getPmId());
                } else if (a.getSupportingText() != null && a.getSupportingText().length() > 0) {
                    if (a.getSupportingText().endsWith(",")) a.setSupportingText(a.getSupportingText()+ft.getPmId());
                    else a.setSupportingText(a.getSupportingText() + "," + ft.getPmId());
                    update = true;
                } else {
                    a.setSupportingText(ft.getPmId());
                    update = true;
                }
                if (update) articleRepository.save(a);
                Map<String, List<String>> articleMap = new HashMap<>();
                Map<String, List<String>> extraMap = new HashMap<>();
                articleMap.put("text", Collections.singletonList(ft.getTextEntry()));
                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                articleMap.put("pmid_supporting", Collections.singletonList(ft.getPmId()));
                if (a.getPublicationDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                else if (a.getPubDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                extraMap.put("hasFullText", Collections.singletonList("true"));
                String newId = Integer.toHexString(articleMap.hashCode()) + (ft.getPmId().length() > 3 ? ft.getPmId().substring(ft.getPmId().length()-3) : ft.getPmId());

                try {
                    SolrDocumentList sdl = solrClientTool.find("knowledge", "pmid_supporting:"+ft.getPmId());
                    if (sdl.size() > 0) {
                        for (SolrDocument doc: sdl) {
                            deletedIds.add((String)doc.get("id"));
                        }
                    }
                    values.add(new Triple(articleMap, extraMap, newId));
                } catch (IOException | SolrServerException e) {
                    log.error(e.getMessage());
                }
            });
            log.info("Sending batch to Solr addUpdateDeleteMany() with size {}", values.size());
            try {
                if (values.size() > 0) {
                    values.forEach(t -> {
                        articleMaps.add(t.articleMap);
                        extraMaps.add(t.extraMap);
                        newIds.add(t.newId);
                    });
                    values.clear();
                    solrClientTool.addUpdateDeleteMany("knowledge", articleMaps, extraMaps, newIds, deletedIds);
                } else if (deletedIds.size() > 0) {
                    log.info("Nothing to add but {} items to delete", deletedIds.size());
                    solrClientTool.deleteMany("knowledge", deletedIds);
                }
            } catch (SolrServerException | IOException e) {
                log.error("SolrClientTool.addUpdateDeleteMany/deleteMany: {}", e.getMessage());
                e.printStackTrace();
            }
            newIds.clear();
            deletedIds.clear();
            articleMaps.clear();
            extraMaps.clear();

        } while (fullTextList.hasNext());
    }

    public void updateSolrSupportingInformation() {
        updateSolrSupportingInformation(0, 3000);
    }

    @Override
    public void addArticle(String pmid) {
        Article a = articleRepository.findByPmId(pmid);
        if (a == null) {
            log.error("Cannot add to Solr collection: No article with PMID {} exists", pmid);
            return;
        }
        Map<String, List<String>> articleMap = new HashMap<>();
        Map<String, List<String>> extraMap = new HashMap<>();
        String text = Article.toText(a);
        articleMap.put("text", Collections.singletonList(text));
        //articleMap.put("text_ws", Collections.singletonList(text));
        articleMap.put("authors", Collections.singletonList(a.getAuthors()));
        articleMap.put("pmid", Collections.singletonList(a.getPmId()));
        a.setSolrId(Integer.toHexString(articleMap.hashCode()) + (a.getPmId().length() > 3 ? a.getPmId().substring(a.getPmId().length()-3) : a.getPmId()));
        if (a.getPublicationDate() != null)
            extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
        else if (a.getPubDate() != null)
            extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
        if (a.getFulltext() == null) extraMap.put("hasFullText", Collections.singletonList("false"));
        else extraMap.put("hasFullText", Collections.singletonList("true"));
        try {
            SolrDocumentList sdl = solrClientTool.find("knowledge", "pmid:"+a.getPmId());
            if (sdl.size() > 0) {
                for (SolrDocument doc: sdl) {
                    solrClientTool.delete("knowledge", (String)doc.get("id"), true);
                }
            }
            solrClientTool.add("knowledge", articleMap, extraMap);
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }

    private String buildSearchExpression(List<String> terms, List<String> genes, List<List<String>> geneSynonyms, List<String> mutations, List<List<String>> mutationSynonyms,
                                         List<String> drugs, List<List<String>> drugSynonyms, List<String> cancers, List<List<String>> cancerSynonyms, List<String> keywords,
                                         String authors, DateTime after) {
        StringBuilder sb = new StringBuilder(256);
        int count = 0;
        if (genes != null) for (int i = 0; i < genes.size(); i++) {
            if (count > 0) sb.append(" AND ");
            sb.append("+("); //sb.append("(text:");
            sb.append(quote(escape(genes.get(i))));
            terms.add(genes.get(i));
            if (geneSynonyms != null && geneSynonyms.size() > i && geneSynonyms.get(i) != null && geneSynonyms.get(i).size() > 0) {
                for (int j = 0; j < geneSynonyms.get(i).size(); j++) {
                    sb.append(" OR "); //sb.append(" OR text:");
                    sb.append(quote(escape(geneSynonyms.get(i).get(j))));
                }
            }
            count++;
            sb.append(")");
        }
        if (mutations != null) for (int i = 0; i < mutations.size(); i++) {
            if (count > 0) sb.append(" AND ");
            sb.append("+("); //sb.append("(text:");
            sb.append(quote(escape(mutations.get(i))));
            terms.add(mutations.get(i));
            if (mutationSynonyms != null && mutationSynonyms.get(i) != null && mutationSynonyms.get(i).size() > 0) {
                for (int j = 0; j < mutationSynonyms.get(i).size(); j++) {
                    sb.append(" OR "); //sb.append(" OR text:");
                    sb.append(quote(escape(mutationSynonyms.get(i).get(j))));
                }
            }
            count++;
            sb.append(")");
        }
        if (drugs != null) for (int i = 0; i < drugs.size(); i++) {
            if (count > 0) sb.append(" AND ");
            sb.append("+("); //sb.append("(text:");
            sb.append(quote(escape(drugs.get(i))));
            terms.add(drugs.get(i));
            if (drugSynonyms != null && drugSynonyms.get(i).size() > 0) {
                for (int j = 0; j < drugSynonyms.get(i).size(); j++) {
                    sb.append(" OR "); //sb.append(" OR text:");
                    sb.append(quote(escape(drugSynonyms.get(i).get(j))));
                }
            }
            count++;
            sb.append(")");
        }
        if (cancers != null) for (int i = 0; i < cancers.size(); i++) {
            if (count > 0) sb.append(" AND ");
            sb.append("+("); //sb.append("(text:");
            sb.append(quote(escape(cancers.get(i))));
            terms.add(cancers.get(i));
            if (cancerSynonyms != null && cancerSynonyms.get(i).size() > 0) {
                for (int j = 0; j < cancerSynonyms.get(i).size(); j++) {
                    sb.append(" OR "); //sb.append(" OR text:");
                    sb.append(quote(escape(cancerSynonyms.get(i).get(j))));
                }
            }
            count++;
            sb.append(")");
        }
        if (keywords != null) for (String keyword : keywords) {
            if (count > 0) sb.append(" AND ");
            //sb.append("(");
            if (keyword.contains("\"")) sb.append(keyword);
            else sb.append(quote(keyword));
            terms.add(0, keyword);
            count++;
            //sb.append(")");
        }
        if (after != null) {
            sb.append(" AND date:[").append(after.toDateTime(DateTimeZone.UTC).toString()).append(" TO NOW]");
        }
        if (authors != null && authors.length() > 0) {
            sb.append(" AND authors:").append(quote(escape(authors)));
        }
        return sb.toString();
    }


    @SuppressWarnings("unchecked")
    @Override
    public SearchResult searchSolr(int limit, List<String> genes, List<List<String>> geneSynonyms, List<String> mutations, List<List<String>> mutationSynonyms,
                                       List<String> drugs, List<List<String>> drugSynonyms, List<String> cancers, List<List<String>> cancerSynonyms, List<String> keywords,
                                       String authors, DateTime after) {

        List<String> terms = new ArrayList<>();
        String query = buildSearchExpression(terms, genes, geneSynonyms, mutations, mutationSynonyms, drugs, drugSynonyms, cancers, cancerSynonyms, keywords, authors, after);
        log.info(query);
        String freqTerm = null;
        if (terms.size() == 1) freqTerm = escape(terms.get(0));
        else if (keywords != null && keywords.size() == 1) freqTerm = escape(terms.get(0));
        else {
            if (mutations != null && mutations.size() > 0) freqTerm = escape(mutations.get(0));
            else freqTerm = escape(terms.get(0));
        }
        try {
            SolrDocumentList result;
            if (query.contains("*")) solrClientTool.setDefaultField("text_ws");
            else solrClientTool.setDefaultField("text");
            result = solrClientTool.find("knowledge", query, "pmid,pmid_supporting,date,score", limit, freqTerm);
            log.info(result.toString());
            List<String> pmIds = new ArrayList<>(result.size());
            List<Float> scores = new ArrayList<>(result.size());
            for (SolrDocument item : result) {
                List<Long> values = (ArrayList<Long>) (item.getFieldValue("pmid"));
                if (item.getFieldValue("pmid_supporting") != null) {
                    List<String> supportingPMID = (ArrayList<String>)(item.getFieldValue("pmid_supporting"));
                    pmIds.add(supportingPMID.get(0));
                } else pmIds.add(values.get(0) + "");
                scores.add((Float)(item.getFieldValue("score")));
            }
            return new SearchResult(result, pmIds, scores);
        } catch (IOException|SolrServerException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public SearchResult searchSolr(int limit, String searchTerm, DateTime after, boolean rank) {
        try {
            SolrDocumentList result;
            solrClientTool.setDefaultField("text");
            if (!searchTerm.contains(":")) result = solrClientTool.find("knowledge", searchTerm, "pmid,pmid_supporting", limit, rank ? searchTerm : null);
            else result = solrClientTool.find("knowledge", searchTerm, rank ? "pmid,pmid_supporting" : "pmid,pmid_supporting,text", limit, searchTerm);
            log.info(result.toString());
            List<String> pmIds = new ArrayList<>(result.size());
            List<Float> scores = new ArrayList<>(result.size());
            for (SolrDocument item : result) {
                //log.info(item.getFieldValue("pmid").getClass().getSimpleName());
                List<Long> values = (ArrayList<Long>) (item.getFieldValue("pmid"));
                if (item.getFieldValue("pmid_supporting") != null) {
                    List<String> supportingPMID = (ArrayList<String>)(item.getFieldValue("pmid_supporting"));
                    pmIds.add(supportingPMID.get(0));
                } else pmIds.add(values.get(0) + "");
                scores.add((Float)(item.getFieldValue("score")));
            }
            return new SearchResult(result, pmIds, scores);
        } catch (IOException|SolrServerException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public SolrDocument findArticle(String pmId) {
        boolean supplementary = pmId.contains("S");
        try {
            SolrDocumentList sdl = solrClientTool.find("knowledge", (supplementary ? "pmid_supporting:" : "-pmid_supporting:* AND pmid:") + '"' + pmId + '"');
            if (sdl.size() == 1) {
                return sdl.get(0);
            }
            if (sdl.size() == 0) {
                log.error("No SolrDocument found for PMID {}, trying to create", pmId);
                addArticle(pmId);
                solrClientTool.refreshCollection("knowledge");
                sdl = solrClientTool.find("knowledge", (supplementary ? "pmid_supporting:" : "-pmid_supporting:* AND pmid:") + '"' + pmId + '"');
                if (sdl.size() == 1) return sdl.get(0);
                log.error("Still could not find the record in collection");
                return null;
            } else {  // one or more duplicates! delete one.
                Long max = 0L;
                SolrDocument maxDoc = sdl.get(0);
                for (int i = 0; i < sdl.size(); i++) {
                    SolrDocument doc = sdl.get(i);
                    Long d = (Long)doc.get("_version_");
                    if (d > max) {
                        max = d;
                        maxDoc = doc;
                    }
                }
                for (int i = 0; i < sdl.size(); i++) {
                    SolrDocument doc = sdl.get(i);
                    Long d = (Long)doc.get("_version_");
                    if (d < max) {
                        log.info("Duplicate Solr document: Deleting result {}", i);
                        solrClientTool.delete("knowledge", (String)doc.get("id"), true);
                    }
                }
                return maxDoc;
            }
        } catch (IOException | SolrServerException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteArticle(String pmId) {
        boolean supplementary = pmId.contains("S");
        try {
            SolrDocumentList sdl = solrClientTool.find("knowledge", (supplementary ? "pmid_supporting:" : "-pmid_supporting:* AND pmid:") + '"' + pmId + '"');
            log.info("{} document(s) to delete", sdl.size());
            if (sdl.size() == 0) return false;
            for (SolrDocument doc : sdl) {
                Collection<String> fields = doc.getFieldNames();
                if (!supplementary && fields.contains("pmid_supporting")) continue;
                String id = (String)doc.get("id");
                solrClientTool.delete("knowledge", id);
                //System.out.println(doc);
            }
        } catch (IOException | SolrServerException e) {
            log.error(e.getMessage());
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getText(SolrDocument item) {
        if (item.get("text") instanceof List) return ((List<String>)item.get("text")).get(0);
        else return (String)item.get("text");
    }

    public static String quote(String term) {
        return '"' + term + '"';
    }

    public static String toText(Article article) {
        StringBuilder sb = new StringBuilder(50000);
        sb.append(" {!title} ");
        sb.append(article.getTitle());
        sb.append(" {!keywords} ");
        if (article.getKeywords() != null) {
            String[] kw = article.getKeywords().split(";");
            for (int i = 0; i < kw.length; i++) {
                if (i != 0) sb.append(" , ");
                if (kw[i].split(":").length > 1) sb.append(kw[i].split(":")[1]);
            }
        }
        sb.append(" {!meshterms} ");
        if (article.getMeshTerms() != null) {

            String[] mt = article.getMeshTerms().split(";");
            for (int i = 0; i < mt.length; i++) {
                if (i != 0) sb.append(" , ");
                if (mt[i].split(":").length > 2) sb.append(mt[i].split(":")[2]);
            }
        }
        sb.append(" {!abstract} ");
        if (article.getPubAbstract() != null) sb.append(article.getPubAbstract());
        sb.append(" {!fulltext} ");
        //if (article.getFulltext() != null) sb.append(article.getFulltext());
        return sb.toString();
    }

    private UpdateConfig readConfig() {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            UpdateConfig uc = (UpdateConfig)jaxbUnmarshaller.unmarshal(new File("UpdateConfig.xml"));
            return uc;
        }
        catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SolrClientTool getSolrClientTool() {
        return solrClientTool;
    }


}
