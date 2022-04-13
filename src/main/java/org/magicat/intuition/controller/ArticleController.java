package org.mskcc.knowledge.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.mskcc.knowledge.model.*;
import org.mskcc.knowledge.repository.*;
import org.mskcc.knowledge.repository.FullTextRepository;
import org.mskcc.knowledge.service.*;
import org.mskcc.knowledge.util.AminoAcids;
import org.mskcc.knowledge.pdf.PDFHighlighter;
import org.mskcc.knowledge.util.SolrClientTool;
import org.mskcc.knowledge.util.TikaTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CrossOrigin(originPatterns = {"**"})
@Api("Utilities for getting full-text articles and other resources")
@RestController
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);
    private static final Map<User, Map<String, Object>> userCache = new ConcurrentHashMap<>();

    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final org.mskcc.knowledge.service.FullTextService fullTextService;
    private final FullTextRepository fullTextRepository;
    private final TargetRepository targetRepository;
    private final GridFsTemplate gridFsTemplate;
    //private final GeneMapRepository geneMapRepository;
    //private final MutationMapRepository mutationMapRepository;
    //private final DrugMapRepository drugMapRepository;
    //private final CancerMapRepository cancerMapRepository;
    private final AnalyticsService analyticsService;
    private final SolrService solrService;
    private final TextService textService;
    private final UserRepository userRepository;
    private final ProjectListRepository projectListRepository;

    private String threadStop = "";

    @Autowired
    public ArticleController(TargetRepository targetRepository, ArticleRepository articleRepository, ArticleService articleService, org.mskcc.knowledge.service.FullTextService fullTextService, TextService textService,
                             AnalyticsService analyticsService, SolrService solrService, UserRepository userRepository, /*GeneMapRepository geneMapRepository, CancerMapRepository cancerMapRepository,
                             MutationMapRepository mutationMapRepository, DrugMapRepository drugMapRepository,*/ FullTextRepository fullTextRepository, GridFsTemplate gridFsTemplate, ProjectListRepository projectListRepository) {
        this.articleService = articleService;
        this.articleRepository = articleRepository;
        this.fullTextService = fullTextService;
        this.fullTextRepository = fullTextRepository;
        this.targetRepository = targetRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.userRepository = userRepository;
        //this.geneMapRepository = geneMapRepository;
        //this.mutationMapRepository = mutationMapRepository;
        //this.drugMapRepository = drugMapRepository;
        //this.cancerMapRepository = cancerMapRepository;
        this.analyticsService = analyticsService;
        this.solrService = solrService;
        this.textService = textService;
        this.projectListRepository = projectListRepository;
    }


    @ApiOperation(value = "Write a spreadsheet for all use data about the variants")
    @RequestMapping(value = "/createSpreadsheet/{name}.xlsx", method = RequestMethod.GET, produces = {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})
    public byte[] createSpreadsheet(@PathVariable String name, List<Variant> variants) {
        return analyticsService.processVariants(variants);
    }

    @ApiOperation(value = "Download and view the article/supporting text from a docx file")
    @RequestMapping(value = "/getDOCX/{pmid}.docx", method = RequestMethod.GET, produces = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"})
    public byte[] getDOCXArticle(@PathVariable String pmid) throws IOException {
        log.info("Redirected successfully to DOCX controller");
        Optional<FullText> of = fullTextRepository.findById(pmid);
        if (of.isEmpty()) return null;
        FullText f = of.get();
        String[] resources = f.getResourceIds();
        List<GridFSFile> gridFSFiles = new ArrayList<>();
        for (String resource : resources) {
            gridFSFiles.add(gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resource))));
        }
        List<GridFSFile> result = gridFSFiles.stream().filter(a -> a.getFilename().toLowerCase().endsWith(".doc") || a.getFilename().toLowerCase().endsWith(".docx"))
                .sorted(Comparator.comparingInt(a -> a.getFilename().length())).collect(Collectors.toList());
        if (result.size() > 0) return IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent());
        return null;
    }

    @ApiOperation(value = "Download and view the PMC article if available for the given PubMed ID, integrating text and SI into system")
    @RequestMapping(value = "/getPDFs/{pmid}.pdf", method = RequestMethod.GET, produces = {"application/pdf"})
    public byte[] getPDFArticle(@PathVariable String pmid, @RequestParam(required = false) String terms, HttpServletResponse response) throws IOException {
        Optional<FullText> of = fullTextRepository.findById(pmid);
        if (of.isPresent()) {
            FullText f = of.get();
            String[] resources = f.getResourceIds();
            List<GridFSFile> gridFSFiles = new ArrayList<>();
            for (String resource : resources) {
                gridFSFiles.add(gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resource))));
            }
            List<GridFSFile> result = gridFSFiles.stream().filter(a -> a.getFilename().endsWith(".pdf"))
                    .sorted(Comparator.comparingInt(a -> a.getFilename().length())).collect(Collectors.toList());
            if (result.size() == 0) {
                log.info("Attempting redirect to DOCX endpoint");
                response.sendRedirect("redirect:/getDOCX/" + pmid + ".docx");
                return null;
            }
            for (GridFSFile x : result) {
                log.info(x.getFilename());
            }
            if (terms == null || terms.length() == 0)
                return IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent());
            else {
                return new PDFHighlighter().highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Arrays.stream(terms.split(",")).map(String::toUpperCase).collect(Collectors.toList()));
            }
        }
        if (!pmid.contains("S") && articleService.addArticlePDF(pmid)) {
            Stream<Path> walk = Files.walk(Paths.get("PMC/" + pmid));
            List<String> result = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
                    .filter(f -> f.endsWith("pdf"))
                    .sorted(Comparator.comparingInt(String::length))
                    .collect(Collectors.toList());
            if (result.size() > 0) {
                File f = new File(result.get(0));
                if (f.exists()) {
                    new Thread(() -> fullTextService.addArticle(pmid)).start();
                    if (terms == null || terms.length() == 0)
                        return FileUtils.readFileToByteArray(f);
                    else
                        return new PDFHighlighter().highlight(FileUtils.readFileToByteArray(f), Arrays.stream(terms.split(",")).collect(Collectors.toList()));
                }
                return null;
            } else return null;
        } else {
            log.info("Not able to match PMID to full-text");
            return null;
        }
    }

    @ApiOperation(value = "Download and view the PMC article if available for the given PubMed ID, integrating text and SI into system")
    @RequestMapping(value = "/getPDF/{pmid}.pdf", method = RequestMethod.GET, produces = {"application/pdf"})
    public byte[] getProPDFArticle(@PathVariable String pmid, @RequestParam(required = false) String terms, HttpServletResponse response) throws IOException {
        Optional<FullText> of = fullTextRepository.findById(pmid);
        String[] colors = {"#FCE4B0", "#E7CEF3", "#D1EEF0", "#F87063", "#8DD08A", "#D7CCC8"};
        if (of.isPresent()) {
            FullText f = of.get();
            String[] resources = f.getResourceIds();
            List<GridFSFile> gridFSFiles = new ArrayList<>();
            for (String resource : resources) {
                gridFSFiles.add(gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resource))));
            }
            List<GridFSFile> result = gridFSFiles.stream().filter(a -> a.getFilename().endsWith(".pdf"))
                    .sorted(Comparator.comparingInt(a -> a.getFilename().length())).collect(Collectors.toList());
            if (result.size() == 0) {
                log.info("Attempting redirect to DOCX endpoint");
                response.sendRedirect("redirect:/getDOCX/" + pmid + ".docx");
                return null;
            }
            for (GridFSFile x : result) {
                log.info(x.getFilename());
            }
            if (terms == null || terms.length() == 0)
                return IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent());
            else {
                String[] items = terms.split(",");
                byte[] data = null;
                int ci = 0;
                for (String item: items) {
                    if (item.contains(":")) {
                        String color = "#" + item.split(":")[1];
                        colors[ci] = color;
                        item = item.substring(0, item.indexOf(":"));
                    }
                    if (item.equals("$KEYWORDS")) {
                        List<String> words = new ArrayList<>();
                        List<ProjectList> pitems = projectListRepository.findIdExpression("keywordListM");
                        for (ProjectList p : pitems) {
                            words.addAll(p.getSynonyms());
                        }
                        if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), words);
                        else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, words);
                    } else if (item.startsWith("$ALTERATION") && item.contains("-")) {
                        String term = item.substring(item.indexOf("-")+1);
                        String synonym = AminoAcids.mutationSynonym(term);
                        log.info("{} {}", term, synonym);
                        if (!term.equals(synonym)) {
                            if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Arrays.asList(term, synonym));
                            else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Arrays.asList(term, synonym));
                            List<String> hotspotSynonyms = AminoAcids.hotspotSubstitution(term);
                            if (hotspotSynonyms != null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, hotspotSynonyms);
                        } else {
                            if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Collections.singletonList(term));
                            else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Collections.singletonList(term));
                        }
                    } else {
                        if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Collections.singletonList(item));
                        else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Collections.singletonList(item));
                    }
                    ci++;
                }
                return data;
            }
        }
        if (!pmid.contains("S") && articleService.addArticlePDF(pmid)) {
            Stream<Path> walk = Files.walk(Paths.get("PMC/" + pmid));
            List<String> result = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
                    .filter(f -> f.endsWith("pdf"))
                    .sorted(Comparator.comparingInt(String::length))
                    .collect(Collectors.toList());
            if (result.size() > 0) {
                File f = new File(result.get(0));
                if (f.exists()) {
                    new Thread(() -> fullTextService.addArticle(pmid)).start();
                    if (terms == null || terms.length() == 0)
                        return FileUtils.readFileToByteArray(f);
                    else {
                        String[] items = terms.split(",");
                        byte[] data = null;
                        int ci = 0;
                        for (String item: items) {
                            if (item.equals("$KEYWORDS")) {
                                List<String> words = new ArrayList<>();
                                List<ProjectList> pitems = projectListRepository.findIdExpression("keywordListM");
                                for (ProjectList p : pitems) {
                                    words.addAll(p.getSynonyms());
                                }
                                if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), words);
                                else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, words);
                            } else if (item.startsWith("$ALTERATION") && item.contains("-")) {
                                String term = item.substring(item.indexOf("-")+1);
                                String synonym = AminoAcids.mutationSynonym(term);
                                log.info("{} {}", term, synonym);
                                if (!term.equals(synonym)) {
                                    if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Arrays.asList(term, synonym));
                                    else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Arrays.asList(term, synonym));
                                    List<String> hotspotSynonyms = AminoAcids.hotspotSubstitution(term);
                                    if (hotspotSynonyms != null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, hotspotSynonyms);
                                } else {
                                    if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Collections.singletonList(term));
                                    else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Collections.singletonList(term));
                                }
                            } else {
                                if (data == null) data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(IOUtils.toByteArray(gridFsTemplate.getResource(result.get(0)).getContent()), Collections.singletonList(item));
                                else data = new PDFHighlighter(parseColor(colors[ci % colors.length])).highlight(data, Collections.singletonList(item));
                            }
                            ci++;
                        }
                        return data;
                    }
                }
                return null;
            } else return null;
        } else {
            log.info("Not able to match PMID to full-text");
            return null;
        }
    }


    @Secured("ROLE_USER")
    @ApiOperation("Check that the download jobs are all finished to launch a new one, passing the running-{hashCode} item")
    @GetMapping(value = "/checkStatus/{code}")
    public String checkStatus(@PathVariable String code, Principal principal) {
        if (principal.getName() == null) return null;
        log.info(principal.getName());
        User user = userRepository.findByEmailAddress(principal.getName());
        if (user == null) {
            log.error("No User?");
            return null;
        }
        Map<String, Object> jobMap = userCache.get(user);
        if (jobMap.containsKey(code)) return code;
        String doneCode = "done-" + code.substring(code.indexOf("-") + 1);
        if (jobMap.containsKey(doneCode)) {
            synchronized (userCache) {
                jobMap.remove(doneCode);
            }
            return doneCode;
        }
        return "done";
    }

    @Secured("ROLE_USER")
    @ApiOperation("Attempt to download all listed PMIDs")
    @PostMapping(value = "/downloadKnowledge")
    public String getItemsPDF(@RequestBody List<String> pmids, Principal principal) {
        if (principal.getName() == null) return null;
        log.info(principal.getName());
        User user = userRepository.findByEmailAddress(principal.getName());
        if (user == null) {
            log.error("No User?");
            return null;
        }
        final String threadId = "running-" + SolrClientTool.randomId();
        final Thread t = new Thread(() -> {
            for (String pmid: pmids) {
                fullTextService.addArticle(pmid);
                if (threadStop.equals(threadId)) {
                    threadStop = "";
                    break;
                }
            }
            Thread t2;
            Map<String, Object> map = userCache.get(user);
            for (String s : map.keySet()) {
                if (s.equals(threadId)) {
                    t2 = (Thread)map.get(s);
                    map.remove(s);
                    map.put("done-"+s.substring(s.indexOf('-')+1), t2);
                    synchronized (userCache) {
                        userCache.put(user, map);
                    }
                }
            }
        });
        synchronized (userCache) {
            if (userCache.get(user) == null || userCache.get(user).isEmpty()) {
                Map<String, Object> jobMap = new ConcurrentHashMap<>();
                jobMap.put(threadId, t);
                t.setName(threadId);
                userCache.put(user, jobMap);
            } else {
                Map<String, Object> jobMap = userCache.get(user);
                jobMap.put(threadId, t);
                t.setName(threadId);
                userCache.put(user, jobMap);
            }
        }
        t.start();
        return threadId;
    }

    @Secured("ROLE_USER")
    @ApiOperation("Get the list of PMIDs and return the Article entities for each")
    @RequestMapping(value = "/fetch", method = RequestMethod.POST)
    public List<Article> getPMIDs(@RequestBody List<String> PMIDs) {
        return articleRepository.findAllByPmIdIn(PMIDs);
    }


    @RequestMapping(value = "/getSentences", method = RequestMethod.GET)
    public Map<String, Integer> getSentences(String PMID, String term) {
        return textService.getSentences(PMID, term);
    }

    @RequestMapping(value = "/getSupportingIDs/{pmid}", method = RequestMethod.GET)
    public List<String> getSupportingIDs(@PathVariable String pmid) {
        return fullTextRepository.findAllFullTextRegex(pmid).stream().map(FullText::getPmId).filter(pmId -> pmId.contains("S")).collect(Collectors.toList());
    }

    @Secured("ROLE_ADMIN")
    @ApiOperation(value = "Populate newest full text articles into MongoDB (for supplementaries) and Solr (for clustering)")
    @RequestMapping(value = "/populate", method = RequestMethod.GET)
    public String populateMongoSolr() {
        try {
            fullTextService.addAllArticles();
            return "Success";
        } catch (IOException | SolrServerException e) {
            return "Failure: " + e.getMessage();
        }
    }

    @Secured("ROLE_USER")
    @ApiOperation(value = "Find articles from the query interface")
    @RequestMapping(value = "/query2", method = RequestMethod.POST)
    public List<Article> queryArticles2(@RequestBody ArticleFilters filters, Principal principal) {
        if (filters.getGroups().length != filters.getValues().length || principal == null) return null; // invalid date;
        final DateTime after;
        if (filters.getDate() != null) {
            after = new DateTime(filters.getDate());
            log.info(after.toString());
        } else after = null;

        List<String> genes = new ArrayList<>();
        final List<List<String>> geneSynonyms = new ArrayList<>();
        List<String> mutations = new ArrayList<>();
        final List<List<String>> mutationSynonyms = new ArrayList<>();
        List<String> drugs = new ArrayList<>();
        List<String> cancers = new ArrayList<>();
        for (int i = 0; i < filters.getGroups().length; i++) {
            final String value = filters.getValues()[i];
            if (filters.getGroups()[i] == 0) {
                log.info("GeneMap filter " + i + " for value " + value);
                genes.add(value.toUpperCase());
                final Optional<Target> optionalTarget = targetRepository.findBySymbol(value.toUpperCase());
                optionalTarget.ifPresent(target -> {
                    List<String> values = new ArrayList<>();
                    Arrays.asList(target.getSynonyms().split(";")).parallelStream().forEach(values::add);
                    geneSynonyms.add(values);
                });
            }
            if (filters.getGroups()[i] == 1) {
                log.info("MutationMap filter " + i + " for value " + value);
                mutations.add(value.toUpperCase());
                String synonym = AminoAcids.mutationSynonym(value.toUpperCase());
                if (!synonym.equalsIgnoreCase(value)) {
                    mutationSynonyms.add(Collections.singletonList(synonym));
                }
            }
            if (filters.getGroups()[i] == 2) {
                log.info("DrugMap filter " + i + " for value " + value);
                drugs.add(value);
            }
            if (filters.getGroups()[i] == 3) {
                log.info("CancerMap filter " + i + " for value " + value);
                cancers.add(value);
            }
        }
        int limit = filters.getLimit();
        log.info("limit - maximum number is {} articles", limit);
        if (filters.getSearchTerms() != null && filters.getSearchTerms().length() > 0) {
            List<String> terms = Arrays.asList(filters.getSearchTerms().split(";"));
            log.info("{} custom terms", terms.size());
            cancers.addAll(terms);
        }

        SolrService.SearchResult result = solrService.searchSolr(limit+(int)(0.2*limit), genes, geneSynonyms, mutations, mutationSynonyms, drugs, null, cancers, null, null, filters.getAuthors(), after);
        List<String> pmIdsRaw = result.getPmIds();
        final Map<String, String> supportingText = new HashMap<>();
        List<String> pmIdsWithDuplicates = pmIdsRaw.stream().map(s -> {
            if (s.contains("S")) {
                if (supportingText.containsKey(s.substring(0, s.indexOf("S")))) supportingText.put(s.substring(0, s.indexOf("S")), supportingText.get(s.substring(0, s.indexOf("S")))+","+s);
                else supportingText.put(s.substring(0, s.indexOf("S")), s);

                return s.substring(0, s.indexOf("S"));
            }
            return s;
        }).collect(Collectors.toList());
        List<String> pmIds = new ArrayList<>(new LinkedHashSet<>(pmIdsWithDuplicates));

        List<Article> allArticles = new ArrayList<>();
        for (int i = 0; i < (pmIds.size()-1) / 1000 + 1; i++) {
            if (i * 1000 >= pmIds.size() || i * 1000 >= limit) {
                log.error("Code went over the limit");
                break;
            }
            if (after == null) allArticles.addAll(articleRepository.findAllByPmIdIn(pmIds.subList(i * 1000, min((i + 1) * 1000, pmIds.size(), limit))));
            else allArticles.addAll(articleRepository.findAllByPmIdInAndPublicationDateAfter(pmIds.subList(i * 1000, min((i + 1) * 1000, pmIds.size(), limit)), after));
        }
        log.info("All articles with possible date filtering are " + allArticles.size());
        allArticles.forEach(a -> {if (supportingText.containsKey(a.getPmId())) a.setInSupporting(supportingText.get(a.getPmId()));}); // inSupporting
        //List<Article> result = articles.parallelStream().map(id -> articleRepository.findByPmId(id+"")).filter(a -> after == null || (a.getPublicationDate() != null && a.getPublicationDate().isAfter(after))).limit(filters.getLimit()).collect(Collectors.toList());
        return allArticles;
    }

    public static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    @Secured("ROLE_USER")
    @ApiOperation("Get all supporting document resourceIds and filenames")
    @GetMapping("/supporting/{pmid}")
    public List<String> getSupportingResources(@PathVariable String pmid) {
        Optional<FullText> fullTextOptional = fullTextRepository.findById(pmid);
        if (fullTextOptional.isPresent()) {
            String[] resourceIds = fullTextOptional.get().getResourceIds();
            for (int i = 0; i < resourceIds.length; i++) {
                GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[i])));
                if (file == null) return null;
                resourceIds[i] += ":" + file.getFilename();
            }
            return Arrays.asList(resourceIds);
        }
        return new ArrayList<>();
    }

    @Secured("ROLE_USER")
    @ApiOperation("Get a supporting document from the GridFS")
    @GetMapping("/supporting/{resourceId}/{filename}")
    public ResponseEntity<byte[]> getResource(@PathVariable("resourceId") String resourceId, @PathVariable("filename") String filename) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
        if (file != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentType(MediaType.valueOf(gridFsTemplate.getResource(file).getContentType()));
            return new ResponseEntity<>(IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent()), headers, HttpStatus.OK);
        } else return null;
    }

    @Secured("ROLE_USER")
    @PostMapping(value = "/articles/addtext/{pmId}")
    public ResponseEntity<String> addPDF(@PathVariable String pmId, @RequestParam(value = "attachment") MultipartFile attachment) {
        Optional<FullText> oft = fullTextRepository.findById(pmId);
        if (oft.isPresent()) return new ResponseEntity<>("Full-text already exists", HttpStatus.BAD_REQUEST);
        final String fileName = "PMC/scratch/" + attachment.getOriginalFilename();
        final File file = new File(fileName);
        try {
            Files.write(file.toPath(), attachment.getBytes());
            FullText fullText = new FullText();
            fullText.setPmId(pmId);
            fullText.setTextEntry(TikaTool.parseDocument(fileName));
            fullText.setHTMLEntry(TikaTool.parseDocumentHTML(fileName));
            DBObject metaData = new BasicDBObject();
            metaData.put("pmId", pmId);
            ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(attachment.getBytes()), attachment.getOriginalFilename(), Files.probeContentType(file.toPath()), metaData);
            fullText.setResourceIds(new String[]{oid.toString()});
            fullTextRepository.save(fullText);
            Article article = articleRepository.findByPmId(pmId);
            article.setFulltext(fullText.getTextEntry());
            articleRepository.save(article);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (!file.delete()) log.error("Could not delete {}", fileName);
        log.info("Importing {} for PMID {} is complete", fileName, pmId);

        return new ResponseEntity<>("Success with " + attachment.getOriginalFilename(), HttpStatus.OK);
    }

    @Secured("ROLE_USER")
    @GetMapping("/stop/{threadId}")
    public ResponseEntity<String> stopJob(@PathVariable("threadId") String threadId, Principal principal) {
        String userName = principal.getName();
        User user = userRepository.findByEmailAddress(userName);
        synchronized (userCache) {
            Map<String, Object> userMap = userCache.get(user);
            if (userMap.containsKey(threadId)) {
                if (threadId.startsWith("running")) {
                    threadStop = threadId;
                    Thread t = (Thread)userMap.remove(threadId);
                    userMap.put("done-" + threadId.substring(threadId.indexOf("-")+1), t);
                    return new ResponseEntity<>("Success", HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>("No match", HttpStatus.OK);
    }

    public static Map<User, Map<String, Object>> getUserCache() {
        return userCache;
    }

    private static float[] parseColor(String color) {
        color = color.toUpperCase();
        int c1 = Integer.decode("0x" + color.substring(1, 3));
        int c2 = Integer.decode("0x" + color.substring(3, 5));
        int c3 = Integer.decode("0x" + color.substring(5, 7));
        return new float[] {c1/(float)255, c2/(float)255, c3/(float)255};
    }

}
