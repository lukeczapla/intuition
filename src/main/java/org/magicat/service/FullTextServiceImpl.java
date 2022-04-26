package org.magicat.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.joda.time.DateTimeZone;
import org.magicat.model.Article;
import org.magicat.model.FullText;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.FullTextRepository;
import org.magicat.util.SolrClientTool;
import org.magicat.util.TikaTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FullTextServiceImpl implements org.magicat.service.FullTextService {

    public static final Logger log = LoggerFactory.getLogger(org.magicat.service.FullTextService.class);

    private final ArticleService articleService;
    private final GridFsTemplate gridFsTemplate;
    private final ArticleRepository articleRepository;
    private final FullTextRepository fullTextRepository;
    private final SolrClientTool solrClientTool;

    @Autowired
    public FullTextServiceImpl(ArticleService articleService, GridFsTemplate gridFsTemplate, ArticleRepository articleRepository,
                               FullTextRepository fullTextRepository, SolrClientTool solrClientTool) {
        this.articleService = articleService;
        this.gridFsTemplate = gridFsTemplate;
        this.articleRepository = articleRepository;
        this.fullTextRepository = fullTextRepository;
        this.solrClientTool = solrClientTool;
    }

    @Override
    public boolean addArticle(String pmId) {
        try {
            boolean found = true;
            if (!(new File("PMC/" + pmId).exists())) found = articleService.addArticlePDF(pmId);
            if (!found) return false;

            Stream<Path> walk = Files.walk(Paths.get("PMC/" + pmId));
            List<String> files = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).collect(Collectors.toList());
            if (files.size() == 0) return false;
            walk = Files.walk(Paths.get("PMC/" + pmId));

            files = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).filter(s -> !s.endsWith(".tar.gz") && !s.endsWith(".pdf")).collect(Collectors.toList());
            walk = Files.walk(Paths.get("PMC/" + pmId));
            List<String> PDFfiles = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).filter(s -> s.endsWith(".pdf")).collect(Collectors.toList());
            if (PDFfiles.size() > 1) {
                for (int i = 0; i < PDFfiles.size(); i++)
                    for (int j = i + 1; j < PDFfiles.size(); j++) {
                        if (new File(PDFfiles.get(i)).length() == new File(PDFfiles.get(j)).length()) {
                            PDFfiles.remove(j);
                            j--;
                        }
                    }
                PDFfiles.sort(Comparator.comparingInt(String::length));
                log.info(PDFfiles.toString());
            }
            Optional<FullText> opt = fullTextRepository.findById(pmId);
            if (opt.isEmpty()) {
                FullText fullText = new FullText();
                fullText.setPmId(pmId);
                Binary[] binary = new Binary[PDFfiles.size() + files.size()];
                String[] fileNames = new String[PDFfiles.size() + files.size()];
                String[] contentTypes = new String[PDFfiles.size() + files.size()];
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < PDFfiles.size(); i++) {
                    binary[i] = new Binary(Files.readAllBytes(new File(PDFfiles.get(i)).toPath()));
                    fileNames[i] = PDFfiles.get(i).substring(PDFfiles.get(i).lastIndexOf("/") + 1);
                    contentTypes[i] = Files.probeContentType(new File(PDFfiles.get(i)).toPath());
                    if (i == 0) {
                        log.info("Analyzing " + PDFfiles.get(0));
                        fullText.setTextEntry(TikaTool.parseDocument(PDFfiles.get(0)));
                        fullText.setHTMLEntry(TikaTool.parseDocumentHTML(PDFfiles.get(0)));
                    }
                }
                files.sort((a, b) -> (int) (new File(a).length() - new File(b).length()));
                for (int i = 0; i < files.size(); i++) {
                    binary[i + PDFfiles.size()] = new Binary(Files.readAllBytes(new File(files.get(i)).toPath()));
                    fileNames[i + PDFfiles.size()] = files.get(i).substring(files.get(i).lastIndexOf("/") + 1);
                    try {
                        contentTypes[i + PDFfiles.size()] = Files.probeContentType(new File(files.get(i)).toPath());
                    } catch (IOException e) {
                        System.out.println("An exception occurred on item " + i + " of " + (files.size() - 1));
                        contentTypes[i + PDFfiles.size()] = "None";
                    }
                }
                DBObject metaData = new BasicDBObject();
                metaData.put("pmId", pmId);
                for (int i = 0; i < PDFfiles.size() + files.size(); i++) {
                    if (contentTypes[i] == null || contentTypes[i].equals("None")) continue;
                    ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(binary[i].getData()), fileNames[i], contentTypes[i], metaData);
                    ids.add(oid.toString());
                    log.info(ids.get(ids.size() - 1) + " : " + fileNames[i]);
                }
                String[] resourceIds = new String[ids.size()];
                for (int i = 0; i < ids.size(); i++) resourceIds[i] = ids.get(i);
                fullText.setResourceIds(resourceIds);
                Article a = articleRepository.findByPmId(fullText.getPmId());
                if (a == null) {
                    log.info("Something went wrong");
                    return false;
                }
                String oldText = toText(a);
                a.setFulltext(fullText.getTextEntry());
                log.info("Adding Item");
                UpdateResponse updateResponse = solrClientTool.add("knowledge", SolrClientTool.randomId(), a.getTitle(), a.getAuthors(), "https://aimlcoe.mskcc.org/knowledge/getPDF/" + fullText.getPmId() + ".pdf", null, fullText.getTextEntry());
                log.info(updateResponse.toString());
                Map<String, List<String>> articleMap = new HashMap<>();
                Map<String, List<String>> extraMap = new HashMap<>();
                String text = Article.toText(a);
                articleMap.put("text", Collections.singletonList(text));
                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                String id = Integer.toHexString(articleMap.hashCode()) + (a.getPmId().length() > 3 ? a.getPmId().substring(a.getPmId().length()-3) : a.getPmId());
                a.setSolrId(id);
                if (a.getPublicationDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                else if (a.getPubDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                if (a.getFulltext() != null && a.getFulltext().length() > 0)
                    extraMap.put("hasFullText", Collections.singletonList("true"));
                else
                    extraMap.put("hasFullText", Collections.singletonList("false"));
                if (!solrClientTool.exists("knowledge", Integer.toHexString(articleMap.hashCode()))) {
                    Map<String, List<String>> articleMapOld = new HashMap<>();
                    articleMapOld.put("text", Collections.singletonList(oldText));
                    articleMapOld.put("authors", Collections.singletonList(a.getAuthors()));
                    articleMapOld.put("pmid", Collections.singletonList(a.getPmId()));
                    if (a.getFulltext() != null && solrClientTool.exists("knowledge", Integer.toHexString(articleMapOld.hashCode()))) {
                        log.info("Found an old record to delete and update for {}", a.getPmId());
                        solrClientTool.delete("knowledge", Integer.toHexString(articleMapOld.hashCode()));
                    }
                    solrClientTool.add("knowledge", articleMap, extraMap);
                }

                if (a.getFulltext() == null) {
                    log.error("No fulltext correctly retrieved, possible earlier exception, like detection of possible 'zip bomb'");
                    return false;
                }

                if (a.getFulltext().length() > 500000) {
                    log.info("Trucating Article fulltext");
                    a.setFulltext(a.getFulltext().substring(0, 500000));
                }
                articleRepository.save(a);


                fullTextRepository.save(fullText);
                File directory = new File("PMC/" + pmId);
                if (directory.exists()) {
                    if (!deleteDirectory(directory)) log.error("Could not delete the folder {}", "PMC/" + pmId);
                }
                return true;
            }
        } catch (IOException|SolrServerException e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Override
    public boolean addArticle(Article article) {
        return addArticle(article.getPmId());
    }

    @Override
    public void addAllArticles() throws IOException, SolrServerException {
        Stream<Path> walk = Files.walk(Paths.get("PMC"));
        List<String> paths = walk.filter(Files::isDirectory).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        for (String path: paths) {
            if (path.equals("PMC") || path.equals("scratch")) continue;
            walk = Files.walk(Paths.get("PMC/"+path));
            List<String> files = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).collect(Collectors.toList());
            if (files.size() == 0) continue;
            walk = Files.walk(Paths.get("PMC/"+path));

            files = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).filter(s -> !s.endsWith(".tar.gz") && !s.endsWith(".pdf")).collect(Collectors.toList());
            walk = Files.walk(Paths.get("PMC/"+path));
            List<String> PDFfiles = walk.filter(p -> !Files.isDirectory(p)).map(Path::toString).filter(s -> s.endsWith(".pdf")).collect(Collectors.toList());
            if (PDFfiles.size() > 1) {
                for (int i = 0; i < PDFfiles.size(); i++) for (int j = i+1; j < PDFfiles.size(); j++) {
                    if (new File(PDFfiles.get(i)).length() == new File(PDFfiles.get(j)).length()) {
                        PDFfiles.remove(j);
                        j--;
                    }
                }
                PDFfiles.sort(Comparator.comparingInt(String::length));
                log.info(PDFfiles.toString());
            }
            Optional<FullText> opt = fullTextRepository.findById(path);
            if (opt.isEmpty()) {
                FullText fullText = new FullText();
                fullText.setPmId(path);
                Binary[] binary = new Binary[PDFfiles.size()+files.size()];
                String[] fileNames = new String[PDFfiles.size()+files.size()];
                String[] contentTypes = new String[PDFfiles.size()+files.size()];
                String[] ids = new String[PDFfiles.size()+files.size()];
                for (int i = 0; i < PDFfiles.size(); i++) {
                    binary[i] = new Binary(Files.readAllBytes(new File(PDFfiles.get(i)).toPath()));
                    fileNames[i] = PDFfiles.get(i).substring(PDFfiles.get(i).lastIndexOf("/")+1);
                    contentTypes[i] = Files.probeContentType(new File(PDFfiles.get(i)).toPath());
                    if (i == 0) {
                        log.info("Analyzing " + PDFfiles.get(0));
                        fullText.setTextEntry(TikaTool.parseDocument(PDFfiles.get(0)));
                        fullText.setHTMLEntry(TikaTool.parseDocumentHTML(PDFfiles.get(0)));
                    }
                }
                for (int i = 0; i < files.size(); i++) {
                    binary[i+PDFfiles.size()] = new Binary(Files.readAllBytes(new File(files.get(i)).toPath()));
                    fileNames[i+PDFfiles.size()] = files.get(i).substring(files.get(i).lastIndexOf("/")+1);
                    contentTypes[i+PDFfiles.size()] = Files.probeContentType(new File(files.get(i)).toPath());
                }
                DBObject metaData = new BasicDBObject();
                metaData.put("pmId", path);
                for (int i = 0; i < PDFfiles.size()+files.size(); i++) {
                    ObjectId oid = gridFsTemplate.store(new ByteArrayInputStream(binary[i].getData()), fileNames[i], contentTypes[i], metaData);
                    ids[i] = oid.toString();
                    log.info(ids[i]);
                }
                fullText.setResourceIds(ids);
                Article a = articleRepository.findByPmId(fullText.getPmId());
                if (a == null) {
                    log.error("Something went wrong");
                    throw new IOException("No database item for Article " + fullText.getPmId());
                }
                a.setFulltext(fullText.getTextEntry());
                a.setHasFullText(true);
                log.info("Adding Item");
                Map<String, List<String>> articleMap = new HashMap<>();
                Map<String, List<String>> extraMap = new HashMap<>();
                String text = Article.toText(a);
                articleMap.put("text", Collections.singletonList(text));
                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                String id = Integer.toHexString(articleMap.hashCode()) + (a.getPmId().length() > 3 ? a.getPmId().substring(a.getPmId().length()-3) : a.getPmId());
                a.setSolrId(id);
                if (a.getPublicationDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                else if (a.getPubDate() != null)
                    extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                extraMap.put("hasFullText", Collections.singletonList("true"));
                SolrDocumentList sdl = solrClientTool.find("knowledge", "pmid:"+a.getPmId());
                if (sdl.size() > 0) {
                    List<String> deleted = new ArrayList<>();
                    for (SolrDocument doc: sdl) {
                       deleted.add((String)doc.get("id"));
                    }
                    log.info("Deleting {}", deleted);
                    solrClientTool.deleteMany("knowledge", deleted);
                }
                UpdateResponse updateResponse = solrClientTool.add("knowledge", articleMap, extraMap);
                //UpdateResponse updateResponse = solrClientTool.add("knowledge", SolrClientTool.randomId(), a.getTitle(), a.getAuthors(), "https://aimlcoe.mskcc.org/knowledge/getPDF/"+fullText.getPmId()+".pdf", null, fullText.getTextEntry());
                log.info(updateResponse.toString());
                fullTextRepository.save(fullText);
                articleRepository.save(a);
                //System.out.println(fullText.getTextEntry());
            }
        }
    }

    @Override
    public void addSupplementary() {
        int pageNumber = 0;
        int pageSize = 5000;
        Page<FullText> fullTextList;
        do {
            fullTextList = fullTextRepository.findAll(PageRequest.of(pageNumber++, pageSize));
            log.info("Processing {} elements", fullTextList.getNumberOfElements());
            fullTextList.getContent().parallelStream().forEach(fullText -> {
                if (fullText.getPmId().contains("S")) return; // it's a supplementary already!
                if (fullTextRepository.existsById(fullText.getPmId() + "S"))
                    return; // already been processed into SI.
                int PDFcount = 0;
                String[] resourceIds = fullText.getResourceIds();
                // skip 0, that was the "original" PDF
                for (int i = 1; i < resourceIds.length; i++) {
                    GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[i])));
                    if (file != null) {
                        String fileName = file.getFilename();
                        if (fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx")) {
                            log.info("Supplementary PDF - Trying to save");
                            try {
                                //if (fileName.toLowerCase().endsWith(".docx")) log.info("Trying Word document: " + fileName);
                                File output = new File("PMC/scratch/" + fileName);
                                Files.write(output.toPath(), IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent()));
                                Article a = articleRepository.findByPmId(fullText.getPmId());
                                FullText fullTextS = new FullText();
                                fullTextS.setPmId(fullText.getPmId() + "S" + (PDFcount > 0 ? PDFcount + "" : ""));
                                fullTextS.setFileNames(new String[]{fileName});
                                fullTextS.setTextEntry(TikaTool.parseDocument("PMC/scratch/" + fileName));
                                fullTextS.setHTMLEntry(TikaTool.parseDocumentHTML("PMC/scratch/" + fileName));

                                fullTextS.setResourceIds(new String[]{resourceIds[i]});
                                fullTextRepository.save(fullTextS);
                                log.info("Adding supplementary " + PDFcount + " for article " + a.getPmId());
                                Map<String, List<String>> articleMap = new HashMap<>();
                                Map<String, List<String>> extraMap = new HashMap<>();
                                articleMap.put("text", Collections.singletonList(fullTextS.getTextEntry()));
                                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                                articleMap.put("pmid_supporting", Collections.singletonList(fullTextS.getPmId()));
                                if (a.getPublicationDate() != null)
                                    extraMap.put("date", Collections.singletonList(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString()));
                                else if (a.getPubDate() != null)
                                    extraMap.put("date", Collections.singletonList(a.getPubDate().toDateTime(DateTimeZone.UTC).toString()));
                                extraMap.put("hasFullText", Collections.singletonList("true"));
                                String newId = Integer.toHexString(articleMap.hashCode()) + (fullTextS.getPmId().length() > 3 ? fullTextS.getPmId().substring(fullTextS.getPmId().length()-3) : fullTextS.getPmId());

                                SolrDocumentList sdl = solrClientTool.find("knowledge", "pmid_supporting:"+fullTextS.getPmId());
                                if (sdl.size() > 0) {
                                    List<String> deleted = new ArrayList<>();
                                    for (SolrDocument doc: sdl) {
                                        deleted.add((String)doc.get("id"));
                                    }
                                    log.info("Deleting {}", deleted);
                                    solrClientTool.deleteMany("knowledge", deleted);
                                }
                                UpdateResponse updateResponse = solrClientTool.add("knowledge", articleMap, extraMap);
                                //UpdateResponse updateResponse = solrClientTool.add("knowledge", SolrClientTool.randomId(), a.getTitle() + " SUPPLEMENTARY " + PDFcount, a.getAuthors(), "https://aimlcoe.mskcc.org/knowledge/getPDF/" + fullTextS.getPmId() + ".pdf", null, fullTextS.getTextEntry());
                                log.info(updateResponse.toString());
                                if (!output.delete()) log.error("Problem deleting the temporary file {}", "PMC/scratch/" + fileName);
                                PDFcount++;
                            } catch (IOException | SolrServerException e) {
                                log.error(e.getMessage());
                            }
                        }
                    }

                }
            });
        } while (fullTextList.hasNext());
    }

    @Override
    public void addHTMLTextAll() {
        int i = 0;
        Page<FullText> articlePage;
        do {
            articlePage = fullTextRepository.findAll(PageRequest.of(i++, 5000));
            List<FullText> articles = articlePage.getContent();
            //        List<FullText> articles = fullTextRepository.findAll();
            for (FullText ft : articles) {
                if (ft.getHTMLEntry() == null) {
                    String[] resourceIds = ft.getResourceIds();
                    if (resourceIds.length == 0) {
                        log.info("No resourceIds present for document with PMID {}", ft.getPmId());
                        return;
                    }
                    GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[0])));
                    if (file != null) {
                        String fileName = file.getFilename();
                        if (fileName.toLowerCase().endsWith(".pdf")) {
                            try {
                                //if (fileName.toLowerCase().endsWith(".docx")) log.info("Trying Word document: " + fileName);
                                File output = new File("PMC/scratch/" + fileName);
                                Files.write(output.toPath(), IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent()));
                                ft.setHTMLEntry(TikaTool.parseDocumentHTML("PMC/scratch/" + fileName));
                                fullTextRepository.save(ft);
                                if (!output.delete()) {
                                    log.error("Can't delete the scratch file {}!", "PMC/scratch/" + fileName);
                                }
                            } catch (IOException e) {
                                log.error(e.getMessage());
                                return;
                            }
                        }
                    }
                }
            }
        } while (articlePage.hasNext());
    }

    @Override
    public boolean addSupplementary(String pmId) {
        if (pmId.contains("S")) {
            log.info("{} is already a supporting document", pmId);
            return false;
        }
        Optional<FullText> oft = fullTextRepository.findById(pmId);
        if (oft.isPresent()) {
            FullText fullText = oft.get();
            if (fullTextRepository.existsById(fullText.getPmId()+"S")) {
                log.info("Supporting information already processed for PMId {}", fullText.getPmId());
                return false;
            } // already had been processed into SI.
            int PDFcount = 0;
            String[] resourceIds = fullText.getResourceIds();
            // skip 0, that was the "original" PDF
            for (int i = 1; i < resourceIds.length; i++) {
                GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[i])));
                if (file != null) {
                    String fileName = file.getFilename();
                    if (fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx")) {
                        log.info("Supplementary document {} - Trying to save", fileName);
                        try {
                            //if (fileName.toLowerCase().endsWith(".docx")) log.info("Trying Word document: " + fileName);
                            File output = new File("PMC/scratch/" + fileName);
                            Files.write(output.toPath(), IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent()));
                            Article a = articleRepository.findByPmId(fullText.getPmId());
                            FullText fullTextS = new FullText();
                            fullTextS.setPmId(fullText.getPmId() + "S" + (PDFcount > 0 ? PDFcount+"" : ""));
                            fullTextS.setTextEntry(TikaTool.parseDocument("PMC/scratch/" + fileName));
                            fullTextS.setHTMLEntry(TikaTool.parseDocumentHTML("PMC/scratch/" + fileName));
                            fullTextS.setResourceIds(new String[] { resourceIds[i] });
                            fullTextRepository.save(fullTextS);
                            log.info("Adding supplementary " + PDFcount + " for article " + a.getPmId());
                            UpdateResponse updateResponse = solrClientTool.add("knowledge", SolrClientTool.randomId(), a.getTitle() + " SUPPLEMENTARY " + PDFcount, a.getAuthors(), "https://aimlcoe.mskcc.org/knowledge/getPDF/"+fullTextS.getPmId()+".pdf", null, fullTextS.getTextEntry());
                            log.info(updateResponse.toString());
                            output.delete();
                            PDFcount++;
                        } catch (IOException|SolrServerException e) {
                            log.error(e.getMessage());
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }


    public byte[] getMainPDF(String pmId) {
        if (pmId.contains("S")) {
            log.error("getMainPDF(String pmIdS) cannot take a supporting information pmId like 29948589S2, use getSupportingPDF() for this - redirecting!");
            return getSupportingPDF(pmId);
        }
        Optional<FullText> oft = fullTextRepository.findById(pmId);
        if (oft.isPresent()) {
            FullText ft = oft.get();
            String[] resourceIds = ft.getResourceIds();
            String[] fileNames = new String[ft.getResourceIds().length];
            if (ft.getFileNames() == null || ft.getFileNames().length != ft.getResourceIds().length) {
                for (int i = 0; i < resourceIds.length; i++) {
                    GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[i])));
                    if (file != null) {
                        fileNames[i] = file.getFilename();
                    }
                }
                ft.setFileNames(fileNames);
                fullTextRepository.save(ft);
            }
            if (resourceIds.length == 0) return null; // probably should delete the item, too.  Need to check elsewhere in code to prevent this.
            String resourceId = resourceIds[0];
            GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
            if (file != null) {
                try {
                    return IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent());
                } catch(IOException e) {
                    log.error(e.getMessage());
                    return null;
                }
            }
        }
        log.error("Main article {} does not exist", pmId);
        return null;
    }


    public byte[] getSupportingPDF(String pmIdS) {
        if (!pmIdS.contains("S")) {
            log.error("getSupporting(String pmIdS) must take a supporting information pmId like 29948589S2, exiting");
            return null;
        }
        Optional<FullText> oft = fullTextRepository.findById(pmIdS);
        if (oft.isEmpty()) {
            log.error("Supporting article {} does not exist", pmIdS);
            return null;
        }
        FullText ft = oft.get();
        String resourceId = ft.getResourceIds()[0];
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceId)));
        if (file != null) {
            String fileName = file.getFilename();
            if (ft.getFileNames() == null) {
                ft.setFileNames(new String[] {fileName});
                fullTextRepository.save(ft);
            }
            try {
                return IOUtils.toByteArray(gridFsTemplate.getResource(file).getContent());
            } catch(IOException e) {
                log.error(e.getMessage());
                return null;
            }
        } else return null;
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
        if (article.getFulltext() != null) sb.append(article.getFulltext());
        return sb.toString();
    }

}
