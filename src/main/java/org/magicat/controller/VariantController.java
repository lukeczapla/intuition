package org.magicat.controller;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.magicat.model.*;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.ProjectListRepository;
import org.magicat.repository.UserRepository;
import org.magicat.repository.VariantRepository;
import org.magicat.service.AnalyticsService;
import org.magicat.service.TextService;
import org.magicat.service.VariantService;
import org.magicat.util.SolrClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(originPatterns = {"**"})
@Api("Endpoints for getting Variant data and working with Variants lists and results for terms")
@RestController
public class VariantController {

    private final static Logger log = LoggerFactory.getLogger(VariantController.class);

    private final VariantRepository variantRepository;
    private final VariantService variantService;
    private final TextService textService;
    private final MongoTemplate mongoTemplate;
    private final ArticleRepository articleRepository;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final ProjectListRepository projectListRepository;

    @Autowired
    public VariantController(VariantRepository variantRepository, VariantService variantService, TextService textService, MongoTemplate mongoTemplate,
                             ArticleRepository articleRepository, AnalyticsService analyticsService, UserRepository userRepository, ProjectListRepository projectListRepository) {
        this.variantRepository = variantRepository;
        this.variantService = variantService;
        this.textService = textService;
        this.mongoTemplate = mongoTemplate;
        this.articleRepository = articleRepository;
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
        this.projectListRepository = projectListRepository;
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/variantsByKey/{key}", method = RequestMethod.GET)
    public List<Variant> getVariantsByKey(@PathVariable("key") String key) {
        return variantRepository.findAllByKey(key);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class SentencePage implements Comparable<SentencePage> {
        int page;
        String sentence;
        @Override
        public int compareTo(SentencePage other) {
            return this.page - other.page;
        }
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/variant/textAnalysis", method = RequestMethod.POST)
    public List<TextAnalysis> getTextAnalysis(@RequestBody TextAnalysis input) {
        Variant variant = variantRepository.findByDescriptor(input.getVariant());
        String[] urls = variant.getArticleURLs().split(" ");
        String pmId = input.getPmId();
        log.info("Looking for PMID {}", pmId);
        String term = input.getTerm();
        if (term.toLowerCase().endsWith(" fusion")) term = term.substring(0, term.toLowerCase().indexOf(" fusion")).trim();
        Set<String> pmIds = new TreeSet<>();
        boolean found = false;
        for (String url: urls) {
            if (url.contains(pmId)) {
                String pmIdPortion = url.substring(url.indexOf(pmId), url.lastIndexOf("."));
                if (pmIdPortion.equals(pmId) || (pmIdPortion.length() > pmId.length() && pmIdPortion.charAt(pmId.length()) == 'S')) {
                    pmIds.add(pmIdPortion);
                    log.info("PMID {} found", pmIdPortion);
                    found = true;
                }
            }
        }
        List<ProjectList> keywords = projectListRepository.findIdExpression("keywordListM");
        if (found) {
            List<TextAnalysis> output = new ArrayList<>();
            String pmIdsString = pmIds.stream().reduce("", (a, b) -> {
                if (a.equals("")) return a + b;
                else return a + "," + b;
            });
            log.info("The relevant PMID(s) is/are {} for the first passage", pmIdsString);
            for (String id : pmIds) {
                TextAnalysis t = new TextAnalysis();
                t.setPmId(id);
                t.setTerm(term);
                t.setParagraphs(textService.getParagraphs(id, term));
                t.setSentenceIslands(textService.getSentences(id, term));
                t.setPageNumbers(textService.getPageNumbers());
                List<SentencePage> sp = new ArrayList<>();
                List<String> keywordSentences = new ArrayList<>();
                List<Integer> keywordPages = new ArrayList<>();
                Map<String, Integer> keywordMap = textService.findKeywordSentences(term, keywords, id, false);
                for (String key: keywordMap.keySet()) {
                    sp.add(new SentencePage(keywordMap.get(key), key));
                }
                Collections.sort(sp);
                List<SentencePage> sp2 = new ArrayList<>();
                keywordMap = textService.findKeywordOnlySentences(term, keywords, id, false);
                for (String key: keywordMap.keySet()) {
                    sp2.add(new SentencePage(keywordMap.get(key), key));
                }
                Collections.sort(sp2);
                for (SentencePage p : sp) {
                    keywordSentences.add(p.getSentence());
                    keywordPages.add(p.getPage());
                }
                for (SentencePage p : sp2) {
                    keywordSentences.add(p.getSentence());
                    keywordPages.add(p.getPage());
                }
                t.setKeywordSentences(keywordSentences);
                t.setKeywordPageNumbers(keywordPages);
                output.add(t);
            }
            log.info("Returning output");
            return output;
        } else {
            log.info("Not found in URLs, sending the abstract and data");
            TextAnalysis t = new TextAnalysis();
            t.setPmId(input.getPmId());
            t.setTerm(term);
            Article a = articleRepository.findByPmId(input.getPmId());
            List<String> paragraphs = Collections.singletonList(a.getPubAbstract());
            t.setParagraphs(paragraphs);
            t.setSentenceIslands(null);
            t.setPageNumbers(Collections.singletonList(1));
            List<String> keywordSentences = new ArrayList<>();
            List<Integer> keywordPages = new ArrayList<>();
            Map<String, Integer> keywordMap = textService.findKeywordSentences(term, keywords, input.getPmId(), false);
            for (String key: keywordMap.keySet()) {
                keywordSentences.add(key);
                keywordPages.add(keywordMap.get(key));
            }
            keywordMap = textService.findKeywordOnlySentences(term, keywords, input.getPmId(), false);
            for (String key: keywordMap.keySet()) {
                keywordSentences.add(key);
                keywordPages.add(keywordMap.get(key));
            }
            t.setKeywordSentences(keywordSentences);
            t.setKeywordPageNumbers(keywordPages);
            return Collections.singletonList(t);
        }
    }

    @Secured("ROLE_USER")
    @GetMapping(value = "/variants/download/autocuration{key}.xlsx", produces = {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})
    public byte[] getVariantSpreadsheet(@PathVariable("key") String key) {
        byte[] data;
        if (key.equals("_no_key")) {
            data = variantService.createVariantSpreadsheet(variantRepository.findAllKeyless());
        }
        else {
            data = variantService.createVariantSpreadsheet(variantRepository.findAllByKey(key));
        }
        return data;
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/getVariantKeys", method = RequestMethod.GET)
    public List<String> getAllKeys() {
        List<String> result = new ArrayList<>();
        for (String s : mongoTemplate.getCollection("Variants").distinct("key", String.class)) {
            result.add(s);
        }
        return result;
    }

    @Secured("ROLE_USER")
    @GetMapping(value = "/getVariantsByGene/{gene}")
    public List<Variant> getVariantsByGene(@PathVariable("gene") String gene) {
        return variantRepository.findAllByGene(gene.toUpperCase());
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/runVariant", method = RequestMethod.GET)
    public void runVariant(String descriptor, Principal principal) {
        analyticsService.processVariants(Collections.singletonList(variantRepository.findByDescriptor(descriptor)));
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/runVariants/{key}", method = RequestMethod.GET)
    public ResponseEntity<String> runVariants(@PathVariable("key") String key, Principal principal) {
        User user = userRepository.findByEmailAddress(principal.getName());
        if (user == null) {
            log.error("No User?");
            return new ResponseEntity<>("Forbidden, no user found.", HttpStatus.FORBIDDEN);
        }
        final Map<User, Map<String, Object>> userCache = ArticleController.getUserCache();
        final String threadId = "vrunning-"+SolrClientTool.randomId();
        Thread t = new Thread(() -> {
            if (key.equals("none")) analyticsService.processVariants(variantRepository.findAllKeyless());
            else analyticsService.processVariants(variantRepository.findAllByKey(key));
            Thread t2;
            Map<String, Object> map = new ConcurrentHashMap<>(userCache.get(user));
            for (String s : map.keySet()) {
                if (s.startsWith("vrunning-") && s.equals(threadId)) {
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
                t.setName(threadId);
                jobMap.put(t.getName(), t);
                userCache.put(user, jobMap);
            } else {
                Map<String, Object> jobMap = userCache.get(user);
                t.setName(threadId);
                jobMap.put(t.getName(), t);
                userCache.put(user, jobMap);
            }
        }
        t.start();
        return new ResponseEntity<>(t.getName(), HttpStatus.OK);
    }

    @Secured("ROLE_USER")
    @GetMapping("/vstop/{threadId}")
    public ResponseEntity<String> stopJob(@PathVariable("threadId") String threadId, Principal principal) {
        String userName = principal.getName();
        User user = userRepository.findByEmailAddress(userName);
        final Map<User, Map<String, Object>> userCache = ArticleController.getUserCache();
        synchronized (userCache) {
            Map<String, Object> userMap = userCache.get(user);
            if (userMap.containsKey(threadId)) {
                if (threadId.startsWith("vrunning")) {
                    analyticsService.setThreadStop(threadId);
                    Thread t = (Thread)userMap.remove(threadId);
                    userMap.put("done-" + threadId.substring(threadId.indexOf("-")+1), t);
                    return new ResponseEntity<>("Success", HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>("No match", HttpStatus.OK);
    }

    @Secured("ROLE_USER")
    @GetMapping(value = "/getPDB/{PDBcode}")
    public ResponseEntity<String> getPDB(@PathVariable("PDBcode") String PDBcode) {
        if (!PDBcode.endsWith(".pdb")) PDBcode += ".pdb";
        log.info("Retrieving PDB {}", PDBcode);
        RestTemplate rest = new RestTemplate();
        String resourceUrl = "https://files.rcsb.org/download/" + PDBcode;
        ResponseEntity<String> response = rest.getForEntity(resourceUrl, String.class);
        if (response.hasBody()) {
            return new ResponseEntity<>(response.getBody(), HttpStatus.OK);
        } else return new ResponseEntity<>("Could not find", HttpStatus.BAD_REQUEST);
    }

    @Secured("ROLE_USER")
    @PostMapping(value = "/updateVariant")
    public ResponseEntity<String> updateVariant(@RequestBody Variant v) {
        if (v.getId() == null) {
            log.error("Id field is not set on this variant, is this a new variant or an update?");
            return new ResponseEntity<>("No Id field on Variant, does not seem to be an update.", HttpStatus.BAD_REQUEST);
        }
        variantRepository.save(v);
        return new ResponseEntity<>("Success!", HttpStatus.OK);
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/getAllVariants", method = RequestMethod.GET)
    public List<Variant> getAllVariants() {
        return variantRepository.findAll();
    }

    @Secured("ROLE_USER")
    @PostMapping(value = "/addVariants/{key}")
    public ResponseEntity<String> addVariants(@PathVariable("key") String key, @RequestParam(value = "attachment") MultipartFile attachment, @RequestParam(value = "submitrun", required=false) Boolean submitRun, Principal principal) {
        if (principal.getName() == null) return new ResponseEntity<>("Forbidden, no user located.", HttpStatus.FORBIDDEN);
        log.info(principal.getName());
        final User user = userRepository.findByEmailAddress(principal.getName());
        if (user == null) {
            log.error("No User?");
            return new ResponseEntity<>("Forbidden, no user found.", HttpStatus.FORBIDDEN);
        }
        if (attachment.getOriginalFilename() == null || !attachment.getOriginalFilename().toLowerCase().endsWith(".xlsx")) return new ResponseEntity<>("The file attachment is invalid, should end with .xlsx", HttpStatus.BAD_REQUEST);
        File folder = new File("PMC/scratch");
        if (!folder.exists()) folder.mkdir();
        String fileName = "PMC/scratch/" + attachment.getOriginalFilename();
        log.info("fileName : {}, submitRun : {}, key : {}", fileName, submitRun, key);
        File file = new File(fileName);
        String jobName = "";
        try {
            Files.write(file.toPath(), attachment.getBytes());
            analyticsService.setKey(key);
            analyticsService.processSpreadsheet(fileName);
            if (submitRun) {
                final Map<User, Map<String, Object>> userCache = ArticleController.getUserCache();
                final String threadId = "vrunning-" + SolrClientTool.randomId();
                Thread t = new Thread(() -> {
                    analyticsService.setThread(threadId);
                    analyticsService.processVariants(variantRepository.findAllByKey(key));
                    Thread t2;
                    Map<String, Object> map = new ConcurrentHashMap<>(userCache.get(user));
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
                        Map<String, Object> jobMap = new HashMap<>();
                        t.setName(threadId);
                        jobMap.put(t.getName(), t);
                        userCache.put(user, jobMap);
                    } else {
                        Map<String, Object> jobMap = userCache.get(user);
                        t.setName(threadId);
                        jobMap.put(t.getName(), t);
                        userCache.put(user, jobMap);
                    }
                }
                t.start();
                jobName = t.getName();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (!file.delete()) log.error("Could not delete {}", fileName);
        if (submitRun != null && submitRun) return new ResponseEntity<>(jobName, HttpStatus.OK);
        return new ResponseEntity<>("Success with " + attachment.getOriginalFilename(), HttpStatus.OK);
    }


}
