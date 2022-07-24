package org.magicat.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.joda.time.DateTime;
import org.magicat.model.*;
import org.magicat.repository.*;
import org.magicat.util.AminoAcids;
import org.magicat.util.ArticleQuery;
import org.magicat.util.SolrClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.magicat.util.SolrClientTool.quote;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    public final static Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static int processedCount = 0;


    private final ArticleRepository articleRepository;
    private final FullTextRepository fullTextRepository;
    private final ProjectListRepository projectListRepository;
    private final TargetRepository targetRepository;
    private final VariantRepository variantRepository;
    private final SolrService solrService;
    private final FullTextService fullTextService;
    private final TextService textService;
    private final VariantService variantService;

    private String key;
    private String thread = "";
    private String stop = "";


    @Autowired
    public AnalyticsServiceImpl(ArticleRepository articleRepository, FullTextRepository fullTextRepository, TargetRepository targetRepository,
                                VariantRepository variantRepository, SolrService solrService, FullTextService fullTextService, TextService textService,
                                ProjectListRepository projectListRepository, VariantService variantService) {
        this.articleRepository = articleRepository;
        this.fullTextRepository = fullTextRepository;
        this.targetRepository = targetRepository;
        this.variantRepository = variantRepository;
        this.solrService = solrService;
        this.fullTextService = fullTextService;
        this.textService = textService;
        this.projectListRepository = projectListRepository;
        this.variantService = variantService;
    }

    public byte[] processSpreadsheet(String filename) {
        Workbook wb;
        Sheet firstSheet;
        try (FileInputStream inputStream = new FileInputStream(filename);) {
            wb = new XSSFWorkbook(inputStream);
            firstSheet = wb.getSheetAt(0);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Map<String, List<ArticleQuery.WordPair>> q = ArticleQuery.tsvReaderSimple(new File("allAnnotatedVariants.txt"));
        Row row = firstSheet.getRow(0);
        Cell cell = row.getCell(5);
        if (cell == null) cell = row.createCell(5);
        cell.setCellValue("PMIDs from OncoKB Curation");
        cell = row.createCell(6);
        cell.setCellValue("Automated PMIDs (limit 200 PMIDs listed)");
        cell = row.createCell(7);
        cell.setCellValue("Matching PMIDs");

        log.info("Reading rows");
        List<Variant> variantList = new ArrayList<>();
        for (int i = 1; i <= firstSheet.getLastRowNum(); i++) {
            String gene = "", mutation = "", cancerType = "", drug = "";
            Set<String> articleSet = new HashSet<>();
            row = firstSheet.getRow(i);
            cell = row.getCell(0);
            if (cell != null) gene = cell.getStringCellValue().trim();
            cell = row.getCell(1);
            if (cell != null) mutation = cell.getStringCellValue().trim();
            //if (mutation.endsWith("fusion")) mutation = mutation.substring(0, mutation.toLowerCase().indexOf("fusion")-1).trim();

            cell = row.getCell(2);
            if (cell != null) cancerType = cell.getStringCellValue().trim();
            if (cancerType.endsWith(", NOS")) cancerType = cancerType.substring(0, cancerType.indexOf(", NOS"));
            cell = row.getCell(3);
            if (cell != null) drug = cell.getStringCellValue().toLowerCase().trim();
            cell = row.getCell(5);
            String pmidString = null;
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                pmidString = ((int)cell.getNumericCellValue()) + "";
                articleSet.add((int) (cell.getNumericCellValue()) + "");
            }
            else if (cell != null && cell.getCellType() == CellType.STRING) {
                pmidString = cell.getStringCellValue();
                String[] pmidsmanual = cell.getStringCellValue().trim().split(", ");
                articleSet.addAll(Arrays.asList(pmidsmanual));
            }
            if (cancerType.equals("All Solid Tumors")) cancerType = "";
            log.info("Gene: " + gene + " Mutation: " + mutation + " CancerType: " + cancerType + " drug(s):" + drug + " ");
            Variant v = variantRepository.findByDescriptor(gene+":"+mutation+":"+cancerType+":"+drug);
            if (v == null) {
                v = new Variant();
                v.setGene(gene);
                v.setMutation(mutation);
                if (drug.length() > 0) v.setDrugs(drug);
                if (cancerType.length() > 0) v.setCancerTypes(cancerType);
                v.setDescriptor(gene+":"+mutation+":"+cancerType+":"+drug);
            }
            if (pmidString != null) v.setCuratedPMIds(pmidString);
            if (getKey() != null) v.setKey(getKey());
            variantRepository.save(v);
            variantList.add(v);
        }
        return processVariants(variantList);
    }

    @Override
    public byte[] processVariants(List<Variant> variants) {
        final boolean threaded = !thread.equals("");
        final String threadId = threaded ? thread : "";
        if (threaded) {
            thread = "";
        }

        Workbook wb;
        try {
            wb = WorkbookFactory.create(true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Sheet firstSheet = wb.createSheet("Gene and Alteration data");
        Row row = firstSheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Gene");
        cell = row.createCell(1);
        cell.setCellValue("Alteration");
        cell = row.createCell(2);
        cell.setCellValue("Cancer type");
        cell = row.createCell(3);
        cell.setCellValue("Drug(s)");

        cell = row.createCell(5);
        cell.setCellValue("Curated PMIDs");

        cell = row.createCell(6);
        cell.setCellValue("Discovered PMIDs");

        cell = row.createCell(7);
        cell.setCellValue("Document and Supporting URLs");

        cell = row.createCell(8);
        cell.setCellValue("Document and Supporting pages");

        cell = row.createCell(9);
        cell.setCellValue("Consensus PMIDs");

        cell = row.createCell(10);
        cell.setCellValue("Tier 1 Articles");

        cell = row.createCell(11);
        cell.setCellValue("Tier 2 Articles");

        cell = row.createCell(12);
        cell.setCellValue("Tier 1 Score Codes");

        cell = row.createCell(13);
        cell.setCellValue("Tier 2 Score Codes");


        int[] count = {1};
        //final Semaphore concurrentDFSExecutions = new Semaphore(16);
        List<ProjectList> pl = projectListRepository.findIdExpression("keywordListM");
        for (Variant v : variants) {
            // this line below if for resuming when the data is just being re-curated again and something disconnects or crashes
            if (v.getLastUpdate() != null && v.getLastUpdate().isAfter(DateTime.now().minusHours(12))) continue;  //.minusDays(0))) continue;
            String gene = v.getGene();
            String mutation = null;
            if (v.getMutation() != null) {
                if (v.getMutation().toUpperCase().endsWith("FUSION")) mutation = v.getMutation().substring(0, v.getMutation().toUpperCase().indexOf(" FUSION"));
                else mutation = v.getMutation();
            }
            if (mutation != null && (mutation.equals("") || mutation.equals("Oncogenic Mutations"))) {
                log.info("Blank mutation or 'Oncogenic Mutations', continuing");
                continue;
            }
            //Optional<Target> ot = targetRepository.findBySymbol(gene);
            //List<String> geneSynonyms = ot.map(target -> Arrays.asList(escape(target.getSynonyms()).split(";"))).orElse(null);
            List<Target> targetList = targetRepository.findAllBySymbol(gene);  // treat multiple hits
            List<String> geneSynonyms = null;
            if (targetList != null && targetList.size() > 0) {
                for (Target t : targetList) {
                    if (t.getSynonyms() != null) {
                        if (geneSynonyms == null) geneSynonyms = new ArrayList<>(List.of(t.getSynonyms().split(";")));
                        else geneSynonyms.addAll(Arrays.asList(t.getSynonyms().split(";")));
                    }
                }
            }
            List<String> mutationSynonyms = (mutation != null && !AminoAcids.mutationSynonym(mutation).equalsIgnoreCase(mutation) ? new ArrayList<>() : null);
            if (mutationSynonyms != null) mutationSynonyms.add(AminoAcids.mutationSynonym(mutation));
            List<String> drugs = null;
            List<String> cancerTypes = null;
            List<String> hotspotSynonyms = mutation != null ? AminoAcids.hotspotSubstitution(mutation) : null;
            if (hotspotSynonyms != null && mutationSynonyms != null) {
                hotspotSynonyms.addAll(mutationSynonyms);
                mutationSynonyms = hotspotSynonyms;
            }
            if (v.getDrugs() != null && v.getDrugs().length() > 0) {
                drugs = Arrays.asList(v.getDrugs().split(", "));
            }
            boolean containsWildcard = false;
            if (v.getCancerTypes() != null && v.getCancerTypes().length() > 0) cancerTypes = Collections.singletonList(v.getCancerTypes());
            if (mutation != null && mutation.contains("*")) solrService.getSolrClientTool().setDefaultField("text_ws");
            else solrService.getSolrClientTool().setDefaultField("text");
            if (mutation.contains("?")) {
                containsWildcard = true;
                mutation = mutation.replace("?", "*");
                if (mutationSynonyms.size() > 0) {
                    mutationSynonyms.set(0, mutationSynonyms.get(0).replace("?", "*"));
                }
            }
            SolrService.SearchResult result = solrService.searchSolr(500, Collections.singletonList(gene), Collections.singletonList(geneSynonyms), mutation != null ? Collections.singletonList(mutation) : null,
                    Collections.singletonList(mutationSynonyms), drugs, null, cancerTypes, null, null, null, null);
            List<String> pmIdsRaw = result.getPmIds();
            log.info("Pass 1 with 500 limit{}", pmIdsRaw);  // display before merging
            final Map<String, String> supportingText = new HashMap<>();
            List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                if (s.contains("S")) {
                    if (supportingText.containsKey(s.substring(0, s.indexOf("S"))))
                        supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S"))) + "," + s);
                    else supportingText.put(s.substring(0, s.indexOf("S")), s);

                    return s.substring(0, s.indexOf("S"));
                }
                return s;
            }).collect(Collectors.toList());
            List<String> pmIds = new ArrayList<>(new LinkedHashSet<>(pmIdsWithDuplicates));
            List<Article> articlesNoFull = articleRepository.findAllByPmIdInNoFullText(pmIds);
            boolean rescore = false;
            for (Article a: articlesNoFull) {
                log.info("Trying to get article {}", a.getPmId());
                if ((a.getHasFullText() == null || (!a.getHasFullText() /*&& a.getLastChecked() != null && a.getLastChecked().isBefore(DateTime.now().minusDays(0))*/)) && fullTextService.addArticle(a)) rescore = true;
                else {
                    a.setHasFullText(false);
                    a.setLastChecked(DateTime.now());
                    articleRepository.save(a);
                }
            }
            //Map<String, String> pmIdText = new HashMap<>();
            if (/*rescore*/true) {
                result = solrService.searchSolr(300, Collections.singletonList(gene), Collections.singletonList(geneSynonyms), mutation != null ? Collections.singletonList(mutation) : null,
                        Collections.singletonList(mutationSynonyms), drugs, null, cancerTypes, null, null, null, null);
              /*  for (SolrDocument doc : docs) {
                    Collection<String> fieldNames = doc.getFieldNames();
                    String id = "";
                    if (fieldNames.contains("pmid_supporting")) {
                        List<String> pmid_supporting = (List<String>)doc.get("pmid_supporting");
                        List<String> text = (List<String>)doc.get("text");
                        if (text == null) {
                            log.error("No Text??? {}", pmid_supporting.get(0));

                        }
                        pmIdText.put(pmid_supporting.get(0), text.get(0));
                    }
                    else if (fieldNames.contains("pmid")) {
                        List<Long> pmidField = (List<Long>)doc.get("pmid");
                        List<String> text = (List<String>)doc.get("text");
                        if (text == null) {
                            log.error("No Text??? {}", pmidField.get(0));
                            pmIdText.put()
                        } else pmIdText.put(pmidField.get(0)+"", text.get(0));
                    }
                    else {
                        log.error("Totally missing PMID field!!");
                    }
                } */
                pmIdsRaw = result.getPmIds();
                supportingText.clear();
                pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
                    if (s.contains("S")) {
                        if (supportingText.containsKey(s.substring(0, s.indexOf("S"))))
                            supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S"))) + "," + s);
                        else supportingText.put(s.substring(0, s.indexOf("S")), s);

                        return s.substring(0, s.indexOf("S"));                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }
                    return s;
                }).collect(Collectors.toList());
                pmIds = new ArrayList<>(new LinkedHashSet<>(pmIdsWithDuplicates));
            }
            Row row1 = firstSheet.createRow(count[0]);
            Cell cell1 = row1.createCell(0);
            cell1.setCellValue(gene);
            cell1 = row1.createCell(1);
            cell1.setCellValue(mutation);
            if (v.getCancerTypes() != null && v.getCancerTypes().length() > 0) {
                cell1 = row1.createCell(2);
                cell1.setCellValue(v.getCancerTypes());
            }
            if (v.getDrugs() != null && v.getDrugs().length() > 0) {
                cell1 = row1.createCell(3);
                cell1.setCellValue(v.getDrugs());
            }
            if (v.getCuratedPMIds() != null && v.getCuratedPMIds().length() > 0) {
                cell1 = row1.createCell(5);
                cell1.setCellValue(v.getCuratedPMIds());
            }
            cell1 = row1.createCell(6);   // DISCOVERED PMIDs!!
            cell1.setCellValue("");
            for (String p : pmIds) {
                if (cell1.getStringCellValue().equals("")) cell1.setCellValue(p);
                else cell1.setCellValue(cell1.getStringCellValue()+", " + p);
            }
            Cell cell2 = row1.createCell(7);   // DOCUMENTS AND SUPPORTING URLs (links)
            cell2.setCellValue("");
            Cell cell3 = row1.createCell(8);   // PAGES OF EACH DOCUMENT AND COUNTS!
            cell3.setCellValue("");
            if (containsWildcard && mutation.contains("*")) {
                mutation.replace("*", "");
                if (mutationSynonyms.size() > 0) mutationSynonyms.set(0, mutationSynonyms.get(0).replace("*", ""));
            }
            String mutation_term = mutation;  // effectively final mutation variable
            Map<String, Integer> pmIdPageCounts = new HashMap<>(); // pageCount for each PMID
            for (String s: pmIds) {
                if (supportingText.containsKey(s)) {
                    List<String> supporting = new ArrayList<>();
                    if (supportingText.get(s).contains(",")) {
                        String[] docs = supportingText.get(s).split(",");
                        supporting.addAll(Arrays.asList(docs));
                    } else supporting.add(supportingText.get(s));
                    for (String supplement: supporting) {
                        if (cell2.getStringCellValue().equals("")) {
                            cell2.setCellValue("https://aimlcoe.mskcc.org/knowledge/getPDF/" + supplement + ".pdf?terms=" + URLEncoder.encode(mutation == null ? gene : mutation, Charset.defaultCharset()));
                            v.setArticleURLs("https://aimlcoe.mskcc.org/knowledge/getPDF/" + supplement + ".pdf?terms=" + URLEncoder.encode(mutation == null ? gene : mutation, Charset.defaultCharset()));
                        }
                        else {
                            cell2.setCellValue(cell2.getStringCellValue() + " " + "https://aimlcoe.mskcc.org/knowledge/getPDF/" + supplement + ".pdf?terms=" + URLEncoder.encode(mutation == null ? gene : mutation, Charset.defaultCharset()));
                            v.setArticleURLs(v.getArticleURLs() + " " + "https://aimlcoe.mskcc.org/knowledge/getPDF/" + supplement + ".pdf?terms=" + URLEncoder.encode(mutation == null ? gene : mutation, Charset.defaultCharset()));
                        }
                        textService.setupPageNumbers(supplement, mutation == null ? gene : mutation);
                        //Map<String, Map<Integer, Integer>> termMap = PDFEditorUtil.countPagesPerTerm(fullTextService.getSupportingPDF(supplement), Collections.singletonList(mutation));
                        Map<Integer, Integer> pageCounts = textService.getPageCounts();//termMap.get(mutation);
                        if (mutation != null && mutationSynonyms != null && mutationSynonyms.size() == 1) {
                            Map<Integer, Integer> pageCounts2 = null;
                            textService.setupPageNumbers(supplement, mutationSynonyms.get(0));
                            pageCounts2 = textService.getPageCounts();
                            pageCounts2.forEach((key, value) -> pageCounts.merge(key, value, Integer::sum));
                        } else if (mutationSynonyms != null && mutationSynonyms.size() > 1) {  // new 3/9/22
                            Map<Integer, Integer> pageCounts2 = null;
                            for (String mutationSyn : mutationSynonyms) {
                                textService.setupPageNumbers(supplement, mutationSyn);
                                pageCounts2 = textService.getPageCounts();
                                pageCounts2.forEach((key, value) -> pageCounts.merge(key, value, Integer::sum));
                            }
                        }
                        //pageCounts.values().removeAll(Collections.singletonList(0));
                        final StringBuilder sb = new StringBuilder();
                        pageCounts.keySet().forEach(key -> {
                            if (pageCounts.get(key) > 0) {
                                if (sb.length() == 0)
                                    sb.append("Mutation:" + mutation_term + " -> " + supplement + " Page " + (key+1) + ":" + pageCounts.get(key) + " mentions");
                                else
                                    sb.append(", " + supplement + " Page " + (key+1) + ":" + pageCounts.get(key) + " mentions");
                            }
                        });
                        if (cell3.getStringCellValue().equals("")) {
                            cell3.setCellValue(sb.length() > 32750 ? sb.substring(0,32750) : sb.toString());
                            v.setArticlePages(sb.toString());
                        }
                        else {
                            int len = cell3.getStringCellValue().length();
                            int remaining = 32750 - len;
                            if (remaining > sb.length()) cell3.setCellValue(cell3.getStringCellValue() + " " + sb);
                            else if (remaining > 20) cell3.setCellValue(cell3.getStringCellValue() + " " + sb.substring(0, remaining));
                            v.setArticlePages(v.getArticlePages()+" "+sb);
                        }
                    }
                }
                if (fullTextRepository.existsById(s)) {
                    //Map<String, Map<Integer, Integer>> termMap = PDFEditorUtil.countPagesPerTerm(fullTextService.getMainPDF(s), Collections.singletonList(mutation));
                    textService.setupPageNumbers(s, mutation == null ? gene: mutation);
                    final Map<Integer, Integer> pageCounts = textService.getPageCounts();//termMap.get(mutation);
                    if (mutation != null && mutationSynonyms != null && mutationSynonyms.size() == 1) {
                        Map<Integer, Integer> pageCounts2 = null;
                        textService.setupPageNumbers(s, mutationSynonyms.get(0));
                        pageCounts2 = textService.getPageCounts();
                        pageCounts2.forEach((key, value) -> pageCounts.merge(key, value, Integer::sum));
                    } else if (mutationSynonyms != null && mutationSynonyms.size() > 1) {
                        Map<Integer, Integer> pageCounts2 = null;
                        for (String mutationSyn: mutationSynonyms) {
                            textService.setupPageNumbers(s, mutationSyn);
                            pageCounts2 = textService.getPageCounts();
                            pageCounts2.forEach((key, value) -> pageCounts.merge(key, value, Integer::sum));
                        }
                    }
                    int total = pageCounts.values().parallelStream().reduce(0, Integer::sum);
                    pmIdPageCounts.put(s, total);
                   // if (textService.validateText(pmIdText.get(s), gene, mutation, total)) {
                        //pageCounts.values().removeAll(Collections.singletonList(0));
                    if (pageCounts.keySet().size() > 0) {
                        if (cell2.getStringCellValue().equals(""))
                            cell2.setCellValue("https://aimlcoe.mskcc.org/knowledge/getPDF/" + s + ".pdf?terms=" + URLEncoder.encode(mutation == null? gene: mutation, Charset.defaultCharset()));
                        else
                            cell2.setCellValue(cell2.getStringCellValue() + " " + "https://aimlcoe.mskcc.org/knowledge/getPDF/" + s + ".pdf?terms=" + URLEncoder.encode(mutation == null? gene: mutation, Charset.defaultCharset()));
                        final StringBuilder sb = new StringBuilder();
                        pageCounts.keySet().forEach(key -> {
                            if (pageCounts.get(key) > 0) {
                                if (sb.length() == 0)
                                    sb.append("Mutation:" + (mutation_term == null ? gene:mutation_term) + " -> " + s + " Page " + (key + 1) + ":" + pageCounts.get(key) + " mentions");
                                else
                                    sb.append(", " + s + " Page " + (key + 1) + ":" + pageCounts.get(key) + " mentions");
                            }
                        });
                        // POI Limitation of maximum cell size is 32767 characters
                        if (cell3.getStringCellValue().equals("")) {
                            cell3.setCellValue(sb.length() > 32750 ? sb.substring(0,32750) : sb.toString());
                        }
                        else {
                            int len = cell3.getStringCellValue().length();
                            int remaining = 32750 - len;
                            if (remaining > sb.length()) cell3.setCellValue(cell3.getStringCellValue() + " " + sb);
                                  else if (remaining > 20) cell3.setCellValue(cell3.getStringCellValue() + " " + sb.substring(0, remaining));
                        }
                        v.setArticlePages(sb.toString());
                    }
                }

            }
            v.setAutomatedPMIds(cell1.getStringCellValue());   // Potentially all Tier "2" articles
            v.setArticleURLs(cell2.getStringCellValue());
            //v.setArticlePages(cell3.getStringCellValue());
            v.setLastUpdate(DateTime.now());
            String[] automatedPMIDs = cell1.getStringCellValue().split(", ");
            String resultC = "";
            for (String value : automatedPMIDs) {  // contained in both URLs and in the page lists
                if (cell2.getStringCellValue().contains(value) && cell3.getStringCellValue().contains(value)) {
                    if (resultC.equals("")) resultC = value;
                    else resultC += ", " + value;
                }  // created the final set
            }
            v.setConsensusPMIds(resultC);
            cell1 = row1.createCell(9);
            cell1.setCellValue(resultC);
            assignTiers(v, pmIdPageCounts, geneSynonyms);
            scoreKeywords(v, pl, false, true);
            variantService.rescoreArticles(v);
            variantService.rescoreArticlesHongxin(v);


            cell1 = row1.createCell(10);
            resultC = "";
            if (v.getArticlesTier1() != null && v.getArticlesTier1().size() > 0) for (String value : v.getArticlesTier1()) {
                if (resultC.equals("")) resultC = value;
                else resultC += ", " + value;
            }
            cell1.setCellValue(resultC);

            cell1 = row1.createCell(11);
            resultC = "";
            for (String value : v.getArticlesTier2()) {
                if (resultC.equals("")) resultC = value;
                else resultC += ", " + value;
            }
            cell1.setCellValue(resultC);
            cell1 = row1.createCell(12);
            resultC = "";
            if (v.getScoreCode1() != null) for (String value: v.getScoreCode1()) {
                if (resultC.equals("")) resultC = value;
                else resultC += ", " + value;
            }
            cell1.setCellValue(resultC);
            cell1 = row1.createCell(13);
            resultC = "";
            if (v.getScoreCode2() != null) for (String value: v.getScoreCode2()) {
                if (resultC.equals("")) resultC = value;
                else resultC += ", " + value;
            }
            cell1.setCellValue(resultC);

            variantRepository.save(v);
            count[0]++;

            if (threaded && stop.equals(threadId)) {
                stop = "";
                return null;
            }

        }
        for (int i = 0; i < 14; i++) firstSheet.autoSizeColumn(i);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            wb.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @AllArgsConstructor
    @Getter
    static class Item {
        private String id;
        private Integer score;
    }

    public void assignTiers(Variant v, Map<String, Integer> pageCounts, List<String> geneSynonyms) {
        log.info("Variant ID {}", v.getId());
        if ((v.getAutomatedPMIds() == null || v.getAutomatedPMIds().length() == 0) && (v.getConsensusPMIds() == null || v.getConsensusPMIds().length() == 0)) {
            log.info("No articles for variant {}:{}", v.getGene(), v.getMutation());
            v.setArticlesTier1(new ArrayList<>());
            v.setArticlesTier2(new ArrayList<>());
            v.setTotal(0);
            variantRepository.save(v);
            return;
        }
        if (v.getMutation() == null || v.getMutation().equals("")) return;
        List<String> h;
        if (v.getConsensusPMIds() != null && v.getConsensusPMIds().length() > 0) h = Arrays.asList(v.getConsensusPMIds().split(", "));
        else h = new ArrayList<>();
        List<String> l;
        if (v.getAutomatedPMIds() != null && v.getAutomatedPMIds().length() > 0) l = Arrays.asList(v.getAutomatedPMIds().split(", "));
        else l = new ArrayList<>();
        List<String> highArticles = new ArrayList<>(h);
        List<String> lowArticles = new ArrayList<>(l);
        String gene = v.getGene();
        if (gene == null) {
            log.error("No gene name provided for variant " + v.getId());
            return;
        }
        String mutation = v.getMutation();
        if (mutation == null) mutation = gene;
        if (pageCounts == null) {
            pageCounts = new HashMap<>();
            String[] PMIDs = v.getAutomatedPMIds().split(", ");
            for (String s : PMIDs) {
                if (!fullTextRepository.existsById(s)) {
                    log.info("Checking for article for PMID {}", s);
                    fullTextService.addArticle(s);
                }
                if (fullTextRepository.existsById(s)) {
                    textService.setupPageNumbers(s, mutation);
                    final Map<Integer, Integer> pc = textService.getPageCounts();
                    int total = pc.values().parallelStream().reduce(0, Integer::sum);
                    pageCounts.put(s, total);
                }
            }
        }
        if (geneSynonyms == null) {
            List<Target> targets = targetRepository.findAllBySymbol(gene);
            if (targets.size() > 0 && targets.get(0).getSynonyms() != null) geneSynonyms = Arrays.asList(targets.get(0).getSynonyms().split(";"));
        }
        String[] urls = v.getArticleURLs().split(" ");
        Map<String, Integer> scores = new HashMap<>();
        Map<String, String> scoreCodes = new HashMap<>();
        List<String> geneNames = new ArrayList<>();
        geneNames.add(gene); if (geneSynonyms != null) geneNames.addAll(geneSynonyms);
        final String mutationSynonym = AminoAcids.mutationSynonym(mutation);
        List<String> mutationSynonyms = null;
        if (AminoAcids.hotspotSubstitution(mutation) != null) mutationSynonyms = AminoAcids.hotspotSubstitution(mutation);
        if (mutation.contains(" Fusion")) mutation = mutation.substring(0, mutation.indexOf(" Fusion"));
        log.info("Scoring articles for geneNames {} and mutation {} with mutationSynonym {}", geneNames, mutation, mutationSynonym);
        for (String s : highArticles) {
            String text = solrService.getText(solrService.findArticle(s));
            String snippet = text;
            int refpos = textService.getReferencesPosition(snippet);
            if ((refpos != -1 && snippet.indexOf(mutation) > refpos) || !snippet.contains(mutation)) {
                if (mutationSynonym == null || ((refpos != -1 && snippet.indexOf(mutationSynonym) > refpos) || !snippet.contains(mutationSynonym))) {
                    boolean found = false;
                    if (mutationSynonyms != null) {
                        for (String mut: mutationSynonyms) {
                            if ((refpos == -1 || snippet.indexOf(mut) <= refpos) && snippet.contains(mut)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        log.info("Skipping high article {} with refpos {} and length {}, no mentions of mutation {}", s, refpos, snippet.length(), mutation);
                        lowArticles.remove(s);
                        continue;
                    }
                }
            }
            int pCount = pageCounts.getOrDefault(s, 1);  // DEFAULT VALUE 1 due to the check
            if (!mutation.toUpperCase().contains(gene.toUpperCase()) && !textService.validateText(text, gene, geneSynonyms, mutation, pCount)) {
                log.info("Skipping high article {} with refpos {} and length {}, seems to be another gene (validateText())", s, refpos, snippet.length());
                lowArticles.remove(s);
                continue;
            }
            scores.put(s, 2*pCount);
            if (pCount > 0) scoreCodes.put(s, "A"+pCount);
            if (snippet.contains(" {!fulltext} ")) snippet = text.substring(0, text.indexOf(" {!fulltext} "));
            int missed = 0;
            int total = 0;
            for (String g : geneNames) {
                Pattern p = Pattern.compile("\\b"+g+"\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(snippet);
                boolean found = false;
                while (m.find()) {
                    found = true;
                    if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                        scores.put(s, scores.get(s) + 4);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "B");
                    } else {
                        scores.put(s, scores.get(s) + 2);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "C");
                    }
                }
                if (!found) missed++;
                total++;
            }
            if (missed == total) {
                scores.put(s, scores.get(s)-4);
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "D");
            }
            missed = 0;

            Pattern p = Pattern.compile("\\b"+mutation+"\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(snippet);
            boolean found = false;
            while (m.find()) {
                found = true;
                if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                    scores.put(s, scores.get(s) + 8);
                    scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                } else {
                    scores.put(s, scores.get(s) + 4);
                    scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                }
            }
            if (!found) missed++;

            if (mutationSynonym != null) {
                p = Pattern.compile("\\b"+mutationSynonym+"\\b", Pattern.CASE_INSENSITIVE);
                m = p.matcher(snippet);
                found = false;
                while (m.find()) {
                    found = true;
                    if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                        if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("E")) {
                            scores.put(s, scores.get(s) + 8);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                        }
                    } else if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("F")) {
                        scores.put(s, scores.get(s) + 4);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                    }
                }
                if (!found) missed++;
            }
            if (mutationSynonyms != null) {
                missed--; // because now missed technically has to be 3
                found = false;
                for (String mut: mutationSynonyms) {
                    p = Pattern.compile("\\b"+mut+"\\b", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(snippet);
                    while (m.find()) {
                        found = true;
                        if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                            if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("E")) {
                                scores.put(s, scores.get(s) + 8);
                                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                            }
                        } else if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("F")) {
                            scores.put(s, scores.get(s) + 4);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                        }
                    }
                }
                if (!found) missed++;
            }
            if (missed == 2) {
                scores.put(s, scores.get(s)-4);
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "G");
            }
            Set<String> doc = new HashSet<>();
            for (String url: urls) {
                if (url.contains(s)) {
                    String pmIdPortion = url.substring(url.indexOf(s), url.lastIndexOf("."));
                    if (!pmIdPortion.equals(s) && !doc.contains(pmIdPortion) && pmIdPortion.contains("S")) {
                        doc.add(pmIdPortion);
                        lowArticles.remove(pmIdPortion);
                        // supporting doc
                        log.info("Supporting text {} found for variant with gene {} and mutation {}", pmIdPortion, gene, mutation);
                        int references = pageCounts.getOrDefault(pmIdPortion, 0);
                        if (pageCounts.containsKey(pmIdPortion)) {
                            scores.put(s, scores.get(s) + 2*references);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "H" + references);
                        }
                    }
                    else doc.add(pmIdPortion);
                    log.info("PMID {} found", pmIdPortion);
                }
            }
            if (doc.size() > 1) {
                scores.put(s, scores.get(s)+2*(doc.size()-1));
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "I" + (doc.size()-1));
            }
            lowArticles.remove(s);
        }
        // remove any remaining supporting document missed in the set of automated PMIDs
        List<String> supporting = new ArrayList<>();
        for (int i = 0; i < lowArticles.size(); i++) {
            if (lowArticles.get(i).contains("S")) {
                supporting.add(lowArticles.get(i));
                lowArticles.remove(i--);
            }
        }
        log.info("{} remaining unassigned supporting information documents", supporting.size());
        // see what can be scored from the 'crummy' articles
        for (int i = 0; i < lowArticles.size(); i++) {
            String s = lowArticles.get(i);
            String text = solrService.getText(solrService.findArticle(s));
            String snippet = text;
            int refpos = textService.getReferencesPosition(snippet);
            if ((refpos != -1 && snippet.indexOf(mutation) > refpos) || !snippet.contains(mutation)) {
                if (mutationSynonym == null || ((refpos != -1 && snippet.indexOf(mutationSynonym) > refpos) || !snippet.contains(mutationSynonym))) {
                    boolean found = false;
                    if (mutationSynonyms != null) {
                        for (String mut: mutationSynonyms) {
                            if ((refpos == -1 || snippet.indexOf(mut) <= refpos) && snippet.contains(mut)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        log.info("Skipping high article {} with refpos {} and length {}, no mentions of mutation {}", s, refpos, snippet.length(), mutation);
                        lowArticles.remove(s);
                        continue;
                    }
                }
            }
            int pCount = pageCounts.getOrDefault(s, 1);
            if (!mutation.toUpperCase().contains(gene.toUpperCase()) && !textService.validateText(text, gene, geneSynonyms, mutation, pCount)) {
                log.info("Skipping low article {}, seems to be another gene (validateText())", s);
                lowArticles.remove(i--);
                continue;
            }
            scores.put(s, 2*pCount);
            if (pCount > 0) scoreCodes.put(s, "A"+pCount);
            if (snippet.contains(" {!fulltext} ")) snippet = text.substring(0, text.indexOf(" {!fulltext} "));
            int missed = 0;
            int total = 0;
            for (String g : geneNames) {
                Pattern p = Pattern.compile("\\b"+g+"\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(snippet);
                boolean found = false;
                while (m.find()) {
                    found = true;
                    if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                        scores.put(s, scores.get(s) + 4);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "B");
                    } else {
                        scores.put(s, scores.get(s) + 2);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "C");
                    }
                }
                if (!found) missed++;
                total++;
            }
            if (missed == total) {
                scores.put(s, scores.get(s)-4);
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "D");
            }
            missed = 0;
            Pattern p = Pattern.compile("\\b"+mutation+"\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(snippet);
            boolean found = false;
            while (m.find()) {
                found = true;
                if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                    scores.put(s, scores.get(s) + 8);
                    scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                } else {
                    scores.put(s, scores.get(s) + 4);
                    scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                }
            }
            if (!found) missed++;
            if (mutationSynonym != null && mutationSynonym.length() > 0) {
                p = Pattern.compile("\\b"+mutationSynonym+"\\b", Pattern.CASE_INSENSITIVE);
                m = p.matcher(snippet);
                found = false;
                while (m.find()) {
                    found = true;
                    if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                        if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("E")) {
                            scores.put(s, scores.get(s) + 8);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                        }
                    } else if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("F")) {
                        scores.put(s, scores.get(s) + 4);
                        scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                    }
                }
                if (!found) missed++;
            }
            if (mutationSynonyms != null) {
                missed--; // because now missed technically has to be 3
                found = false;
                for (String mut: mutationSynonyms) {
                    p = Pattern.compile("\\b"+mut+"\\b", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(snippet);
                    while (m.find()) {
                        found = true;
                        if (snippet.substring(m.end()-1).contains(" {!abstract} ")) {
                            if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("E")) {
                                scores.put(s, scores.get(s) + 8);
                                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "E");
                            }
                        } else if (scoreCodes.get(s) == null || !scoreCodes.get(s).contains("F")) {
                            scores.put(s, scores.get(s) + 4);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "F");
                        }
                    }
                }
                if (!found) missed++;
            }
            if (missed == 2) {
                scores.put(s, scores.get(s)-4);
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "G");
            }
            Set<String> doc = new HashSet<>();
            for (String url: urls) {
                if (url.contains(s)) {
                    String pmIdPortion = url.substring(url.indexOf(s), url.lastIndexOf("."));
                    if (!pmIdPortion.equals(s) && !doc.contains(pmIdPortion) && pmIdPortion.contains("S")) {
                        doc.add(pmIdPortion);
                        supporting.remove(pmIdPortion);
                        // supporting doc
                        int references = pageCounts.getOrDefault(pmIdPortion, 0);
                        if (pageCounts.containsKey(pmIdPortion)) {
                            scores.put(s, scores.get(s) + 2 * references);
                            scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "H" + references);
                        }
                    }
                    else doc.add(pmIdPortion);
                    log.info("PMID {} found", pmIdPortion);
                }
            }
            if (doc.size() > 1) {
                scores.put(s, scores.get(s)+2*(doc.size()-1));
                scoreCodes.put(s, (scoreCodes.get(s) != null ? scoreCodes.get(s) : "") + "I" + (doc.size()-1));
            }
            lowArticles.remove(i--); // I think we need this?
        }

        List<String> sortedPMID = scores.keySet().stream().map(s -> new Item(s, scores.get(s))).sorted(Comparator.comparingInt(Item::getScore).reversed()).map(a -> a.id).collect(Collectors.toList());
        List<String> tier1 = new ArrayList<>();
        List<String> tier2 = new ArrayList<>();
        List<Integer> scores1 = new ArrayList<>();
        List<Integer> scores2 = new ArrayList<>();
        List<String> scoreCodes1 = new ArrayList<>();
        List<String> scoreCodes2 = new ArrayList<>();
        if (sortedPMID.size() <= 5) {
            tier1.addAll(sortedPMID);
            for (String p : tier1) {
                scores1.add(scores.getOrDefault(p, 0));
                scoreCodes1.add(scoreCodes.getOrDefault(p, ""));
            }
            //sortedPMID.clear();   <--- WHY
            while (tier1.size() < 5 && lowArticles.size() > 0) {
                String value = lowArticles.get(0);
                tier1.add(lowArticles.remove(0));
                scores1.add(scores.getOrDefault(value, 0));
                scoreCodes1.add(scoreCodes.getOrDefault(value, ""));
            }
        } else {
            for (int i = 0; i < 5; i++) {
                String value = sortedPMID.get(0);
                lowArticles.remove(value);
                tier1.add(sortedPMID.remove(0));
                scores1.add(scores.getOrDefault(value, 0));
                scoreCodes1.add(scoreCodes.getOrDefault(value, ""));
            }
            int remainingLength = sortedPMID.size();
            for (int i = 0; i < remainingLength; i++) {
                String value = sortedPMID.get(0);
                lowArticles.remove(value);
                tier2.add(sortedPMID.remove(0));
                scores2.add(scores.getOrDefault(value, 0));
                scoreCodes2.add(scoreCodes.getOrDefault(value, ""));
            }
        }
        while (lowArticles.size() > 0) {
            String value = lowArticles.get(0);
            tier2.add(lowArticles.remove(0));
            scores2.add(scores.getOrDefault(value, 0));
            scoreCodes2.add(scoreCodes.getOrDefault(value, ""));
        }

        for (int i = 0; i < scoreCodes1.size(); i++) {
            scoreCodes1.set(i, processCodes(scoreCodes1.get(i)));
        }
        for (int i = 0; i < scoreCodes2.size(); i++) {
            scoreCodes2.set(i, processCodes(scoreCodes2.get(i)));
        }

        v.setArticlesTier1(tier1);
        v.setArticlesTier2(tier2);
        v.setScores1(scores1);
        v.setScores2(scores2);
        v.setTotal(tier1.size()+tier2.size());
        v.setScoreCode1(scoreCodes1);
        v.setScoreCode2(scoreCodes2);
        //variantRepository.save(v); // [saved in processVariants]

    }

    public void assignTiers(Variant v) {
        assignTiers(v, null, null);
    }


    public String processCodes(String code) {
        int[] counts = new int[6];
        //String originalCode = code;
        for (char c = 'B'; c <= 'G'; c++) {
            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == c) {
                    counts[c-'B']++;
                    code = code.substring(0, i) + (i+1 < code.length() ? code.substring(i+1) : "");
                    i--;
                }
            }
        }

        String reduced = code;
        for (int i = 0; i < 6; i++) {
            char letter = (char)('B' + i);
            if (counts[i] > 0) {
                reduced += "" + letter + (counts[i] > 1 ? counts[i] : "");
            }
        }
        return reduced;
    }


    public void scoreKeywords(Variant variant, List<ProjectList> pl, boolean useSupporting, boolean useCount) {
        if (variant.getArticlesTier1() == null || variant.getArticlesTier1().size() == 0) {
            log.info("Skipping keywords for {}, no articles exist.", variant.getDescriptor());
            variant.setKeywordScores(new ArrayList<>());
            variantRepository.save(variant);
            return;
        }
        log.info("Scoring keywords for {}", variant.getDescriptor());

        Set<String> articleList = new LinkedHashSet<>(variant.getArticlesTier1());
        articleList.addAll(variant.getArticlesTier2());
        StringBuilder sb = new StringBuilder(12*articleList.size());
        sb.append("pmid:(");
        boolean first = true;
        for (String pmid: articleList) {
            if (first) {
                first = false;
                if (pmid.contains("[excluded] ")) pmid = pmid.substring("[excluded] ".length());
                sb.append(pmid);
            } else {
                if (pmid.contains("[excluded] ")) pmid = pmid.substring("[excluded] ".length());
                sb.append(" ").append(pmid);
            }
        }
        sb.append(")");
        if (!useSupporting) sb.append(" AND -pmid_supporting:*");
        String fq = sb.toString();
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String article: articleList) scores.put(article, 0);
        assert articleList.size() == variant.getArticlesTier1().size()+variant.getArticlesTier2().size();

        //log.info("fq = {}", fq);
        for (ProjectList p : pl) {
            if (p.getSynonyms() == null || p.getSynonyms().size() == 0) continue;
            for (String keyword: p.getSynonyms()) {
                keyword = quote(keyword);
                SolrClientTool.ResultMap resultMap;
                solrService.getSolrClientTool().setDefaultField("text");
                try {
                    resultMap = solrService.getSolrClientTool().queryCount("knowledge", keyword, fq);
                } catch (IOException | SolrServerException e) {
                    log.error("AN ERROR OCCURRED WITH queryCount() = {}", e.getMessage());
                    e.printStackTrace();
                    return;
                }
                DocumentObjectBinder binder = new DocumentObjectBinder();
                List<SolrItem> items = binder.getBeans(SolrItem.class, resultMap.getDocs());

                for (SolrItem item : items) {
                    String pmid = item.getPmid().get(0) + "";
                    String id = item.getId();
                    if (scores.get(pmid) != null) {
                        if (useCount)
                            scores.put(pmid, scores.get(pmid) + getAfter(resultMap.getExplainMap().get(id), "freq="));
                        else scores.put(pmid, scores.get(pmid) + 1);
                    }
                }
            }
        }
        List<Integer> keywordScores = new ArrayList<>();
        assert scores.keySet().size() == variant.getArticlesTier1().size()+variant.getArticlesTier2().size();
        for (String key: scores.keySet()) {
            log.info("{} {}", key, scores.get(key));
            keywordScores.add(scores.get(key));
        }
        variant.setKeywordScores(keywordScores);
        variantRepository.save(variant);
    }

    private int getAfter(Object mapValue, String phrase) {
        String input = mapValue.toString();
        int index = input.indexOf(phrase)+phrase.length();
        int index2;
        if (input.substring(index).contains(")")) index2 = index + input.substring(index).indexOf(")");
        else index2 = index + input.substring(index).indexOf(" ");
        double d = Double.parseDouble(input.substring(index, index2));
        return (int)d;
    }

    public void scoreKeywords(List<Variant> variants, List<ProjectList> pl, boolean useSupporting, boolean useCount) {
        variants.parallelStream().forEach(v -> {
            //log.info("Scoring keywords for {}", v.getDescriptor());
            scoreKeywords(v, pl, useSupporting, useCount);
        });
    }



    public Set<String> missingFullTextArticles(String filename) {
        Workbook wb;
        Sheet firstSheet;
        Set<String> articleSet = new HashSet<>();
        Set<String> containedSet = new HashSet<>();
        try (FileInputStream inputStream = new FileInputStream(filename);) {
            wb = new XSSFWorkbook(inputStream);
            firstSheet = wb.getSheetAt(0);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        for (int i = 1; i <= firstSheet.getLastRowNum(); i++) {
            Row row = firstSheet.getRow(i);
            Cell cell = row.getCell(5);
            if (cell.getCellType() == CellType.NUMERIC) {
                String pmid = (int)(cell.getNumericCellValue())+"";
                Article a = articleRepository.findByPmId(pmid);
                if (a == null) {
                    log.info("An unknown item {}", pmid);
                    articleSet.add(pmid);
                } else if (a.getFulltext() == null) articleSet.add(pmid);
                else containedSet.add(pmid);
            }
            else {
                String[] pmidsmanual = cell.getStringCellValue().trim().split(", ");
                for (String pmid: pmidsmanual) {
                    Article a = articleRepository.findByPmId(pmid);
                    if (a == null) {
                        log.info("An unknown item {}", pmid);
                        articleSet.add(pmid);
                    } else if (a.getFulltext() == null) articleSet.add(pmid);
                    else containedSet.add(pmid);
                }
            }
        }
        String line;
        try (BufferedReader TSVReader = new BufferedReader(new FileReader("allAnnotatedVariants.txt"))) {
            int i = 0;
            while ((line = TSVReader.readLine()) != null) {
                //System.out.println(i++);
                String[] items = line.split("\t");
                if (items.length > 11) {
                    String[] pmids = items[11].split(", ");
                    for (String pmid : pmids) {
                        Article a = articleRepository.findByPmId(pmid);
                        if (a == null) {
                            log.info("An unknown item {}", pmid);
                            articleSet.add(pmid);
                        } else if (a.getFulltext() == null) articleSet.add(pmid);
                        else containedSet.add(pmid);
                    }
                }
                i++;
            }
        } catch (IOException e) {
            log.error("Error reading the items from allAnnotatedVariants");
        }
        log.info("Contained articles {}", containedSet);
        return articleSet;
    }

    public static List<String> getOncogenicMutations(String gene, Map<String, List<ArticleQuery.WordPair>> map) {
        gene = gene.toUpperCase();
        List<String> result = new ArrayList<>();
        for (ArticleQuery.WordPair pair: map.get(gene)) {
            if (pair.word2.equalsIgnoreCase("oncogenic")) result.add(pair.word1.toLowerCase());
        }
        return result;
    }

    public static String removeSupportingTag(String pmid) {
        if (pmid.contains("S")) return pmid.substring(0, pmid.indexOf("S"));
        return pmid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setThread(String id) {
        this.thread = id;
    }

    @Override
    public void setThreadStop(String id) {
        this.stop = id;
    }
}
