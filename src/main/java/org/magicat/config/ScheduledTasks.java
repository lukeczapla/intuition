package org.magicat.config;

import org.joda.time.DateTime;
import org.magicat.model.GlobalTimestamp;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.GlobalTimestampRepository;
import org.magicat.service.ArticleService;
import org.magicat.service.MapService;
import org.magicat.service.SolrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScheduledTasks {

    private final static Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final MapService mapService;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final GlobalTimestampRepository globalTimestampRepository;
    private final SolrService solrService;

    @Autowired
    public ScheduledTasks(MapService mapService, ArticleService articleService, ArticleRepository articleRepository, GlobalTimestampRepository globalTimestampRepository, SolrService solrService) {
        this.mapService = mapService;
        this.articleService = articleService;
        this.articleRepository = articleRepository;
        this.globalTimestampRepository = globalTimestampRepository;
        this.solrService = solrService;
    }

    private final Counter counter = new Counter();

    @Scheduled(fixedDelay = 1000*3600L)
    public void reportCurrentTime() {
        log.info("The time is now {}", DateTime.now());
    }
/*
    @Scheduled(fixedDelay = 100*3600L)
    public void indexArticles() {
        Optional<GlobalTimestamp> ogt = globalTimestampRepository.findById("maps");
        if (ogt.isPresent() && ogt.get().getAfter().isBeforeNow()) {
            log.info("{} - Time to make the donuts!", DateTime.now());
            new Thread(() -> counter.x = mapService.updateMapsAll(counter.x)).start();
        }
    }
*/

    @Scheduled(fixedDelay = 3000*3600)
    public void checkArticles() {
        Optional<GlobalTimestamp> ogt = globalTimestampRepository.findById("articles");
        if (ogt.isPresent() && ogt.get().getAfter().isBeforeNow()) {
            log.info("{} - The aliens are landing! (UPDATING ARTICLES)", DateTime.now());
            new Thread(() -> {
                GlobalTimestamp gt = ogt.get();
                int pageNumber = 0;
                if (gt.getNotes() != null) {
                    String notes = gt.getNotes();
                    pageNumber = Integer.parseInt(notes.split(" ")[0])/50000;
                    log.info("Got notes {}, page number is {}", notes, pageNumber);
                } else {
                    pageNumber = (int)(articleRepository.count() / 50000);
                }
                long oldTotal = articleRepository.count();
                gt.setAfter(DateTime.now().plusDays(2));
                globalTimestampRepository.save(gt);
                articleService.updateArticlesPubmedRecent();
                gt.setNotes(articleRepository.count() + " articles now in system, added " + (articleRepository.count()-oldTotal));
                globalTimestampRepository.save(gt);
                articleService.updateCitations(pageNumber, 50000);
                articleService.addCitationRecords(pageNumber, 50000);
                solrService.updateSolrArticles(pageNumber, 50000);
            }).start();
        }
    }


    static class Counter {
        public int x;

        public Counter() {
            x = 0;
        }
    }

}
