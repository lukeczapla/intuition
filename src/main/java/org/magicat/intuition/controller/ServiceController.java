package org.mskcc.knowledge.controller;

import io.swagger.annotations.Api;
import org.mskcc.knowledge.model.Article;
import org.mskcc.knowledge.pdf.Document;
import org.mskcc.knowledge.pdf.PDFHighlighter;
import org.mskcc.knowledge.pdf.Section;
import org.mskcc.knowledge.repository.ArticleRepository;
import org.mskcc.knowledge.repository.FullTextRepository;
import org.mskcc.knowledge.service.FullTextService;
import org.mskcc.knowledge.util.SpellChecking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Api("Platform for calling API Service methods")
@RestController
public class ServiceController {
    
    private final ArticleRepository articleRepository;
    private final FullTextRepository fullTextRepository;
    private final FullTextService fullTextService;
    private final GridFsTemplate gridFsTemplate;
    private final SpellChecking spellChecking;
    
    @Autowired
    public ServiceController(ArticleRepository articleRepository, FullTextRepository fullTextRepository,
                             FullTextService fullTextService, GridFsTemplate gridFsTemplate, SpellChecking spellChecking) {
        this.articleRepository = articleRepository;
        this.fullTextRepository = fullTextRepository;
        this.fullTextService = fullTextService;
        this.gridFsTemplate = gridFsTemplate;
        this.spellChecking = spellChecking;
    }
    
    @Secured("ROLE_USER")
    @RequestMapping(value = "/textArticle/{pmid}", method = RequestMethod.GET)
    public List<Section> getTextArticle(@PathVariable String pmid, @RequestParam(required = false) Boolean rebuild) throws IOException {
        Article article = articleRepository.findByPmId(pmid);
        if (article == null) return null;
        Document document = Document.readDocument(article, fullTextRepository, gridFsTemplate);
        if (document != null && rebuild != null && !rebuild) {
            if (!document.isTagsAdded()) document.addTags();
            return document.getSections();
        }
        PDFHighlighter pdfHighlighter = new PDFHighlighter();
        pdfHighlighter.setFullTextService(fullTextService);
        pdfHighlighter.analyze(article, spellChecking);
        document = pdfHighlighter.getDocument();
        document.stripArticleData();
        document.annotate();
        if (!document.isTagsAdded()) document.addTags();
        document.writeDocument(fullTextRepository, gridFsTemplate);
        return document.getSections();
    }

    @Secured("ROLE_USER")
    @RequestMapping(value = "/HTMLArticle/{pmid}", method = RequestMethod.GET)
    public String getHTMLArticle(@PathVariable String pmid, @RequestParam(required = false) Boolean rebuild) throws IOException {
        Article article = articleRepository.findByPmId(pmid);
        if (article == null) return null;
        Document document = Document.readDocument(article, fullTextRepository, gridFsTemplate);
        if (document != null && rebuild != null && !rebuild) {
            document.setArticle(article);
            return document.toHTML();
        }
        PDFHighlighter pdfHighlighter = new PDFHighlighter();
        pdfHighlighter.setFullTextService(fullTextService);
        pdfHighlighter.analyze(article, spellChecking);
        document = pdfHighlighter.getDocument();
        document.stripArticleData();
        document.annotate();
        document.writeDocument(fullTextRepository, gridFsTemplate);
        return document.toHTML();
    }
    
}
