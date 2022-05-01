package org.magicat.service;

import org.apache.commons.lang.WordUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.magicat.model.Article;
import org.magicat.model.CancerType;
import org.magicat.model.DrugMap;
import org.magicat.model.ProjectList;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.CancerTypeRepository;
import org.magicat.repository.DrugMapRepository;
import org.magicat.repository.ProjectListRepository;
import org.magicat.util.ProcessUtil;
import org.magicat.util.Tree;
import org.magicat.util.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepository;
    private final ProjectListRepository projectListRepository;
    private final CancerTypeRepository cancerTypeRepository;
    private final DrugMapRepository drugMapRepository;

    private boolean relevance = false;
    private XMLParser parser = null;
    private int hashCode;
    private final Map<String, List<Pattern>> patternMap = new HashMap<>();

    private int count = 0, runCount = 0;
    private List<Article> processArticles = new ArrayList<>();

    @Autowired
    public ArticleServiceImpl(ArticleRepository articleRepository, ProjectListRepository projectListRepository, CancerTypeRepository cancerTypeRepository,
                              DrugMapRepository drugMapRepository) {
        this.articleRepository = articleRepository;
        this.projectListRepository = projectListRepository;
        this.cancerTypeRepository = cancerTypeRepository;
        this.drugMapRepository = drugMapRepository;
    }

    public boolean addArticlePDF(String pmId) {
        Article a = articleRepository.findByPmId(pmId);
        if (a == null) {
            log.info("No article {}", pmId);
            return false;
        }
        if (a.getFulltext() != null) {
            log.info("Full text already exists for {}", pmId);
            return false;
        }
        if (a.getPmcId() == null || a.getPmcId().length() < 2) {
            String doiurl;
            String fileName;
            if (a.getDoi() == null) return false;
            if (a.getDoi().indexOf("/") == a.getDoi().lastIndexOf("/"))
                doiurl = "https://www.doi.org/" + a.getDoi();
            else
                doiurl = "https://www.doi.org/" + a.getDoi().substring(0, a.getDoi().indexOf("/")) + a.getDoi().substring(a.getDoi().lastIndexOf("/"));
            try {
                fileName = Math.abs(doiurl.hashCode()) + ".pdf";
                ProcessUtil.runScript("python3 python/doi.py " + doiurl + " " + fileName);
                if (!new File(fileName).exists() && a.getDoi().indexOf("/") != a.getDoi().lastIndexOf("/")) {
                    doiurl = "https://www.doi.org/" + a.getDoi();
                    fileName = Math.abs(doiurl.hashCode()) + ".pdf";
                    ProcessUtil.runScript("python3 python/doi.py " + doiurl + " " + fileName);
                }
                if (!new File(fileName).exists()) {
                    log.info("No DOI PDF file exists");
                    return false;
                }
                log.info("Found DOI PDF and saving into {}", "PMC/"+pmId+"/"+pmId+".pdf");
                File f = new File("PMC/" + pmId);
                f.mkdir();
                Files.move(Paths.get(fileName), Paths.get("PMC/" + pmId + "/" + pmId + ".pdf"));
                return true;
            } catch (IOException e) {
                log.error(e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        String PMCId = a.getPmcId().replace("PMC", "");
        boolean found = false;
        try {
            ProcessUtil.runScript("python3 python/PMC.py " + PMCId);
            File f = new File("PMC/" + pmId);
            f.mkdir();
            f = new File(PMCId + ".tar.gz");
            if (f.exists()) {
                //found = true;
                log.info("PMC .tar.gz file found {}", PMCId+".tar.gz");
                Files.move(Paths.get(PMCId + ".tar.gz"), Paths.get("PMC/" + pmId
                        + "/" + PMCId + ".tar.gz"));
                ProcessUtil.runScript("./expand " + pmId + " " + PMCId);
            } else log.info("No PMC .tar.gz file found");
            f = new File(PMCId + ".pdf");
            if (f.exists()) {
                found = true;
                Files.move(Paths.get(PMCId + ".pdf"), Paths.get("PMC/" + pmId
                        + "/" + PMCId + ".pdf"));
            } else log.info("No PMC [PMCId].pdf file found");
            f = new File("PMC" + PMCId + ".pdf");
            if (f.exists()) {
                log.info("The PMC pdf file {} exists!", "PMC"+PMCId+".pdf");
                found = true;
                Files.move(Paths.get("PMC" + PMCId + ".pdf"), Paths.get("PMC/" + pmId
                        + "/PMC" + PMCId + ".pdf"));
            } else log.info("No PMC PMC[PMCID].pdf file found");
            // check to see if any PDF files are in the folder!
            Stream<Path> walk = Files.walk(Paths.get("PMC/" + pmId));
            List<String> files = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).filter(s -> s.toLowerCase().endsWith(".pdf")).collect(Collectors.toList());
            if (files.size() == 0) found = false;
            else found = true;
        } catch (IOException e) {
            log.error("Failed with PMC retrieval: {}, continuing with DOI", e.getMessage());
        }
        if (!found) {   // try DOI!
            String doiurl;
            String fileName = "demo1.pdf";
            if (a.getDoi() == null) return false;
            if (a.getDoi().indexOf("/") == a.getDoi().lastIndexOf("/"))
                doiurl = "https://www.doi.org/" + a.getDoi();
            else
                doiurl = "https://www.doi.org/" + a.getDoi().substring(0, a.getDoi().indexOf("/")) + a.getDoi().substring(a.getDoi().lastIndexOf("/"));
            try {
                fileName = Math.abs(doiurl.hashCode()) + ".pdf";
                ProcessUtil.runScript("python3 python/doi.py " + doiurl + " " + fileName);
                if (!new File(fileName).exists() && a.getDoi().indexOf("/") != a.getDoi().lastIndexOf("/")) {
                    doiurl = "https://www.doi.org/" + a.getDoi();
                    fileName = Math.abs(doiurl.hashCode()) + ".pdf";
                    ProcessUtil.runScript("python3 python/doi.py " + doiurl + " " + fileName);
                }
                if (!new File(fileName).exists()) {
                    log.info("No DOI PDF file exists");
                    return false;
                }
                log.info("Found DOI PDF and saving into {}", "PMC/"+pmId+"/"+pmId+".pdf");
                File f = new File("PMC/" + pmId);
                f.mkdir();
                Files.move(Paths.get(fileName), Paths.get("PMC/" + pmId + "/" + pmId + ".pdf"));
                return true;
            } catch (IOException e) {
                log.error("Failed with DOI retrieval: {}", e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        return found;
    }

    @Override
    public boolean addArticlePMID(String pmId) {
        if (articleRepository.findByPmId(pmId) != null) return false;
        try {
            String XMLfile = "pubmed_ids.xml";
            ProcessUtil.runScript("python3 python/pubmed_ids.py "+ pmId);
            if (parser == null) {
                parser = new XMLParser(XMLfile);
                parser.setArticleRepository(articleRepository);
            }
            else parser.reload(XMLfile);
            parser.DFS(parser.getRoot(), Tree.articleTree(), null);
            return true;
        } catch (IOException|ParserConfigurationException|SAXException|NoSuchFieldException|IllegalAccessException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void addCitation(Article article, List<Article> articleList) {
        if (article.getCitation() == null) {
            try {
                String XMLfile = "pubmed" + Math.abs(article.getPmId().hashCode()) + ".xml";
                ProcessUtil.runScript("python3 python/pubmed.py 1 " + XMLfile + " " + article.getPmId());
                if (parser == null) {
                    parser = new XMLParser(XMLfile);
                    parser.setArticleRepository(articleRepository);
                    parser.setDb(articleList);
                    hashCode = articleList.hashCode();
                }
                else {
                    if (hashCode != articleList.hashCode()) {
                        parser.clearDb();
                        parser.setDb(articleList);
                        hashCode = articleList.hashCode();
                    }
                    parser.reload(XMLfile);
                }
                parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
                article = articleRepository.findByPmId(article.getPmId());
                if (article.getJournal() != null) {
                    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy MMM");
                    article.setCitation(WordUtils.capitalizeFully(article.getJournal()) + " " + (article.getPubDate() != null ? article.getPubDate().toString(fmt) + "; " : "") + (article.getVolume() != null ? article.getVolume(): "") + (article.getIssue() != null ? "("+article.getIssue()+")": "") + (article.getPageNumbers() != null ? ":"+article.getPageNumbers():""));
                    articleRepository.save(article);
                    log.info("Successfully set citation for " + article.getPmId());
                    log.info("Has pubDate = " + (article.getPubDate() != null ? "Check: "+article.getPubDate().toString(fmt): "X"));
                    log.info("Has volume = " + (article.getVolume() != null ? "Check": "X"));
                    log.info("Has issue = " + (article.getIssue() != null ? "Check": "X"));
                    log.info("Has pageNumber = " + (article.getPageNumbers() != null ? "Check": "X"));
                } else {
                    log.info("Not enough information to set citation record for " + article.getPmId());
                }
                if (new File(XMLfile).delete()) log.info("Deleted XMLfile");
            } catch (IOException|ParserConfigurationException|SAXException|NoSuchFieldException|IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    private int total = 0;
    private String pmIdList = "";

    private void processQueueItems(List<Article> articleList) {
        if (pmIdList.equals("")) return;
        List<String> pmIds = Arrays.asList(pmIdList.split(","));
        try {
            String XMLfile = "pubmed_ids.xml";
            ProcessUtil.runScript("python3 python/pubmed_ids.py " + pmIdList);
            if (parser == null) {
                parser = new XMLParser(XMLfile);
                parser.setArticleRepository(articleRepository);
                parser.setDb(articleList);
                hashCode = articleList.hashCode();
            } else {
                if (hashCode != articleList.hashCode()) {
                    log.info("Loading new List<Article>");
                    //parser.clearDb();
                    parser.setDb(articleList);
                    hashCode = articleList.hashCode();
                }
                parser.reload(XMLfile);
            }
            parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
            List<Article> articles = articleRepository.findAllByPmIdIn(pmIds);
            articles.parallelStream().forEach(a -> {
                if (a.getJournal() != null) {
                    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy MMM");
                    a.setCitation(WordUtils.capitalizeFully(a.getJournal()) + " " + (a.getPubDate() != null ? a.getPubDate().toString(fmt) + "; " : "") + (a.getVolume() != null ? a.getVolume() : "") + (a.getIssue() != null ? "(" + a.getIssue() + ")" : "") + (a.getPageNumbers() != null ? ":" + a.getPageNumbers() : ""));
                    articleRepository.save(a);
                    log.info("Successfully set citation for " + a.getPmId());
                    //log.info("Has pubDate = " + (article.getPubDate() != null ? "Check: " + article.getPubDate().toString(fmt) : "X"));
                    //log.info("Has volume = " + (article.getVolume() != null ? "Check" : "X"));
                    //log.info("Has issue = " + (article.getIssue() != null ? "Check" : "X"));
                    //log.info("Has pageNumber = " + (article.getPageNumbers() != null ? "Check" : "X"));

                } else {
                    log.info("Not enough information to set citation (journal + date + volume etc.) record for " + a.getPmId());
                }
            });
            if (new File(XMLfile).delete()) log.info("Deleted XMLfile");
            pmIdList = "";

        } catch (IOException | ParserConfigurationException | SAXException | NoSuchFieldException | IllegalAccessException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void addCitationQueue(Article article, List<Article> articleList, int size) {
        if (article == null) { //flush the queue by passing null
            processQueueItems(articleList);
            return;
        }
        if (article.getCitation() == null) {
            if (pmIdList.equals("")) pmIdList += article.getPmId();
            else pmIdList += ","+article.getPmId();
            total++;
            if (total % size == 0) {
                processQueueItems(articleList);
            }
        }
    }


    public void updateArticlesPubmed() {
        populateDrugs();
        populateGenes();
        populateOncoBase();
    }

    public void updateArticlesPubmedRecent() {
        populateDrugsRecent();
        populateGenesRecent();
        populateOncoRecent();
    }


    public void updateMaps(String[] items) {
        updatePatterns(items);
    }

    public void updateCitations(int pageNumber, int pageLimit) {
        Page<Article> pages;
        do {
            pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
            processArticles(pages.getContent(), pageNumber);
        } while (pages.hasNext());
        log.info("Total items: " + pages.getTotalElements());
        if (processArticles.size() > 0) {
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < processArticles.size(); i++) {
                if (i == 0) items.append(processArticles.get(i).getPmId().trim());
                else items.append(",").append(processArticles.get(i).getPmId().trim());
            }
            try {
                ProcessUtil.runScript("python3 python/pubmed_list.py " + items);
                parser.reload("pubmed_list.xml");
                parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
            } catch (Exception e) {
                log.error("Error occurred: " + e.getMessage());
            }
        }
    }

    public void updateCitations() {
        updateCitations(0, 50000);
    }

    private void processArticles(List<Article> articles, int pageNumber) {
        for (Article article: articles) {
            if (article.getTitle() == null && article.getCitation() != null) {
                processArticles.add(article);
                if (processArticles.size() > 197) {
                    StringBuilder items = new StringBuilder();
                    for (int i = 0; i < processArticles.size(); i++) {
                        if (i == 0) items.append(processArticles.get(i).getPmId().trim());
                        else items.append(",").append(processArticles.get(i).getPmId().trim());
                    }
                    try {
                        if (runCount == 0) {
                            ProcessUtil.runScript("python3 python/pubmed_list.py " + items);
                            setupProcess();
                            parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
                        } else {
                            ProcessUtil.runScript("python3 python/pubmed_list.py " + items);
                            parser.reload("pubmed_list.xml");
                            parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
                        }
                        runCount++;
                    } catch (Exception e) {
                        log.error("Error occurred: " + e.getMessage());
                        //System.exit(-1);
                    }
                    processArticles = new ArrayList<>();
                }
            }
        }
    }

    private void setupProcess() {
        // we may have previously been pulling the most recent articles with all PMIDs loaded
        if (parser != null) {
            parser.reload("pubmed_list.xml");
            return;
        }
        try {
            parser = new XMLParser("pubmed_list.xml");
            log.info("Reading in all article PMIDs");
            List<Article> articles = articleRepository.findAllPmId();
            parser.setDb(articles);
            parser.setArticleRepository(articleRepository);
            log.info("Completed reading in {} articles", articles.size());
        } catch (Exception e) {
            log.error(e.getMessage() + " in setupProcess() of ArticleServiceImpl.java");
        }
    }

    protected void updatePatterns(String[] items) {
        for (String item : items) {
            patternMap.merge(item, Collections.singletonList(Pattern.compile("([\\W\\s^_]|^)(" + item.toLowerCase() + ")([\\W\\s^_]|$)", Pattern.CASE_INSENSITIVE)), (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList()));
        }
    }

    public boolean indexArticles() {
        try {
            parser = new XMLParser("demo.xml");
            parser.setArticleRepository(articleRepository);
            log.info("Setting up list of PMIDs");
            parser.setDb(articleRepository.findAllPmId());
            log.info("Completed setting up PMID list");
        } catch (SAXException|ParserConfigurationException|IOException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public String runSearchPython(String term, int size) {
        if (term == null) return null;
        String XMLFile = "";
        try {
            log.info("RUNNING PYTHON");
            XMLFile = "pubmed" + term.hashCode() + ".xml";
            String pythonScript = relevance ? "python/pubmed_relevance.py" : "python/pubmed.py";
            ProcessUtil.runScript("python3 " + pythonScript + " " + size + " " + XMLFile + " " + term.trim());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return XMLFile;
    }

    public String runSearchPython(String term, String synonyms, int size) {
        if (term == null) return null;
        String XMLFile = "";
        StringBuilder searchTerm = new StringBuilder(term);
        if (synonyms != null && synonyms.length() > 0) {
            for (String synonym : synonyms.split(";")) {
                searchTerm.append(" OR ").append(synonym);
            }
        }
        try {
            log.info("RUNNING PYTHON");
            XMLFile = "pubmed" + searchTerm.toString().hashCode() + ".xml";
            ProcessUtil.runScript("python3 python/pubmed.py " + size + " " + XMLFile + " " + searchTerm.toString().trim());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return XMLFile;
    }

    public void runSearchComplete(String XMLFile) {
        parser.reload(XMLFile);
        try {
            parser.DFSFast(parser.getRoot(), Tree.articleTree(), null);
        } catch (NoSuchFieldException|IllegalAccessException e) {
            log.error(e.getMessage());
        }
        new File(XMLFile).delete();
    }

    public void runSearch(String term, int size, int count) {
        if (term == null) return;
        try {
            log.info("RUNNING PYTHON");
            String XMLFile = "pubmed" + term.hashCode() + ".xml";
            ProcessUtil.runScript("python3 python/pubmed.py " + size + " " + XMLFile + " " + term.trim());
            log.info("DONE RUNNING PYTHON");
            if (count == 0) {
                parser = new XMLParser(XMLFile);
                parser.setArticleRepository(articleRepository);
                parser.setDb(articleRepository.findAllPmId());
            } else {
                parser.reload(XMLFile);
            }
            parser.DFSFast(parser.getRoot(), Tree.articleTree(), null);
            new File(XMLFile).delete();
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("For term: " + term.trim());
        }
    }

    @Override
    public void runSearches(List<String> terms, int size) {
        Collections.shuffle(terms); // ensures that any error from throttling at the beginning is evenly distributed
        if (indexArticles()) {
            final Semaphore concurrentDFSExecutions = new Semaphore(16);
            terms.parallelStream().forEach((g) -> {
                String XMLFile = "";
                synchronized (this) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        log.error("Somebody woke up the monster while sleeping, beware!");
                    }
                    XMLFile = runSearchPython(g, size);
                }

                log.info(g);

                concurrentDFSExecutions.acquireUninterruptibly();
                try {
                    if (XMLFile.length() > 0) runSearchCompleteParallel(XMLFile);
                } finally {
                    concurrentDFSExecutions.release();
                }
            });
        }
    }

    @Override
    public void addCitationRecords(int pageNumber, int pageSize) {
        Page<Article> pages;
        List<Article> articleList;
        do {
            pages = articleRepository.findAll(PageRequest.of(pageNumber, pageSize));
            articleList = pages.getContent();
            log.info("Total articles in page " + pageNumber + ": " + articleList.size());
            for (Article article : articleList) {
                if (article.getCitation() == null) {
                    log.info("Updating citation for " + article.getPmId());
                    addCitationQueue(article, articleList, 200);
                }
            }
            pageNumber++;
        } while (pages.hasNext());
        addCitationQueue(null, articleList, 200);
    }


    void populateGenes() {
        Optional<ProjectList> opl = projectListRepository.findById("geneSearchList");
        if (opl.isEmpty()) {
            log.error("CANNOT FIND THE geneSearchList IN THE ProjectLists collection");
            return;
        }
        ProjectList projectList = opl.get();
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < projectList.getNames().size(); i++) {
            StringBuilder searchTerm = new StringBuilder(quote(projectList.getNames().get(i)));
            if (projectList.getSynonyms() != null && projectList.getSynonyms().size() > i && projectList.getSynonyms().get(i) != null && projectList.getSynonyms().get(i).length() > 0) {
                for (String synonym : projectList.getSynonyms().get(i).split(";")) searchTerm.append(" OR ").append(quote(synonym));
            }
            log.info(projectList.getNames().get(i) + " searchTerm = " + searchTerm);
            terms.add(searchTerm.toString());
        }
        runSearches(terms, 50000);
    }

    void populateGenesRecent() {
        Optional<ProjectList> opl = projectListRepository.findById("geneSearchList");
        if (opl.isEmpty()) {
            log.error("CANNOT FIND THE geneSearchList IN THE ProjectLists COLLECTION");
            return;
        }
        ProjectList projectList = opl.get();
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < projectList.getNames().size(); i++) {
            StringBuilder searchTerm = new StringBuilder(quote(projectList.getNames().get(i)));
            if (projectList.getSynonyms() != null && projectList.getSynonyms().size() > i && projectList.getSynonyms().get(i) != null && projectList.getSynonyms().get(i).length() > 0) {
                for (String synonym : projectList.getSynonyms().get(i).split(";")) searchTerm.append(" OR ").append(quote(synonym));
            }
            log.info(projectList.getNames().get(i) + " searchTerm = " + searchTerm);
            terms.add(searchTerm.toString());
        }
        runSearches(terms, 500);
    }

    void populateDrugs() {
        List<DrugMap> drugs = drugMapRepository.findCancerDrugs();
        List<String> terms = new ArrayList<>();
        for (DrugMap dm : drugs) {
            String term = quote(dm.getDrug());
            if (dm.getSynonyms() != null && dm.getSynonyms().length() > 0) {
                String[] synonyms = dm.getSynonyms().split(",");
                for (String s : synonyms) {
                    term += " OR " + quote(s);
                }
            }
            terms.add(term);
        }
        runSearches(terms, 50000);
    }

    void populateDrugsRecent() {
        List<DrugMap> drugs = drugMapRepository.findCancerDrugs();
        List<String> terms = new ArrayList<>();
        for (DrugMap dm : drugs) {
            String term = quote(dm.getDrug());
            if (dm.getSynonyms() != null && dm.getSynonyms().length() > 0) {
                String[] synonyms = dm.getSynonyms().split(",");
                for (String s : synonyms) {
                    term += " OR " + quote(s);
                }
            }
            terms.add(term);
        }
        runSearches(terms, 500);
    }

    void populateOncoBase() {
        List<CancerType> cancerTypes = cancerTypeRepository.findAll();
        List<String> terms = new ArrayList<>();
        for (CancerType ct : cancerTypes) {
            String term = "";
            if (ct.getMainType() != null) term = quote(ct.getMainType());
            if (ct.getSubType() != null) {
                String subType = ct.getSubType();
                if (subType.endsWith(", NOS")) subType = subType.substring(0, subType.length()-5);
                if (term.length() > 0) term += " OR ";
                term += quote(subType);
            }
            terms.add(term);
        }
        runSearches(terms, 25000);
    }

    void populateOncoRecent() {
        List<CancerType> cancerTypes = cancerTypeRepository.findAll();
        List<String> terms = new ArrayList<>();
        for (CancerType ct : cancerTypes) {
            String term = "";
            if (ct.getMainType() != null) term = quote(ct.getMainType());
            if (ct.getSubType() != null) {
                String subType = ct.getSubType();
                if (subType.endsWith(", NOS")) subType = subType.substring(0, subType.length()-5);
                if (term.length() > 0) term += " OR ";
                term += quote(subType);
            }
            terms.add(term);
        }
        runSearches(terms, 500);

    }

    public void runSearchCompleteParallel(String XMLFile) {
        try {
            XMLParser p2 = parser.dup(XMLFile);
            //p2.reload(XMLFile);
            p2.DFSFast(p2.getRoot(), Tree.articleTree(), null);
        } catch (NoSuchFieldException|IllegalAccessException|ParserConfigurationException|SAXException|IOException e) {
            log.error("Error during parsing XML file");
            log.error(e.getMessage());
        }
        new File(XMLFile).delete();
    }

    public void setRelevance(boolean relevance) {
        this.relevance = relevance;
    }

    public static String quote(String term) {
        return "\"" + term + "\"";
    }

}
