package org.mskcc.knowledge.service;

import com.google.common.collect.Lists;
import org.apache.solr.common.util.Pair;
import org.joda.time.DateTime;
import org.mskcc.knowledge.model.*;
import org.mskcc.knowledge.model.xml.Item;
import org.mskcc.knowledge.model.xml.UpdateConfig;
import org.mskcc.knowledge.model.xml.UpdateItems;
import org.mskcc.knowledge.repository.*;
import org.mskcc.knowledge.util.AminoAcids;
import org.mskcc.knowledge.util.ArticleQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {

    public final static Logger log = LoggerFactory.getLogger(MapService.class);

    private final ArticleRepository articleRepository;
    private final GeneMapRepository geneMapRepository;
    private final DrugMapRepository drugMapRepository;
    private final CancerMapRepository cancerMapRepository;
    private final MutationMapRepository mutationMapRepository;
    private final TargetRepository targetRepository;
    private final ArticleIndexRepository articleIndexRepository;
    private final SolrService solrService;
    private final GlobalTimestampRepository globalTimestampRepository;

    @Autowired
    public MapServiceImpl(ArticleRepository articleRepository, GeneMapRepository geneMapRepository, DrugMapRepository drugMapRepository, CancerMapRepository cancerMapRepository,
                          MutationMapRepository mutationMapRepository, TargetRepository targetRepository, ArticleIndexRepository articleIndexRepository,
                          SolrService solrService, GlobalTimestampRepository globalTimestampRepository) {
        this.articleRepository = articleRepository;
        this.geneMapRepository = geneMapRepository;
        this.drugMapRepository = drugMapRepository;
        this.cancerMapRepository = cancerMapRepository;
        this.mutationMapRepository = mutationMapRepository;
        this.targetRepository = targetRepository;
        this.solrService = solrService;
        this.articleIndexRepository = articleIndexRepository;
        this.globalTimestampRepository = globalTimestampRepository;
    }

    private final Map<String, Map<String, List<String>>> terms = new HashMap<>();
    private List<Pair<Integer, Integer>> geneIndexes, mutationIndexes, drugIndexes, cancerIndexes;
    private List<GeneMap> geneMaps;
    private List<MutationMap> mutationMaps;
    private List<DrugMap> drugMaps;
    private List<CancerMap> cancerMaps;

    @Override
    public int updateMapsAll(int count) {
        if (terms.keySet().size() == 0) {
            geneMaps = geneMapRepository.findAll();
            mutationMaps = mutationMapRepository.findAll();
            drugMaps = drugMapRepository.findAll();
            cancerMaps = cancerMapRepository.findAll();
            List<MutationMap> mutationMaps = mutationMapRepository.findAll();
            List<CancerMap> cancerMaps = cancerMapRepository.findAll();
            List<DrugMap> drugMaps = drugMapRepository.findAll();
            List<Target> targets = targetRepository.findAll();
            final Map<String, List<String>> genes = new HashMap<>();
            geneMaps.stream().forEach(g -> {
                    Optional<Target> ot = targets.stream().filter(t -> t.getSymbol().equalsIgnoreCase(g.getSymbol())).findAny();
                    final List<String> synonyms = new ArrayList<>();
                    ot.ifPresent(t -> synonyms.addAll(Arrays.asList(t.getSynonyms().split(";"))));
                    genes.put(g.getSymbol(), synonyms);
            });
            terms.put("gene", genes);
            final Map<String, List<String>> mutations = new HashMap<>();
            mutationMaps.stream().forEach(g -> {
                final List<String> synonyms = new ArrayList<>();
                synonyms.add(AminoAcids.mutationSynonym(g.getSymbol().toUpperCase()));
                mutations.put(g.getSymbol(), synonyms);
            });
            terms.put("mutation", mutations);
            final Map<String, List<String>> drugs = new HashMap<>();
            drugMaps.stream().forEach(d -> {
                final List<String> synonyms = new ArrayList<>();
                if (d.getSynonyms() != null && d.getSynonyms().length() > 0) {
                    synonyms.addAll(Arrays.asList(d.getSynonyms().split(";")));
                }
                drugs.put(d.getDrug(), synonyms);
            });
            terms.put("drug", drugs);
            final Map<String, List<String>> cancers = new HashMap<>();
            cancerMaps.stream().forEach(c -> {
                final List<String> synonyms = new ArrayList<>();
                if (c.getSynonyms() != null && c.getSynonyms().length() > 0) {
                    synonyms.addAll(Arrays.asList(c.getSynonyms().split(";")));
                }
                cancers.put(c.getCancerType(), synonyms);
            });
            terms.put("cancer", cancers);
            geneIndexes = partition(genes.keySet().size(), 100);
            mutationIndexes = partition(mutations.keySet().size(), 100);
            drugIndexes = partition(drugs.keySet().size(), 100);
            cancerIndexes = partition(cancers.keySet().size(), 100);
            log.info("Data initialized");
            return count;
        }

        int batch = count % 100;
        log.info("Running step {}", batch);
        Map<String, List<String>> item = terms.get("gene");
        List<String> itemList = new ArrayList<>(item.keySet()).subList(geneIndexes.get(batch).first(), geneIndexes.get(batch).second());
        for (String gene : itemList) {
            List<String> pmIdsRaw = solrService.searchSolr(1000000, Collections.singletonList(gene), Collections.singletonList(item.get(gene)), null, null, null, null, null, null, null, null, null).getPmIds();
            final Map<String, String> supportingText = new HashMap<>();
            List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                if (s.contains("S")) {
                    if (supportingText.containsKey(s.substring(0, s.indexOf("S")))) supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S")))+","+s);
                    else supportingText.put(s.substring(0, s.indexOf("S")), s);

                    return s.substring(0, s.indexOf("S"));
                }
                return s;
            }).collect(Collectors.toList());
            Set<Integer> pmIds = new LinkedHashSet<>(pmIdsWithDuplicates).stream().map(Integer::parseInt).collect(Collectors.toSet());
            Optional<GeneMap> ogm = geneMaps.stream().filter(gm -> gm.getSymbol().equalsIgnoreCase(gene)).findAny();
            GeneMap gm = null;
            if (ogm.isPresent()) {
                gm = ogm.get();
                Set<Integer> ids = gm.getListAsSet();
                ids.addAll(pmIds);
                gm.setListAsSet(ids);
                geneMapRepository.save(gm);
            }
        }

        item = terms.get("mutation");
        itemList = new ArrayList<>(item.keySet()).subList(mutationIndexes.get(batch).first(), mutationIndexes.get(batch).second());
        for (String mutation : itemList) {
            List<String> pmIdsRaw = solrService.searchSolr(500000, null, null, Collections.singletonList(mutation), Collections.singletonList(item.get(mutation)), null, null, null, null, null, null, null).getPmIds();
            final Map<String, String> supportingText = new HashMap<>();
            List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                if (s.contains("S")) {
                    if (supportingText.containsKey(s.substring(0, s.indexOf("S")))) supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S")))+","+s);
                    else supportingText.put(s.substring(0, s.indexOf("S")), s);

                    return s.substring(0, s.indexOf("S"));
                }
                return s;
            }).collect(Collectors.toList());
            Set<Integer> pmIds = new LinkedHashSet<>(pmIdsWithDuplicates).stream().map(Integer::parseInt).collect(Collectors.toSet());
            Optional<MutationMap> omm = mutationMaps.stream().filter(gm -> gm.getSymbol().equalsIgnoreCase(mutation)).findAny();
            MutationMap mm = null;
            if (omm.isPresent()) {
                mm = omm.get();
                Set<Integer> ids = mm.getListAsSet();
                ids.addAll(pmIds);
                mm.setListAsSet(ids);
                mutationMapRepository.save(mm);
            }
        }

        item = terms.get("drug");
        itemList = new ArrayList<>(item.keySet()).subList(drugIndexes.get(batch).first(), drugIndexes.get(batch).second());
        for (String drug : itemList) {
            List<String> pmIdsRaw = solrService.searchSolr(500000, null, null, null, null, Collections.singletonList(drug),null, null, null, null, null, null).getPmIds();
            final Map<String, String> supportingText = new HashMap<>();
            List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                if (s.contains("S")) {
                    if (supportingText.containsKey(s.substring(0, s.indexOf("S")))) supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S")))+","+s);
                    else supportingText.put(s.substring(0, s.indexOf("S")), s);

                    return s.substring(0, s.indexOf("S"));
                }
                return s;
            }).collect(Collectors.toList());
            Set<Integer> pmIds = new LinkedHashSet<>(pmIdsWithDuplicates).stream().map(Integer::parseInt).collect(Collectors.toSet());
            Optional<DrugMap> odm = drugMaps.stream().filter(dm -> dm.getDrug().equalsIgnoreCase(drug)).findAny();
            DrugMap dm = null;
            if (odm.isPresent()) {
                dm = odm.get();
                Set<Integer> ids = dm.getListAsSet();
                ids.addAll(pmIds);
                dm.setListAsSet(ids);
                drugMapRepository.save(dm);
            }
        }

        item = terms.get("cancer");
        itemList = new ArrayList<>(item.keySet()).subList(cancerIndexes.get(batch).first(), cancerIndexes.get(batch).second());
        for (String cancer : itemList) {
            List<String> pmIdsRaw = solrService.searchSolr(1000000, null, null, null, null, null, null, Collections.singletonList(cancer), null, null, null, null).getPmIds();
            final Map<String, String> supportingText = new HashMap<>();
            List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                if (s.contains("S")) {
                    if (supportingText.containsKey(s.substring(0, s.indexOf("S")))) supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S")))+","+s);
                    else supportingText.put(s.substring(0, s.indexOf("S")), s);

                    return s.substring(0, s.indexOf("S"));
                }
                return s;
            }).collect(Collectors.toList());
            Set<Integer> pmIds = new LinkedHashSet<>(pmIdsWithDuplicates).stream().map(Integer::parseInt).collect(Collectors.toSet());
            Optional<CancerMap> ocm = cancerMaps.stream().filter(cm -> cm.getCancerType().equalsIgnoreCase(cancer)).findAny();
            CancerMap cm = null;
            if (ocm.isPresent()) {
                cm = ocm.get();
                Set<Integer> ids = cm.getListAsSet();
                ids.addAll(pmIds);
                cm.setListAsSet(ids);
                cancerMapRepository.save(cm);
            }
        }
        log.info("Going to step {}", (count+1) % 100);
        if ((count+1) % 100 == 0) {
            Optional<GlobalTimestamp> ogt = globalTimestampRepository.findById("maps");
            if (ogt.isPresent()) {
                GlobalTimestamp gt = ogt.get();
                gt.setAfter(DateTime.now().plusDays(7));
                globalTimestampRepository.save(gt);
                log.info("Next scheduled map update is {}", gt.getAfter().toDateTimeISO());
            }
        }
        return count+1;
    }

    /**
     * Returns a List of the starting and ending indexes for each of the N sublists when dividing a list of size "size" into "N" nearly
     * equal length sublists.
     * @param size The number of elements in your list.
     * @param N The number of nearly equal subpartitions you want to divide your list into.
     * @return A List where each element is a pair, the first a starting index (i >= index1) and the second an ending index (i < index2) for the ith sublist.
     */
    private List<Pair<Integer, Integer>> partition(int size, int N) {
        double average = size / (double)N;
        int averageFloor = size / N;
        double diff = (average - averageFloor);
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < N; i++) {
            if (i == N-1) index2 = size;
            else if (i/(double)N < diff) index2 = index1 + averageFloor+1;
            else index2 = index1+averageFloor;
            result.add(new Pair<>(index1, index2));
            index1 = index2;
        }
        return result;
    }

    @Override
    public void addOneTerm(String name, List<String> synonyms, CalculationType calculationType) {
        String synonymString = "";
        if (calculationType == null) {
            log.error("CalculationType cannot be equal to null, skipping addOneTerm");
            return;
        }
        String regex = "(([\\W\\s^_]|^)(" + name.toLowerCase() + ")([\\W\\s^_]|$))";
        if (synonyms != null) {
            for (String synonym : synonyms) {
                if (synonymString.length() == 0) synonymString += synonym.toLowerCase();
                else synonymString += ";" + synonym.toLowerCase();
                regex += "|" + "(([\\W\\s^_]|^)(" + synonym.toLowerCase() + ")([\\W\\s^_]|$))";
            }
        }
        Pattern pattern = Pattern.compile(regex);

        int page = 0;
        Page<Article> pages;
        name = name.toLowerCase();

        do {
            pages = articleRepository.findAll(PageRequest.of(page, 500000));
            Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> pattern.matcher(s).find())
                    .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());

            if (calculationType == CalculationType.GENE) {
                GeneMap gm = geneMapRepository.findBySymbol(name);
                if (gm != null) {
                    Set<Integer> oldSet = gm.getListAsSet();
                    oldSet.addAll(pmids);
                    gm.setSynonyms(synonymString);
                    gm.setListAsSet(oldSet);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    gm = new GeneMap(name, pm, synonymString);
                    gm.setSymbol(name);
                    gm.setListAsSet(pmids);
                }
                try {
                    geneMapRepository.save(gm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                }
            }
            else if (calculationType == CalculationType.MUTATION) {
                MutationMap mm = mutationMapRepository.findBySymbol(name);
                if (mm != null) {
                    Set<Integer> oldSet = mm.getListAsSet();
                    oldSet.addAll(pmids);
                    mm.setSynonyms(synonymString);
                    mm.setListAsSet(oldSet);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    mm = new MutationMap(name, pm, synonymString);
                    mm.setSymbol(name);
                    mm.setListAsSet(pmids);
                }
                try {
                    mutationMapRepository.save(mm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                }
            }
            else if (calculationType == CalculationType.DRUG) {
                Optional<DrugMap> optionalDrugMap = drugMapRepository.findById(name);
                DrugMap dm = null;
                if (optionalDrugMap.isPresent()) dm = optionalDrugMap.get();
                if (dm != null) {
                    Set<Integer> oldSet = dm.getListAsSet();
                    oldSet.addAll(pmids);
                    dm.setSynonyms(synonymString);
                    dm.setListAsSet(oldSet);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    dm = new DrugMap(name, pm, synonymString, null);
                    dm.setDrug(name);
                    dm.setListAsSet(pmids);
                }
                try {
                    drugMapRepository.save(dm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                }
            }
            else if (calculationType == CalculationType.CANCER) {
                CancerMap cm = cancerMapRepository.findByCancerType(name);
                if (cm != null) {
                    Set<Integer> oldSet = cm.getListAsSet();
                    oldSet.addAll(pmids);
                    cm.setSynonyms(synonymString);
                    cm.setListAsSet(oldSet);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    cm = new CancerMap(name, pm, synonymString);
                    cm.setCancerType(name);
                    cm.setListAsSet(pmids);
                }
                try {
                    cancerMapRepository.save(cm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                }
            }
            page++;
            log.info("Page completed, attempting page {}", page);
        } while (pages.hasNext());
        log.info("All done");
    }

    @Override
    public void addManyTerms(UpdateItems updateItems) {
        addManyTerms(updateItems, 0);
    }

    @Override
    public void addManyTerms(UpdateItems updateItems, int pageNumber) {
        Map<String, Pattern> patternMap = new HashMap<>();
        for (Item item: updateItems.getItems()) {
            String regex = "(([\\W\\s^_]|^)(" + item.getName().toLowerCase() + ")([\\W\\s^_]|$))";
            if (item.getSynonyms() != null) {
                for (int i = 0; i < item.getSynonyms().size(); i++)
                    regex += "|" + "(([\\W\\s^_]|^)(" + item.getSynonyms().get(i).toLowerCase() + ")([\\W\\s^_]|$))";
            } else {
                // uxe the standard synonym finders
                if (item.getType().equals("mutation")) {
                    item.setSynonyms(Collections.singletonList(AminoAcids.buildExpression(item.getName())));
                    regex += "|" + "(([\\W\\s^_]|^)(" + item.getSynonyms().get(0).toLowerCase() + ")([\\W\\s^_]|$))";
                }
                if (item.getType().equals("gene")) {
                    Optional<Target> ot = targetRepository.findBySymbol(item.getName());
                    ot.ifPresent(target -> item.setSynonyms(Arrays.asList(target.getSynonyms().split(";"))));
                    if (item.getSynonyms().size() > 0) for (int i = 0; i < item.getSynonyms().size(); i++)
                        regex += "|" + "(([\\W\\s^_]|^)(" + item.getSynonyms().get(i).toLowerCase() + ")([\\W\\s^_]|$))";
                }
            }
            log.info(item.getName() + " " + item.getType() + " " + regex);
            Pattern pattern = Pattern.compile(regex);
            patternMap.put(item.getName().toLowerCase(), pattern);
        }

        int pageSize = updateItems.getPageSize();
        if (updateItems.getPageSize() != null) pageSize = updateItems.getPageSize();

        int page = pageNumber;
        Page<Article> pages;

        do {
            pages = articleRepository.findAll(PageRequest.of(page, pageSize));

            for (Item item: updateItems.getItems()) {
                String name = item.getName().toLowerCase();
                String synonymString = "";
                if (item.getSynonyms() != null) {
                    for (String synonym: item.getSynonyms()) {
                        if (synonymString.length() == 0) synonymString += synonym.toLowerCase();
                        else synonymString += ";" + synonym.toLowerCase();
                    }
                }
                CalculationType calculationType = null;
                if (item.getType().equalsIgnoreCase("gene")) calculationType = CalculationType.GENE;
                if (item.getType().equalsIgnoreCase("mutation")) calculationType = CalculationType.MUTATION;
                if (item.getType().equalsIgnoreCase("drug")) calculationType = CalculationType.DRUG;
                if (item.getType().equalsIgnoreCase("cancer")) calculationType = CalculationType.CANCER;
                if (calculationType == null) {
                    log.error("ERROR: Item {} has invalid type (types = ['gene', 'mutation', 'drug', 'cancer'], skipping.", name);
                    continue;
                }
                Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> patternMap.get(name).matcher(s).find())
                        .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());

                if (calculationType == CalculationType.GENE) {
                    Optional<GeneMap> optionalGeneMap = geneMapRepository.findById(name);
                    GeneMap gm = null;
                    if (optionalGeneMap.isPresent()) gm = optionalGeneMap.get();
                    if (gm != null) {
                        Set<Integer> oldSet = gm.getListAsSet();
                        oldSet.addAll(pmids);
                        gm.setSynonyms(synonymString);
                        gm.setListAsSet(oldSet);
                    } else {
                        Integer[] pm = pmids.toArray(Integer[]::new);
                        gm = new GeneMap(name, pm, synonymString);
                        gm.setSymbol(name);
                        gm.setListAsSet(pmids);
                    }
                    try {
                        geneMapRepository.save(gm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                    }
                } else if (calculationType == CalculationType.MUTATION) {
                    Optional<MutationMap> optionalMutationMap = mutationMapRepository.findById(name);
                    MutationMap mm = null;
                    if (optionalMutationMap.isPresent()) mm = optionalMutationMap.get();
                    if (mm != null) {
                        Set<Integer> oldSet = mm.getListAsSet();
                        oldSet.addAll(pmids);
                        mm.setSynonyms(synonymString);
                        mm.setListAsSet(oldSet);
                    } else {
                        Integer[] pm = pmids.toArray(Integer[]::new);
                        mm = new MutationMap(name, pm, synonymString);
                        mm.setSymbol(name);
                        mm.setListAsSet(pmids);
                    }
                    try {
                        mutationMapRepository.save(mm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                    }
                } else if (calculationType == CalculationType.DRUG) {
                    Optional<DrugMap> optionalDrugMap = drugMapRepository.findById(name);
                    DrugMap dm = null;
                    if (optionalDrugMap.isPresent()) dm = optionalDrugMap.get();
                    if (dm != null) {
                        Set<Integer> oldSet = dm.getListAsSet();
                        oldSet.addAll(pmids);
                        dm.setSynonyms(synonymString);
                        dm.setListAsSet(oldSet);
                    } else {
                        Integer[] pm = pmids.toArray(Integer[]::new);
                        dm = new DrugMap(name, pm, synonymString, null);
                        dm.setDrug(name);
                        dm.setListAsSet(pmids);
                    }
                    try {
                        drugMapRepository.save(dm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                    }
                } else {
                    Optional<CancerMap> optionalCancerMap = cancerMapRepository.findById(name);
                    CancerMap cm = null;
                    if (optionalCancerMap.isPresent()) cm = optionalCancerMap.get();
                    if (cm != null) {
                        Set<Integer> oldSet = cm.getListAsSet();
                        oldSet.addAll(pmids);
                        cm.setSynonyms(synonymString);
                        cm.setListAsSet(oldSet);
                    } else {
                        Integer[] pm = pmids.toArray(Integer[]::new);
                        cm = new CancerMap(name, pm, synonymString);
                        cm.setCancerType(name);
                        cm.setListAsSet(pmids);
                    }
                    try {
                        cancerMapRepository.save(cm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", name);
                    }
                }
            }
            page++;
            log.info("Page completed, attempting page {}", page);
        } while (pages.hasNext());
        log.info("All done");
    }


    @Override
    public void updateMaps(UpdateConfig settings) {
        if (settings == null) {
            settings = UpdateConfig.builder().genes(true).mutations(true).cancers(true).drugs(true).complete(true).pageSize(500000).page(0).build();
        }

        ArticleQuery q = new ArticleQuery();
        q.setTargetRepository(targetRepository);
        q.setMutationMapRepository(mutationMapRepository);
        if (settings.getGenes()) q.buildGenePatterns(settings.getGeneCleanup());
        if (settings.getMutations()) q.buildMutationPatterns();
        if (settings.getDrugs()) q.buildDrugPatterns();
        if (settings.getCancers()) q.buildCancerPatterns();
        Page<Article> pages;
        int index = 0;
        do {
            log.info("Page {}", settings.getPage()+index);
            pages = articleRepository.findAll(PageRequest.of(settings.getPage()+index, settings.getPageSize()));

            if (settings.getGenes())
                log.info("CHECKING GENES FOR " + q.getGenePatternMap().keySet().size() + " ITEMS");
            if (settings.getGenes()) for (String gene : q.getGenePatternMap().keySet()) {
                Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> q.getGenePatternMap().get(gene).matcher(s).find())
                        .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());
                GeneMap gm = geneMapRepository.findBySymbol(gene);
                if (gm != null) {
                    Set<Integer> oldSet = gm.getListAsSet();
                    oldSet.addAll(pmids);
                    gm.setListAsSet(oldSet);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("gene:"+gene);
                        else if (!article.getTopics().endsWith("gene:"+gene) && !article.getTopics().contains("gene:"+gene+";"))
                            article.setTopics(article.getTopics()+";gene:"+gene);
                    });
                    articleRepository.saveAll(articles);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    gm = new GeneMap(gene, pm, q.getGeneSynonymMap().get(gene));
                    gm.setSymbol(gene);
                    gm.setListAsSet(pmids);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("gene:"+gene);
                        else if (!article.getTopics().endsWith("gene:"+gene) && !article.getTopics().contains("gene:"+gene+";"))
                            article.setTopics(article.getTopics()+";gene:"+gene);
                    });
                    articleRepository.saveAll(articles);
                }
                try {
                    geneMapRepository.save(gm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", gene);
                }
            }
            if (settings.getMutations())
                log.info("CHECKING MUTATIONS FOR " + q.getMutationPatternMap().keySet().size() + " ITEMS");
            if (settings.getMutations()) {
                for (String gene : q.getMutationPatternMap().keySet()) {
                    Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> q.getMutationPatternMap().get(gene).matcher(s).find())
                            .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());
                    MutationMap mm = mutationMapRepository.findBySymbol(gene);
                    if (mm != null) {
                        Set<Integer> oldSet = mm.getListAsSet();
                        oldSet.addAll(pmids);
                        mm.setListAsSet(oldSet);
                        List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                        articles.forEach(article -> {
                            if (article.getTopics() == null) article.setTopics("mutation:"+gene);
                            else if (!article.getTopics().endsWith("mutation:"+gene) && !article.getTopics().contains("mutation:"+gene+";"))
                                article.setTopics(article.getTopics()+";mutation:"+gene);
                        });
                        articleRepository.saveAll(articles);
                    } else {
                        Integer[] pm = pmids.toArray(Integer[]::new);
                        mm = new MutationMap(gene, pm, "");
                        mm.setSymbol(gene);
                        mm.setListAsSet(pmids);
                        List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                        articles.forEach(article -> {
                            if (article.getTopics() == null) article.setTopics("mutation:"+gene);
                            else if (!article.getTopics().endsWith("mutation:"+gene) && !article.getTopics().contains("mutation:"+gene+";"))
                                article.setTopics(article.getTopics()+";mutation:"+gene);
                        });
                        articleRepository.saveAll(articles);
                    }
                    try {
                        mutationMapRepository.save(mm);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("THIS WILL AFFECT ALL THE RESULTS FOR MUTATION {}", gene);
                    }
                }
            }
            if (settings.getDrugs())
                log.info("CHECKING DRUGS FOR " + q.getDrugPatternMap().keySet().size() + " ITEMS");
            if (settings.getDrugs()) for (String drug : q.getDrugPatternMap().keySet()) {
                Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> q.getDrugPatternMap().get(drug).matcher(s).find())
                        .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());
                DrugMap dm = drugMapRepository.findByDrug(drug);
                if (dm != null) {
                    Set<Integer> oldSet = dm.getListAsSet();
                    oldSet.addAll(pmids);
                    dm.setListAsSet(oldSet);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("drug:"+drug);
                        else if (!article.getTopics().endsWith("drug:"+drug) && !article.getTopics().contains("drug:"+drug+";"))
                            article.setTopics(article.getTopics()+";drug:"+drug);
                    });
                    articleRepository.saveAll(articles);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    dm = new DrugMap(drug, pm, "", null);
                    dm.setDrug(drug);
                    dm.setListAsSet(pmids);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("drug:"+drug);
                        else if (!article.getTopics().endsWith("drug:"+drug) && !article.getTopics().contains("drug:"+drug+";"))
                            article.setTopics(article.getTopics()+";drug:"+drug);
                    });
                    articleRepository.saveAll(articles);
                }
                try {
                    drugMapRepository.save(dm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", drug);
                }
            }
            if (settings.getCancers())
                log.info("CHECKING CANCERTYPES FOR " + q.getCancerPatternMap().keySet().size() + " ITEMS");
            if (settings.getCancers()) for (String cancer : q.getCancerPatternMap().keySet()) {
                Set<Integer> pmids = pages.stream().parallel().map(a -> (a.getPmId().trim() + " " + a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms()).toLowerCase()).filter(s -> q.getCancerPatternMap().get(cancer).matcher(s).find())
                        .mapToInt(s -> Integer.parseInt(s.substring(0, s.indexOf(' ')))).boxed().collect(Collectors.toSet());
                CancerMap cm = cancerMapRepository.findByCancerType(cancer);
                if (cm != null) {
                    Set<Integer> oldSet = cm.getListAsSet();
                    oldSet.addAll(pmids);
                    cm.setListAsSet(oldSet);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("cancer:"+cancer);
                        else if (!article.getTopics().endsWith("cancer:"+cancer) && !article.getTopics().contains("cancer:"+cancer+";"))
                            article.setTopics(article.getTopics()+";cancer:"+cancer);
                    });
                    articleRepository.saveAll(articles);
                } else {
                    Integer[] pm = pmids.toArray(Integer[]::new);
                    cm = new CancerMap(cancer, pm, "");
                    cm.setCancerType(cancer);
                    cm.setListAsSet(pmids);
                    List<Article> articles = articleRepository.findAllByPmIdIn(new ArrayList<>(pmids.parallelStream().map(s -> s + "").collect(Collectors.toList())));
                    articles.forEach(article -> {
                        if (article.getTopics() == null) article.setTopics("cancer:"+cancer);
                        else if (!article.getTopics().endsWith("cancer:"+cancer) && !article.getTopics().contains("cancer:"+cancer+";"))
                            article.setTopics(article.getTopics()+";cancer:"+cancer);
                    });
                    articleRepository.saveAll(articles);
                }
                try {
                    cancerMapRepository.save(cm);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("THIS WILL AFFECT ALL THE RESULTS FOR {}", cancer);
                }
            }
            index++;
        } while (settings.getComplete() && pages.hasNext());
    }


    @Override
    public void indexArticles() {
        List<CancerMap> cancerMaps = cancerMapRepository.findAll();
        List<MutationMap> mutationMaps = mutationMapRepository.findAll();
        List<GeneMap> geneMaps = geneMapRepository.findAll();
        List<DrugMap> drugMaps = drugMapRepository.findAll();
        cancerMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmidList = pmids.stream().map(item -> ""+item).collect(Collectors.toList());
            final List<ArticleIndex> items = new LinkedList<>();
            final List<ArticleIndex> toAdd = new Vector<>();
            List<Article> articles = new LinkedList<>();
            for (List<String> partition: Lists.partition(pmidList, 300)) {
                items.addAll(articleIndexRepository.findAllByPmIdIn(partition));
                articles.addAll(articleRepository.findAllByPmIdIn(partition));
            }
            pmids.parallelStream().forEach(pmid -> {
                ArticleIndex ai = null;
                if (items.stream().noneMatch(i -> i.getPmId().equals(pmid + ""))) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid+"");
                    ai.setTopics("cancer:" + c.getCancerType());
                } else {
                    Optional<ArticleIndex> opt = items.stream().filter(i -> i.getPmId().equals(pmid+"")).findFirst();
                    if (opt.isPresent()) ai = opt.get();
                    if (ai != null && !ai.getTopics().contains("cancer:" + c.getCancerType())) {
                        if (ai.getTopics() == null || ai.getTopics().length() == 0)
                            ai.setTopics("cancer:"+c.getCancerType());
                        else
                            ai.setTopics(ai.getTopics() + ";" + "cancer:" + c.getCancerType());
                    }
                }
                /*Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("cancer:" + c.getCancerType());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("cancer:" + c.getCancerType()))
                        ai.setTopics(ai.getTopics() + ";" + "cancer:" + c.getCancerType());
                }*/
                toAdd.add(ai);
                //articleIndexRepository.save(ai);
                final ArticleIndex articleIndex = ai;
                articles.stream().filter(a -> a.getPmId().equals(articleIndex.getPmId())).forEach(a -> a.setTopics(articleIndex.getTopics()));
            });
            for (List<ArticleIndex> partition: Lists.partition(toAdd, 300)) {
                articleIndexRepository.saveAll(partition);
            }
            for (List<Article> partition : Lists.partition(articles, 300)) {
                articleRepository.saveAll(partition);
            }
        });
        drugMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmidList = pmids.stream().map(item -> ""+item).collect(Collectors.toList());
            final List<ArticleIndex> items = new LinkedList<>();
            final List<ArticleIndex> toAdd = new Vector<>();
            List<Article> articles = new LinkedList<>();
            for (List<String> partition: Lists.partition(pmidList, 300)) {
                items.addAll(articleIndexRepository.findAllByPmIdIn(partition));
                articles.addAll(articleRepository.findAllByPmIdIn(partition));
            }
            pmids.parallelStream().forEach(pmid -> {
                ArticleIndex ai = null;
                if (items.stream().noneMatch(i -> i.getPmId().equals(pmid + ""))) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid+"");
                    ai.setTopics("drug:" + c.getDrug());
                } else {
                    Optional<ArticleIndex> opt = items.stream().filter(i -> i.getPmId().equals(pmid+"")).findFirst();
                    if (opt.isPresent()) ai = opt.get();
                    if (ai != null && !ai.getTopics().contains("drug:" + c.getDrug())) {
                        if (ai.getTopics() == null || ai.getTopics().length() == 0)
                            ai.setTopics("drug:"+c.getDrug());
                        else
                            ai.setTopics(ai.getTopics() + ";" + "drug:" + c.getDrug());
                    }
                }
                toAdd.add(ai);
                final ArticleIndex articleIndex = ai;
                articles.stream().filter(a -> a.getPmId().equals(articleIndex.getPmId())).forEach(a -> a.setTopics(articleIndex.getTopics()));
            });
            for (List<ArticleIndex> partition: Lists.partition(toAdd, 300)) {
                articleIndexRepository.saveAll(partition);
            }
            for (List<Article> partition : Lists.partition(articles, 300)) {
                articleRepository.saveAll(partition);
            }
        });
        mutationMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmidList = pmids.stream().map(item -> ""+item).collect(Collectors.toList());
            final List<ArticleIndex> items = new LinkedList<>();
            final List<ArticleIndex> toAdd = new Vector<>();
            List<Article> articles = new LinkedList<>();
            for (List<String> partition: Lists.partition(pmidList, 300)) {
                items.addAll(articleIndexRepository.findAllByPmIdIn(partition));
                articles.addAll(articleRepository.findAllByPmIdIn(partition));
            }
            pmids.parallelStream().forEach(pmid -> {
                ArticleIndex ai = null;
                if (items.stream().noneMatch(i -> i.getPmId().equals(pmid + ""))) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid+"");
                    ai.setTopics("mutation:" + c.getSymbol());
                } else {
                    Optional<ArticleIndex> opt = items.stream().filter(i -> i.getPmId().equals(pmid+"")).findFirst();
                    if (opt.isPresent()) ai = opt.get();
                    if (ai != null && !ai.getTopics().contains("mutation:" + c.getSymbol())) {
                        if (ai.getTopics() == null || ai.getTopics().length() == 0)
                            ai.setTopics("mutation:"+c.getSymbol());
                        else
                            ai.setTopics(ai.getTopics() + ";" + "mutation:" + c.getSymbol());
                    }
                }
                toAdd.add(ai);
                final ArticleIndex articleIndex = ai;
                articles.stream().filter(a -> a.getPmId().equals(articleIndex.getPmId())).forEach(a -> a.setTopics(articleIndex.getTopics()));
            });
            for (List<ArticleIndex> partition: Lists.partition(toAdd, 300)) {
                articleIndexRepository.saveAll(partition);
            }
            for (List<Article> partition : Lists.partition(articles, 300)) {
                articleRepository.saveAll(partition);
            }
        });
        geneMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            List<String> pmidList = pmids.stream().map(item -> ""+item).collect(Collectors.toList());
            final List<ArticleIndex> items = new LinkedList<>();
            final List<ArticleIndex> toAdd = new Vector<>();
            List<Article> articles = new LinkedList<>();
            for (List<String> partition: Lists.partition(pmidList, 300)) {
                items.addAll(articleIndexRepository.findAllByPmIdIn(partition));
                articles.addAll(articleRepository.findAllByPmIdIn(partition));
            }
            pmids.parallelStream().forEach(pmid -> {
                ArticleIndex ai = null;
                if (items.stream().noneMatch(i -> i.getPmId().equals(pmid + ""))) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid+"");
                    ai.setTopics("gene:" + c.getSymbol());
                } else {
                    Optional<ArticleIndex> opt = items.stream().filter(i -> i.getPmId().equals(pmid+"")).findFirst();
                    if (opt.isPresent()) ai = opt.get();
                    if (ai != null && !ai.getTopics().contains("gene:" + c.getSymbol())) {
                        if (ai.getTopics() == null || ai.getTopics().length() == 0)
                            ai.setTopics("gene:"+c.getSymbol());
                        else
                            ai.setTopics(ai.getTopics() + ";" + "gene:" + c.getSymbol());
                    }
                }
                toAdd.add(ai);
                final ArticleIndex articleIndex = ai;
                articles.stream().filter(a -> a.getPmId().equals(articleIndex.getPmId())).forEach(a -> a.setTopics(articleIndex.getTopics()));
            });
            for (List<ArticleIndex> partition: Lists.partition(toAdd, 300)) {
                articleIndexRepository.saveAll(partition);
            }
            for (List<Article> partition : Lists.partition(articles, 300)) {
                articleRepository.saveAll(partition);
            }

        });
    }

}
