package org.mskcc.knowledge.controller;

import org.mskcc.knowledge.model.Article;
import org.mskcc.knowledge.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@RestController
public class TestController {

    ArticleRepository articleRepository;

    @Autowired
    public TestController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = "/validateDates", method = RequestMethod.GET)
    public String validateDates() {
        final int pageLimit = 50;
        int pageNumber = 0;
        Page<Article> pages;
        StringBuilder result = new StringBuilder(1000);
        for (int i = 0; i < 20; i++) {
            pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
            long t = pages.stream().parallel().filter((a) -> a.getPublicationDate() != null).count();
            pages.stream().filter((a) -> a.getPublicationDate() != null).forEach((a) -> result.append(a.toString() + "\n"));
            result.append(t + " articles\n");
        }
        return result.toString();
    }

}
