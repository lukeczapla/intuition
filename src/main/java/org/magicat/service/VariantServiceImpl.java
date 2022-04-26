package org.magicat.service;

import lombok.*;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.magicat.model.Article;
import org.magicat.model.Variant;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.FullTextRepository;
import org.magicat.repository.VariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class VariantServiceImpl implements VariantService {

    private final static Logger log = LoggerFactory.getLogger(VariantService.class);

    private final org.magicat.service.FullTextService fullTextService;
    private final SolrService solrService;
    private final FullTextRepository fullTextRepository;
    private final VariantRepository variantRepository;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;

    @Autowired
    public VariantServiceImpl(org.magicat.service.FullTextService fullTextService, SolrService solrService, FullTextRepository fullTextRepository, VariantRepository variantRepository,
                              ArticleService articleService, ArticleRepository articleRepository) {
        this.fullTextService = fullTextService;
        this.solrService = solrService;
        this.fullTextRepository = fullTextRepository;
        this.variantRepository = variantRepository;
        this.articleService = articleService;
        this.articleRepository = articleRepository;
    }

    @Override
    public Set<String> missingFullTextArticles(List<Variant> variants) {
        if (variants == null) variants = variantRepository.findAll();
        Set<String> missingPMIds = new LinkedHashSet<>();
        for (Variant v : variants) {
            if (v.getCuratedPMIds() == null || v.getCuratedPMIds().length() == 0) continue;
            String[] pmIds = v.getCuratedPMIds().split(", ");
            for (String pmId: pmIds) {
                if (articleRepository.findByPmId(pmId) == null) {
                    if (!articleService.addArticlePMID(pmId)) missingPMIds.add(pmId);
                }
                if (!fullTextRepository.existsById(pmId)) {
                    if (!fullTextService.addArticle(pmId)) missingPMIds.add(pmId);
                }
            }
        }
        return missingPMIds;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class ArticleData implements Comparable<ArticleData> {
        private String pmId;
        private Integer score;
        private String scoreCode;
        //private Integer oldScore;
        private Integer fscore;

        private static final int lim = -2;

        @Override
        public int compareTo(@NotNull ArticleData o) {
            if (this.fscore.equals(o.fscore) && this.score.equals(o.score)) return 0;
            if (this.fscore == 0 && o.fscore == 0) return this.score - o.score;
            if (this.score >= lim && o.score >= lim && this.fscore > 0 && o.fscore > 0 && this.fscore.equals(o.fscore)) return this.score - o.score;
            if (this.score >= lim && o.score >= lim && this.fscore > 0 && o.fscore > 0) return this.fscore - o.fscore;
            if (this.score >= lim && this.fscore > 0 && o.fscore == 0) return 1;
            if (o.score >= lim && o.fscore > 0 && this.fscore == 0) return -1;
            if (this.score < lim && o.score < lim && !this.fscore.equals(o.fscore)) return this.fscore - o.fscore;
            else return this.score - o.score;
        }
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class ScoreResult implements Comparable<ScoreResult> {
        @NonNull
        private int total;
        @NonNull
        private int alterationScore;
        @NonNull
        private int functionalScore;
        @NonNull
        private int oldScore;
        private String pmId;

        @Override
        public int compareTo(@NotNull ScoreResult o) {
            if (this.total != o.total) return this.total - o.total;
            return this.oldScore - o.oldScore;
        }
    }

    private ScoreResult scoreCodeToPoints(String scoreCode, int functionalScore, int oldScore) {
        char letter;
        int total = 0;
        int geneMentions = 0, mutationTitleAbstract = 0, mutationText = 0, mutationSupporting = 0, fScore = 0;
        for (int i = 0; i < scoreCode.length(); i++) {
            if (Character.isAlphabetic(scoreCode.charAt(i))) {
                letter = scoreCode.charAt(i);
                int N = 1;
                String count = "";
                while (i+1 < scoreCode.length() && Character.isDigit(scoreCode.charAt(i+1))) {
                    count += scoreCode.charAt(i+1);
                    i++;
                }
                if (count.length() > 0) N = Integer.parseInt(count);
                switch (letter) {
                    case 'A' -> mutationText += N;
                    case 'B', 'C' -> geneMentions += N;
                    case 'E', 'F' -> mutationTitleAbstract += N;
                    case 'H' -> mutationSupporting += N;
                    default -> {}
                }
            }
        }
        if (functionalScore <= 10 && functionalScore > 1) {
            total += 5;
            fScore += 5;
        }
        if (functionalScore > 10) {
            total += 10;
            fScore += 10;
        }
        int mutationTotal = mutationSupporting + mutationTitleAbstract + mutationText;
        int mutationScore = 0;
        if (geneMentions >= 5) {
            total += 20;
        }
        int mutationValue = 1;
        if (mutationText > 0) mutationValue = 2;
        if (mutationTitleAbstract > 0) mutationValue = 3;
        // old // double mutationValue = (mutationSupporting + 2*mutationText + 3*mutationTitle) / (double)mutationTotal;
        if (mutationTotal > 0 && mutationTotal <= 5) mutationScore = mutationValue;
        else if (mutationTotal > 5 && mutationTotal <= 10) mutationScore = 2*mutationValue;
        else if (mutationTotal > 10) mutationScore = 3*mutationValue;
        //if (mutationTotal > 1) {
        //    mutationScore += 1;
        //}
        if (mutationScore == 0) mutationScore++;
        if (mutationScore >= 4) mutationScore++;
        total += mutationScore;
        return new ScoreResult(total, mutationScore, fScore, oldScore);
    }

    @Override
    public void rescoreArticlesHongxin(Variant variant) {
        if (variant.getArticlesTier1() == null || variant.getArticlesTier1().size() == 0) return;
        //log.info("Rescoring {}", variant.getDescriptor());
        if (variant.getScores1() == null || variant.getScoreCode1() == null) {
            log.error("ERROR ON VARIANT {} TIER1: null Scores1 or ScoreCode1", variant.getDescriptor());
            return;
        }
        if (variant.getArticlesTier1().size() != variant.getScores1().size() || variant.getArticlesTier1().size() != variant.getScoreCode1().size()) {
            log.error("ERROR ON VARIANT {} TIER1: sizes", variant.getDescriptor());
            return;
        }
        List<ScoreResult> results = new ArrayList<>();
        for (int i = 0; i < variant.getArticlesTier1().size(); i++) {
            int oldScore = variant.getScores1().get(i) + variant.getKeywordScores().get(i);
            ScoreResult score = scoreCodeToPoints(variant.getScoreCode1().get(i), variant.getKeywordScores().get(i), oldScore);
            score.setPmId(variant.getArticlesTier1().get(i));
//            Variant.NewScore item = new Variant.NewScore(score.getTotal(), score.getAlterationScore(), score.getFunctionalScore(), variant.getArticlesTier1().get(i));
            results.add(score);
        }
        if (variant.getArticlesTier2() != null && variant.getArticlesTier2().size() > 0) {
            if (variant.getScores2() == null || variant.getScoreCode2() == null) {
                log.error("ERROR ON VARIANT {} TIER2: null Scores2 or ScoreCode2", variant.getDescriptor());
                return;
            }
            if (variant.getArticlesTier2().size() != variant.getScores2().size() || variant.getArticlesTier2().size() != variant.getScoreCode2().size()) {
                log.error("ERROR ON VARIANT {} TIER2: sizes", variant.getDescriptor());
                return;
            }
            for (int i = 0; i < variant.getArticlesTier2().size(); i++) {
                int oldScore = variant.getScores2().get(i) + variant.getKeywordScores().get(i+5);
                ScoreResult score = scoreCodeToPoints(variant.getScoreCode2().get(i), variant.getKeywordScores().get(i+5), oldScore);
                score.setPmId(variant.getArticlesTier2().get(i));
//                Variant.NewScore item = new Variant.NewScore(score.getTotal(), score.getAlterationScore(), score.getFunctionalScore(), variant.getArticlesTier2().get(i));
                results.add(score);
            }
        }
        Collections.sort(results);
        Collections.reverse(results);
        List<Variant.NewScore> newScores = new ArrayList<>();
        for (ScoreResult r : results) {
            newScores.add(new Variant.NewScore(r.getTotal(), r.getAlterationScore(), r.getFunctionalScore(), r.getPmId()));
        }
        variant.setNewScores(newScores);
        variantRepository.save(variant);
    }

    @Override
    public void rescoreArticlesHongxin(List<Variant> variants) {
        variants.parallelStream().forEach(this::rescoreArticlesHongxin);
    }

    @Override
    public void rescoreArticles(Variant variant) {
        if (variant.getArticlesTier1() == null || variant.getArticlesTier1().size() == 0) return;
        //log.info("Rescoring {}", variant.getDescriptor());
        if (variant.getScores1() == null || variant.getScoreCode1() == null) {
            log.error("ERROR ON VARIANT {} TIER1: null Scores1 or ScoreCode1", variant.getDescriptor());
            return;
        }
        if (variant.getArticlesTier1().size() != variant.getScores1().size() || variant.getArticlesTier1().size() != variant.getScoreCode1().size()) {
            log.error("ERROR ON VARIANT {} TIER1: sizes", variant.getDescriptor());
            return;
        }
        List<ArticleData> articleData = new ArrayList<>();
        for (int i = 0; i < variant.getArticlesTier1().size(); i++) {
            articleData.add(new ArticleData(variant.getArticlesTier1().get(i), variant.getScores1().get(i), variant.getScoreCode1().get(i), variant.getKeywordScores().get(i)));
        }
        if (variant.getArticlesTier2() != null && variant.getArticlesTier2().size() > 0) {
            if (variant.getScores2() == null || variant.getScoreCode2() == null) {
                log.error("ERROR ON VARIANT {} TIER2: null Scores2 or ScoreCode2", variant.getDescriptor());
                return;
            }
            if (variant.getArticlesTier2().size() != variant.getScores2().size() || variant.getArticlesTier2().size() != variant.getScoreCode2().size()) {
                log.error("ERROR ON VARIANT {} TIER2: sizes", variant.getDescriptor());
                return;
            }
            for (int i = 0; i < variant.getArticlesTier2().size(); i++) {
                articleData.add(new ArticleData(variant.getArticlesTier2().get(i), variant.getScores2().get(i), variant.getScoreCode2().get(i), variant.getKeywordScores().get(i+5)));
            }
        }
        Collections.sort(articleData);
        Collections.reverse(articleData);

        //for (ArticleData i : articleData) {
        //    System.out.printf("%s %d %s %d\n", i.pmId, i.score, i.scoreCode, i.fscore);
        //}

        for (int i = 0; i < articleData.size(); i++) {
            if (i < 5) {
                variant.getArticlesTier1().set(i, articleData.get(i).pmId);
                variant.getScores1().set(i, articleData.get(i).score);
                variant.getScoreCode1().set(i, articleData.get(i).scoreCode);
            } else {
                variant.getArticlesTier2().set(i-5, articleData.get(i).pmId);
                variant.getScores2().set(i-5, articleData.get(i).score);
                variant.getScoreCode2().set(i-5, articleData.get(i).scoreCode);
            }
            variant.getKeywordScores().set(i, articleData.get(i).fscore);
        }
        variantRepository.save(variant);

    }

    @Override
    public void rescoreArticles(List<Variant> variants) {
        variants.parallelStream().forEach(this::rescoreArticles);
    }

    @Override
    public byte[] createVariantSpreadsheet(List<Variant> variants) {
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

        int rowCount = 1;
        for (Variant v: variants) {
            row = firstSheet.createRow(rowCount);
            cell = row.createCell(0);
            cell.setCellValue(v.getGene());
            cell = row.createCell(1);
            cell.setCellValue(v.getMutation());
            cell = row.createCell(2);
            if (v.getCancerTypes() != null) cell.setCellValue(v.getCancerTypes());
            cell = row.createCell(3);
            if (v.getDrugs() != null) cell.setCellValue(v.getDrugs());
            cell = row.createCell(5);
            if (v.getCuratedPMIds() != null) cell.setCellValue(v.getCuratedPMIds());
            cell = row.createCell(6);
            if (v.getAutomatedPMIds() != null) cell.setCellValue(v.getAutomatedPMIds());
            cell = row.createCell(7);
            if (v.getArticleURLs() != null) cell.setCellValue(v.getArticleURLs());
            cell = row.createCell(8);
            if (v.getArticlePages() != null) cell.setCellValue(v.getArticlePages());
            cell = row.createCell(9);
            if (v.getConsensusPMIds() != null) cell.setCellValue(v.getConsensusPMIds());
            cell = row.createCell(10);
            if (v.getArticlesTier1() != null) {
                String result = "";
                for (String pmid: v.getArticlesTier1()) {
                    if (result.equals("")) result = pmid;
                    else result += ", " + pmid;
                }
                cell.setCellValue(result);
            }
            cell = row.createCell(11);
            if (v.getArticlesTier2() != null) {
                String result = "";
                for (String pmid: v.getArticlesTier2()) {
                    if (result.equals("")) result = pmid;
                    else result += ", " + pmid;
                }
                cell.setCellValue(result);
            }
            cell = row.createCell(12);
            if (v.getScoreCode1() != null) {
                String result = "";
                for (String score: v.getScoreCode1()) {
                    if (result.equals("")) result = score;
                    else result += ", " + score;
                }
                cell.setCellValue(result);
            }
            cell = row.createCell(13);
            if (v.getScoreCode2() != null) {
                String result = "";
                for (String score: v.getScoreCode2()) {
                    if (result.equals("")) result = score;
                    else result += ", " + score;
                }
                cell.setCellValue(result);
            }
            rowCount++;
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

    @Override
    public void getHumanCuratedPDFs(List<Variant> variants) {
        Set<String> PMIDs = new LinkedHashSet<>();
        for (Variant variant : variants) {
            if (variant.getCuratedPMIds() != null && variant.getCuratedPMIds().length() > 5) {
                PMIDs.addAll(Arrays.asList(variant.getCuratedPMIds().split(", ")));
            }
        }
        log.info("Analyzing all of total {} PMIDs", PMIDs.size());
        PMIDs.parallelStream().forEach(pmid -> {
            if (pmid.equals("20301399") || pmid.equals("20556892")) return;
            Article a;
            if ((a = articleRepository.findByPmId(pmid)) != null) {
                if (a.getFulltext() == null || a.getFulltext().equals("")) {
                    if (fullTextService.addArticle(pmid)) {
                        log.info("Added PDF for article {}", pmid);
                    } else {
                        log.info("No PDF for {}", pmid);
                        a.setHasFullText(false);
                        a.setLastChecked(DateTime.now());
                    }
                }
            } else {
                if (articleService.addArticlePMID(pmid)) {
                    log.info("Added missing PMID {} to the Article database", pmid);
                    a = articleRepository.findByPmId(pmid);
                    if (fullTextService.addArticle(pmid)) {
                        log.info("Added PDF for article {}", pmid);
                    } else {
                        a.setHasFullText(false);
                        a.setLastChecked(DateTime.now());
                    }
                } else {
                    log.error("Could not add missing PMID {} to the Article database", pmid);
                }
            }
        });
    }

    @Override
    public void getArticlePDFs(int limit, String searchTerm) {
        SolrService.SearchResult searchResult = solrService.searchSolr(limit, searchTerm, null, true);
        List<String> PMIDs = searchResult.getPmIds();
        log.info("Analyzing all of total {} PMIDs", PMIDs.size());
        PMIDs.parallelStream().forEach(pmid -> {
            if (pmid.contains("S")) return;
            //if (pmid.equals("20301399") || pmid.equals("20556892")) return;
            Article a;
            if ((a = articleRepository.findByPmId(pmid)) != null) {
                if (a.getFulltext() == null || a.getFulltext().equals("")) {
                    if (fullTextService.addArticle(pmid)) {
                        log.info("Added PDF for article {}", pmid);
                    } else {
                        log.info("No PDF for {}", pmid);
                        a.setHasFullText(false);
                        a.setLastChecked(DateTime.now());
                    }
                }
            } else {
                if (articleService.addArticlePMID(pmid)) {
                    log.info("Added missing PMID {} to the Article database", pmid);
                    a = articleRepository.findByPmId(pmid);
                    if (fullTextService.addArticle(pmid)) {
                        log.info("Added PDF for article {}", pmid);
                    } else {
                        a.setHasFullText(false);
                        a.setLastChecked(DateTime.now());
                    }
                } else {
                    log.error("Could not add missing PMID {} to the Article database", pmid);
                }
            }
        });

    }
}
