package org.magicat.intuition.service;

import com.google.common.collect.Lists;
import org.magicat.intuition.model.*;
import org.magicat.intuition.model.xml.Item;
import org.magicat.intuition.model.xml.UpdateConfig;
import org.magicat.intuition.model.xml.UpdateItems;
import org.magicat.intuition.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleIndexServiceImpl implements ArticleIndexService {

    private static final Logger log = LoggerFactory.getLogger(ArticleIndexService.class);

    @Autowired
    GeneMapRepository geneMapRepository;
    @Autowired
    MutationMapRepository mutationMapRepository;
    @Autowired
    DrugMapRepository drugMapRepository;
    @Autowired
    CancerMapRepository cancerMapRepository;
    @Autowired
    ArticleIndexRepository articleIndexRepository;
    @Autowired
    ArticleRepository articleRepository;



    @Override
    public void indexAllArticles() {
        log.info("Indexing all of the mapped articles");
        List<CancerMap> cancerMaps = cancerMapRepository.findAll();
        List<MutationMap> mutationMaps = mutationMapRepository.findAll();
        List<GeneMap> geneMaps = geneMapRepository.findAll();
        List<DrugMap> drugMaps = drugMapRepository.findAll();
        List<Article> articleList = new ArrayList<>();
        log.info("Indexing cancer types..");
        cancerMaps.forEach(c -> {
            articleList.clear();
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmIdList = pmids.parallelStream().map(Object::toString).collect(Collectors.toList());
            if (pmIdList.size() < 1800) articleList.addAll(articleRepository.findAllByPmIdIn(pmIdList));
            else for (List<String> partition: Lists.partition(pmIdList, 1600)) articleList.addAll(articleRepository.findAllByPmIdIn(partition));
            final List<Article> toSave = Collections.synchronizedList(new ArrayList<>());
            articleList.parallelStream().forEach(a -> {
                if (a.getTopics() == null || a.getTopics().equals("")) {
                    a.setTopics("cancer:"+c.getCancerType());
                    toSave.add(a);
                }
                else if (!a.getTopics().endsWith("cancer:" + c.getCancerType()) && !a.getTopics().contains("cancer:" + c.getCancerType() + ";")) {
                    a.setTopics(a.getTopics() + ";" + "cancer:" + c.getCancerType());
                    toSave.add(a);
                }
            });
            articleRepository.saveAll(toSave);
        });
        log.info("Indexing drugs..");
        drugMaps.forEach(c -> {
            articleList.clear();
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmIdList = pmids.parallelStream().map(Object::toString).collect(Collectors.toList());
            if (pmIdList.size() < 1800) articleList.addAll(articleRepository.findAllByPmIdIn(pmIdList));
            else for (List<String> partition: Lists.partition(pmIdList, 1600)) articleList.addAll(articleRepository.findAllByPmIdIn(partition));
            final List<Article> toSave = Collections.synchronizedList(new ArrayList<>());
            articleList.parallelStream().forEach(a -> {
                if (a.getTopics() == null || a.getTopics().equals("")) {
                    a.setTopics("drug:"+c.getDrug());
                    toSave.add(a);
                }
                else if (!a.getTopics().endsWith("drug:" + c.getDrug()) && !a.getTopics().contains("drug:" + c.getDrug() + ";")) {
                    a.setTopics(a.getTopics() + ";" + "drug:" + c.getDrug());
                    toSave.add(a);
                }
            });
            articleRepository.saveAll(toSave);
        });
        log.info("Indexing alterations..");
        mutationMaps.forEach(c -> {
            articleList.clear();
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmIdList = pmids.parallelStream().map(Object::toString).collect(Collectors.toList());
            if (pmIdList.size() < 1800) articleList.addAll(articleRepository.findAllByPmIdIn(pmIdList));
            else for (List<String> partition: Lists.partition(pmIdList, 1600)) articleList.addAll(articleRepository.findAllByPmIdIn(partition));
            final List<Article> toSave = Collections.synchronizedList(new ArrayList<>());
            articleList.parallelStream().forEach(a -> {
                if (a.getTopics() == null || a.getTopics().equals("")) {
                    a.setTopics("mutation:"+c.getSymbol());
                    toSave.add(a);
                }
                else if (!a.getTopics().endsWith("mutation:" + c.getSymbol()) && !a.getTopics().contains("mutation:" + c.getSymbol() + ";")) {
                    a.setTopics(a.getTopics() + ";" + "mutation:" + c.getSymbol());
                    toSave.add(a);
                }
            });
            articleRepository.saveAll(toSave);
        });
        log.info("Indexing genes..");
        geneMaps.forEach(c -> {
            articleList.clear();
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmIdList = pmids.parallelStream().map(Object::toString).collect(Collectors.toList());
            if (pmIdList.size() < 1800) articleList.addAll(articleRepository.findAllByPmIdIn(pmIdList));
            else for (List<String> partition: Lists.partition(pmIdList, 1600)) articleList.addAll(articleRepository.findAllByPmIdIn(partition));
            final List<Article> toSave = Collections.synchronizedList(new ArrayList<>());
            articleList.parallelStream().forEach(a -> {
                if (a.getTopics() == null || a.getTopics().equals("")) {
                    a.setTopics("gene:"+c.getSymbol());
                    toSave.add(a);
                }
                else if (!a.getTopics().endsWith("gene:" + c.getSymbol()) && !a.getTopics().contains("gene:" + c.getSymbol() + ";")) {
                    a.setTopics(a.getTopics() + ";" + "gene:" + c.getSymbol());
                    toSave.add(a);
                }
            });
            articleRepository.saveAll(toSave);
        });
        log.info("Article indexing complete");

    }

    @Override
    public void reindexAllArticles() {
        log.info("Deleting all indices and rebuilding the system");
        articleIndexRepository.deleteAll();
        indexAllArticles();
    }

    /**
     * Precondition: New articles (measured by UpdateConfig.xml passed here, Page * PageSize = starting Article location) are less than
     * 1-2 million (A Set of Integers with the relevant pmIds will use a lot of memory in this function)
     * @param uc - The UpdateConfig object with filled out Page and PageSize.
     */
    @Override
    public void indexNewArticles(UpdateConfig uc) {
        Page<Article> page;
        Set<Integer> PmIdsLatest = new HashSet<>();
        int index = 0;
        do {
            page = articleRepository.findAll(PageRequest.of(uc.getPage()+index, uc.getPageSize()));
            for (Article a: page) {
                PmIdsLatest.add(Integer.parseInt(a.getPmId()));
            }
            index++;
        } while (page.hasNext());
        List<CancerMap> cancerMaps = cancerMapRepository.findAll();
        List<MutationMap> mutationMaps = mutationMapRepository.findAll();
        List<GeneMap> geneMaps = geneMapRepository.findAll();
        List<DrugMap> drugMaps = drugMapRepository.findAll();
        cancerMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().filter(PmIdsLatest::contains).forEach(pmid -> {
                ArticleIndex ai;
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("cancer:" + c.getCancerType());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("cancer:" + c.getCancerType()))
                        ai.setTopics(ai.getTopics() + ";" + "cancer:" + c.getCancerType());
                }
                articleIndexRepository.save(ai);
                Article a = articleRepository.findByPmId(ai.getPmId());
                a.setTopics(ai.getTopics());
                articleRepository.save(a);
            });
        });
        drugMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().filter(PmIdsLatest::contains).forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("drug:" + c.getDrug());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("drug:" + c.getDrug()))
                        ai.setTopics(ai.getTopics() + ";" + "drug:" + c.getDrug());
                }
                articleIndexRepository.save(ai);
                Article a = articleRepository.findByPmId(ai.getPmId());
                a.setTopics(ai.getTopics());
                articleRepository.save(a);
            });
        });
        mutationMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().filter(PmIdsLatest::contains).forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("mutation:" + c.getSymbol());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("mutation:" + c.getSymbol()))
                        ai.setTopics(ai.getTopics() + ";" + "mutation:" + c.getSymbol());
                }
                articleIndexRepository.save(ai);
                Article a = articleRepository.findByPmId(ai.getPmId());
                a.setTopics(ai.getTopics());
                articleRepository.save(a);
            });
        });
        geneMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("gene:" + c.getSymbol());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("gene:" + c.getSymbol()))
                        ai.setTopics(ai.getTopics() + ";" + "gene:" + c.getSymbol());
                }
                articleIndexRepository.save(ai);
                Article a = articleRepository.findByPmId(ai.getPmId());
                a.setTopics(ai.getTopics());
                articleRepository.save(a);
            });
        });
        log.info("Article indexing complete");
    }

    public void indexNewItems(UpdateItems ui) {
        for (Item item : ui.getItems()) {
            log.info("Indexing " + item.getType() + ":" + item.getName());
            if (item.getType().equals("gene")) {
                GeneMap gm = geneMapRepository.findBySymbol(item.getName().toLowerCase());
                Set<Integer> pmids = gm.getListAsSet();
                pmids.parallelStream().forEach(pmid -> {
                    Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                    ArticleIndex ai;
                    if (oai.isEmpty()) {
                        ai = new ArticleIndex();
                        ai.setPmId(pmid + "");
                        ai.setTopics("gene:" + gm.getSymbol());
                    } else {
                        ai = oai.get();
                        if (!ai.getTopics().contains("gene:" + gm.getSymbol()))
                            ai.setTopics(ai.getTopics() + ";" + "gene:" + gm.getSymbol());
                    }
                    articleIndexRepository.save(ai);
                    Article a = articleRepository.findByPmId(ai.getPmId());
                    a.setTopics(ai.getTopics());
                    articleRepository.save(a);
                });
            }
            if (item.getType().equals("mutation")) {
                MutationMap mm = mutationMapRepository.findBySymbol(item.getName().toLowerCase());
                Set<Integer> pmids = mm.getListAsSet();
                pmids.parallelStream().forEach(pmid -> {
                    Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                    ArticleIndex ai;
                    if (oai.isEmpty()) {
                        ai = new ArticleIndex();
                        ai.setPmId(pmid + "");
                        ai.setTopics("mutation:" + mm.getSymbol());
                    } else {
                        ai = oai.get();
                        if (!ai.getTopics().contains("mutation:" + mm.getSymbol()))
                            ai.setTopics(ai.getTopics() + ";" + "mutation:" + mm.getSymbol());
                    }
                    articleIndexRepository.save(ai);
                    Article a = articleRepository.findByPmId(ai.getPmId());
                    a.setTopics(ai.getTopics());
                    articleRepository.save(a);
                });
            }
            if (item.getType().equals("drug")) {
                DrugMap dm = drugMapRepository.findByDrug(item.getName().toLowerCase());
                Set<Integer> pmids = dm.getListAsSet();
                pmids.parallelStream().forEach(pmid -> {
                    Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                    ArticleIndex ai;
                    if (oai.isEmpty()) {
                        ai = new ArticleIndex();
                        ai.setPmId(pmid + "");
                        ai.setTopics("drug:" + dm.getDrug());
                    } else {
                        ai = oai.get();
                        if (!ai.getTopics().contains("drug:" + dm.getDrug()))
                            ai.setTopics(ai.getTopics() + ";" + "drug:" + dm.getDrug());
                    }
                    articleIndexRepository.save(ai);
                    Article a = articleRepository.findByPmId(ai.getPmId());
                    a.setTopics(ai.getTopics());
                    articleRepository.save(a);
                });
            }
            if (item.getType().equals("camcer")) {
                CancerMap cm = cancerMapRepository.findByCancerType(item.getName().toLowerCase());
                Set<Integer> pmids = cm.getListAsSet();
                pmids.parallelStream().forEach(pmid -> {
                    Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                    ArticleIndex ai;
                    if (oai.isEmpty()) {
                        ai = new ArticleIndex();
                        ai.setPmId(pmid + "");
                        ai.setTopics("cancer:" + cm.getCancerType());
                    } else {
                        ai = oai.get();
                        if (!ai.getTopics().contains("cancer:" + cm.getCancerType()))
                            ai.setTopics(ai.getTopics() + ";" + "cancer:" + cm.getCancerType());
                    }
                    articleIndexRepository.save(ai);
                    Article a = articleRepository.findByPmId(ai.getPmId());
                    a.setTopics(ai.getTopics());
                    articleRepository.save(a);
                });
            }


        }
    }


}
