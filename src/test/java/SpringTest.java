import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.magicat.MIND.SimulationMIND;
import org.magicat.config.ScheduledTasks;
import org.magicat.model.Article;
import org.magicat.model.FullText;
import org.magicat.model.GlobalTimestamp;
import org.magicat.model.xml.UpdateConfig;
import org.magicat.model.xml.UpdateItems;
import org.magicat.montecarlo.MCNetwork;
import org.magicat.repository.*;
import org.magicat.service.*;
import org.magicat.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = org.magicat.Startup.class)
public class SpringTest {

    @Autowired
    MapService mapService;
    @Autowired
    ArticleService articleService;
    @Autowired
    GlobalTimestampRepository globalTimestampRepository;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    ArticleIndexRepository articleIndexRepository;
    @Autowired
    FullTextRepository fullTextRepository;
    @Autowired
    GeneMapRepository geneMapRepository;
    @Autowired
    MutationMapRepository mutationMapRepository;
    @Autowired
    DrugMapRepository drugMapRepository;
    @Autowired
    CancerMapRepository cancerMapRepository;
    @Autowired
    ArticleIndexService articleIndexService;
    @Autowired
    AnalyticsService analyticsService;
    @Autowired
    SolrService solrService;
    @Autowired
    SolrClientTool solrClientTool;
    @Autowired
    SimulationMIND simulationMIND;
    @Autowired
    GridFsTemplate gridFsTemplate;

    private XMLParser parser;
    private List<Article> processArticles = new ArrayList<>();
    static int runCount = 0;

    @Test
    UpdateConfig readConfig() {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            UpdateConfig uc = (UpdateConfig)jaxbUnmarshaller.unmarshal(new File("UpdateConfig.xml"));
            return uc;
        }
        catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void writeConfig() throws JAXBException {
        long itemCount = articleRepository.count();
        UpdateConfig uc = UpdateConfig.builder().genes(true).mutations(true)
                .cancers(true).drugs(true).page((int)(itemCount/10000)).pageSize(10000).complete(true).build();
        JAXBContext jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(uc, new File("UpdateConfig.xml"));
        marshaller.marshal(uc, System.out);
    }



    @Test
    void runUpdateMaps() {
        UpdateConfig uc = readConfig();
        mapService.updateMaps(uc);
    }

    @Test
    void runFullUpdate() {
        ScheduledTasks.updateArticles = false;
        articleService.setRelevance(true);
        int pageNumber = (int)(articleRepository.count() / 50000);
        articleService.updateArticlesPubmed();
        articleService.updateCitations(pageNumber, 50000);
        articleService.addCitationRecords(pageNumber, 50000);
        solrService.updateSolrArticles(pageNumber, 50000);
    }

    @Test
    void followupCitations() {
        ScheduledTasks.updateArticles = false;
        int pageNumber = 222;
        articleService.updateCitations(pageNumber, 50000);
        articleService.addCitationRecords(pageNumber, 50000);
        solrService.updateSolrArticles(pageNumber, 50000);
    }


    @Test
    void updateCitations() {
        final int pageLimit = 50000;
        int pageNumber = 173;
        Page<Article> pages;
        do {
            pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
            processArticles(pages.getContent());
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
                if (parser == null) {
                    parser = new XMLParser("pubmed_list.xml");
                } else {
                    parser.reload("pubmed_list.xml");
                }
                parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
            } catch (IOException|NoSuchFieldException|IllegalAccessException|ParserConfigurationException|SAXException e) {
                log.error("Error occurred: " + e.getMessage());
            }
        }
    }

    @Test
    void processArticles(List<Article> articles) {
        for (Article article: articles) {
            if (article.getTitle() == null && article.getCitation() != null) {
                processArticles.add(article);
                if (processArticles.size() > 199) {
                    StringBuilder items = new StringBuilder();
                    for (int i = 0; i < processArticles.size(); i++) {
                        if (i == 0) items.append(processArticles.get(i).getPmId().trim());
                        else items.append(",").append(processArticles.get(i).getPmId().trim());
                    }
                    try {
                        if (runCount == 0) {
                            ProcessUtil.runScript("python3 python/pubmed_list.py " + items.toString());
                            setupProcess();
                            parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
                        } else {
                            ProcessUtil.runScript("python3 python/pubmed_list.py " + items.toString());
                            parser.reload("pubmed_list.xml");
                            parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
                        }
                        runCount++;
                    } catch (Exception e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
                    processArticles = new ArrayList<>();
                }
            }
        }
    }

    @Test
    void setupProcess() {
        try {
            parser = new XMLParser("pubmed_list.xml");
            {
                final int pageLimit = 50000;
                int pageNumber = 0;
                Page<Article> pages;
                do {
                    pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
                    parser.setDb(pages.getContent());
                } while (pages.hasNext());
                log.info("Total items: " + pages.getTotalElements());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        parser.setArticleRepository(articleRepository);
    }

    @Test
    public void reindexArticles() {
        articleIndexService.reindexAllArticles();
    }

    @Test
    public void indexArticles() { articleIndexService.indexAllArticles(); }

    @Test
    public void indexNewArticles() {
        UpdateConfig updateConfig = readConfig();
        articleIndexService.indexNewArticles(updateConfig);
    }

    @Test
    public void updateSolrFullTextArticles() {
        solrService.updateSolrFullTextArticles();
    }

    @Test
    public void updateSolrSupportingInformation() {
        solrService.updateSolrSupportingInformation();
    }

    @Test
    public void searchSolr() throws IOException, SolrServerException {
        SolrDocumentList sdl = solrClientTool.find("knowledge", "attr_content:tp53");
        log.info(sdl.size() + " documents");
        /*for (int i = 0; i < sdl.size(); i++) {
            for (String name: sdl.get(i).getFieldNames())
              log.info(name + ": " +sdl.get(i).getFieldValue(name));
            log.info("NEXT DOCUMENT:");
        }*/
        log.info(sdl.getMaxScore().toString());
    }

    @Test
    public void updateSolrFulltextCarrot2() {
        log.info("Running Solr Fulltext update for Carrot2 with fileurls");
        UpdateConfig uc = readConfig();
        log.info(new SystemInfo().memInfo());
        int i = 0;
        Page<FullText> page;
        do {
            page = fullTextRepository.findAll(PageRequest.of(i++, 10000));
            List<FullText> articles = page.getContent();
            //Collections.shuffle(articles);  // just checking the order here for a demonstration
            log.info(articles.size() + " items");
            log.info(new SystemInfo().memInfo());
            articles.parallelStream().forEach(a -> {
                Article article;
                if (a.getPmId().contains("S")) {
                    article = articleRepository.findByPmId(a.getPmId().substring(0, a.getPmId().indexOf("S")));
                    article.setTitle(article.getTitle() + ": Supporting Document " + a.getPmId().substring(a.getPmId().indexOf("S")));
                } else article = articleRepository.findByPmId(a.getPmId());
                try {
                    String fileURL = "https://aimlcoe.mskcc.org/knowledge/getPDF/" + a.getPmId() + ".pdf";
                    if (solrClientTool.find("knowledge", "attr_fileurl:\"" + fileURL + "\"").size() == 0) {
                        log.info("Adding fulltext for " + a.getPmId());
                        solrClientTool.add("knowledge", SolrClientTool.randomId(), article.getTitle(), article.getAuthors(), fileURL, null, a.getTextEntry());
                    } else log.info("Already exists: " + a.getPmId());
                } catch (SolrServerException | IOException e) {
                    e.printStackTrace();
                }
            });
        } while (page.hasNext());
    }

    @Test
    public void refreshCarrot2Core() {
        solrClientTool.refreshCollection("knowledge");
    }

    @Test
    public void copyFulltext() {
        List<FullText> fullTexts = fullTextRepository.findAll();
        fullTexts.parallelStream().filter(ft -> !ft.getPmId().contains("S")).forEach(ft -> {
           Article a = articleRepository.findByPmId(ft.getPmId());
           if (a.getFulltext() == null) {
               log.info("Saving " + a.getPmId());
               a.setFulltext(ft.getTextEntry());
               articleRepository.save(a);
           } else log.info(a.getPmId() + " already has fulltext");
        });
    }

    @Test
    public void updateSolr() {
        //solrService.addArticle("27880995");
        solrService.updateSolrArticles(0, 50000);
    }

    @Test
    public void addCitationRecords() {
        int page = 0;
        int pageSize = 50000;
        Page<Article> pages;
        List<Article> articleList;
        do {
            pages = articleRepository.findAll(PageRequest.of(page, pageSize));
            articleList = pages.getContent();
            log.info("Total articles in page " + page + ": " + articleList.size());
            for (Article article : articleList) {
                if (article.getCitation() == null) {
                    log.info("Updating citation for " + article.getPmId());
                    articleService.addCitationQueue(article, articleList, 200);
                }
            }
            page++;
        } while (pages.hasNext());
        articleService.addCitationQueue(null, articleList, 200);
    }

    @Test
    public void pullDaily() {
        ScheduledTasks.updateArticles = false;
        Optional<GlobalTimestamp> ogt = globalTimestampRepository.findById("articles");
        if (ogt.isEmpty()) {
            log.error("BUMMER, gotta bounce there's an error in the daily pull!");
            return;
        }
        GlobalTimestamp gt = ogt.get();
        int pageNumber = 0;
        if (gt.getNotes() != null) {
            String notes = gt.getNotes();
            pageNumber = Integer.parseInt(notes.split(" ")[0])/50000;
            log.info("Got notes {}, page number is {}", notes, pageNumber);
        } else {
            pageNumber = (int)(articleRepository.count() / 50000);
        }
        log.info("We are on page number {} of page size {}", pageNumber, 50000);
        long oldTotal = articleRepository.count();
        gt.setAfter(DateTime.now().plusDays(2));
        globalTimestampRepository.save(gt);
        articleService.updateArticlesPubmedRecent();
        log.info("Recording notes: {} articles now in system, added {}", articleRepository.count(),  (articleRepository.count()-oldTotal));
        gt.setNotes(articleRepository.count() + " articles now in system, added " + (articleRepository.count()-oldTotal) + " articles");
        globalTimestampRepository.save(gt);
        log.info("Updating page {} of pageSize {}", pageNumber, 50000);
        articleService.updateCitations(pageNumber, 50000);
        articleService.addCitationRecords(pageNumber, 50000);
        solrService.updateSolrArticles(pageNumber, 50000);
    }

    @Test
    public void addWilma() {
        String[] pmids = {"26657898", "24926260", "30398411", "33507258", "22510884", "26732095", "30683711", "29899858"};
        List<Article> articles = new ArrayList<>();
        List<FullText> fullTexts = new ArrayList<>();
        for (String pmid: pmids) {
            articles.add(articleRepository.findByPmId(pmid));
            fullTexts.add(fullTextRepository.findFullTextFor(pmid));
        }
        Gson gson = new Gson();
        System.out.println(gson.toJson(articles));
        System.out.println(gson.toJson(fullTexts));
        /*Page<Article> page = articleRepository.findAllFullText(PageRequest.of(0, 1000));
        System.out.println(page.getTotalElements());

        //do {
        page = articleRepository.findAllFullText(PageRequest.of(0, 1000));
        List<Article> articles = page.getContent();
        System.out.println(new Gson().toJson(articles.toArray()));*/
        /*   articles.parallelStream().forEach(a -> {
               FullText fts = fullTextRepository.findFullTextFor(a.getPmId());
               if (fts == null) {
                   a.setHasFullText(false);
                   a.setFulltext(null);
                   log.info("Removing fulltext and updating solr for {}", a.getPmId());
                   solrService.addArticle(a.getPmId());
                   articleRepository.save(a);
               }
            });*/
        //} while (i >= 0);
        //System.out.println(new Gson().toJson(articles.toArray()));

        //articleService.runSearch("Wilma Olson", 2000, 0);
        //articleService.runSearch("DNA structural biology", 2000, 1);
        //articleService.updateCitations(177, 50000);
        //articleService.addCitationRecords(177, 50000);
        //solrService.updateSolrArticles();
    }

    @Test
    public void processSpreadsheet() {
        String fileName = System.getProperty("filename", "oncokb_braf_tp53_ros1_pmids.xlsx");
        String outputFileName = System.getProperty("outputFileName", "OncoKB-out.xlsx");
        log.info(fileName + " and output " + outputFileName);
        analyticsService.processSpreadsheet(fileName);
    }

    @Test
    void updateRecentArticles() {
        articleService.updateArticlesPubmedRecent();
    }

    @Test
    public void purgeMissingFullText() {
        List<FullText> missingItems = fullTextRepository.findAllMissingFulltext();
        missingItems.parallelStream().forEach(ft -> {
            String[] resourceIds = ft.getResourceIds();
            for (String resourceId: resourceIds) {
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(resourceId)));
            }
            List<FullText> supplementary = fullTextRepository.findAllSupplementaryFor(ft.getPmId());
            supplementary.forEach(ftS -> {
                solrService.deleteArticle(ftS.getPmId());
                fullTextRepository.delete(ftS);
            });
            fullTextRepository.delete(ft);
        });
    }

    /*
    @Test
    void updateAllGeneSearches() {
        List<String> genes = Arrays.asList("P21,P53,ABL1,AKT1,ALK,AMER1,APC,AR,ARID1A,ASXL1,ATM,ATRX,AXIN1,BAP1,BCL2,BCOR,BRAF,BRCA1,BRCA2,CARD11,CBL,CDC73,CDH1,CDKN2A,CEBPA,CIC,CREBBP,CTNNB1,DAXX,DNMT3A,EGFR,EP300,ERBB2,EZH2,FBXW7,FGFR2,FGFR3,FLT3,FOXL2,GATA3,GNA11,GNAQ,GNAS,HNF1A,HRAS,IDH1,IDH2,JAK1,JAK2,JAK3,KDM5C,KDM6A,KIT,KMT2D,KRAS,MAP2K1,MAP3K1,MED12,MEN1,MET,MLH1,MPL,MSH2,MSH6,MYD88,NF1,NF2,NFE2L2,NOTCH1,NOTCH2,NPM1,NRAS,PAX5,PBRM1,PDGFRA,PIK3CA,PIK3R1,PPP2R1A,PRDM1,PTCH1,PTEN,PTPN11,RB1,RET,RNF43,SETD2,SF3B1,SMAD2,SMAD4,SMARCA4,SMARCB1,SMO,SOCS1,SPOP,STAG2,STK11,TET2,TNFAIP3,TP53,TSC1,U2AF1,VHL,WT1,AKT2,ARID2,ATR,B2M,BARD1,BCL6,BRD4,BRIP1,BTK,CALR,CASP8,CBFB,CCND1,CCND2,CCND3,CCNE1,CD274,CD79A,CD79B,CDK12,CDK4,CDK6,CDKN1B,CDKN2C,CHEK2,CRLF2,CSF1R,CSF3R,CTCF,CXCR4,DDR2,ERBB3,ERBB4,ERG,ESR1,ETV6,FANCA,FANCC,FGFR1,FGFR4,FLCN,FUBP1,GATA1,GATA2,H3-3A,H3C2,IKZF1,IRF4,JUN,KDM5A,KDR,KEAP1,KMT2A,KMT2C,MAP2K2,MAP2K4,MAPK1,MDM2,MDM4,MITF,MTOR,MUTYH,MYC,MYCL,MYCN,NKX2-1,NSD2,NSD3,NTRK1,NTRK3,PALB2,PDCD1LG2,PDGFRB,PHF6,PIM1,PRKAR1A,RAD21,RAF1,RARA,ROS1,RUNX1,SDHA,SDHB,SDHC,SDHD,SOX2,SPEN,SRC,SRSF2,STAT3,SUFU,SYK,TENT5C,TGFBR2,TMPRSS2,TNFRSF14,TSC2,TSHR,XPO1,AKT3,ARAF,ARID1B,AURKA,AURKB,AXL,BCL10,BCORL1,BCR,BIRC3,BLM,BTG1,CDK8,CDKN2B,CHEK1,CRKL,CYLD,DOT1L,EED,EIF4A2,EPHA3,EPHB1,ERCC4,ETV1,FAS,FGF19,FGF3,FGF4,FH,FLT1,FLT4,FOXO1,FOXP1,GRIN2A,GSK3B,HGF,ID3,IGF1R,IKBKE,IL7R,INPP4B,IRS2,KLF4,LMO1,MALT1,MAP3K13,MCL1,MEF2B,MRE11,MSH3,MSI2,NBN,NCOR1,NFKBIA,NSD1,NT5C2,NTRK2,P2RY8,PDCD1,PIK3CB,PMS2,POLD1,POLE,POT1,PPARG,RAC1,RAD51,RAD51B,RBM10,REL,RHOA,RICTOR,RPTOR,SETBP1,SOX9,STAT5B,SUZ12,TBX3,TCF3,TERT,TET1,TOP1,TP63,TRAF7,ZRSR2,ACVR1,ALOX12B,AXIN2,BCL11B,BCL2L1,BMPR1A,CDKN1A,CIITA,CUL3,CUX1,DDX3X,DICER1,DIS3,DNAJB1,DNMT1,DROSHA,EPAS1,EPHA5,EPHA7,ERCC2,ERCC3,ERCC5,ERRFI1,ETV4,ETV5,EWSR1,FANCD2,FAT1,FBXO11,FOXA1,GLI1,GNA13,H1-2,H3-3B,HDAC1,HLA-A,INHBA,LATS1,LATS2,LYN,MAF,MAP3K14,MAX,MLLT1,MST1R,MYOD1,NCOR2,NOTCH3,NUP93,PARP1,PHOX2B,PIK3C2G,PIK3CG,PIK3R2,PLCG2,PPM1D,PPP6C,PREX2,PRKCI,PRKN,PTPRT,RAD50,RAD51C,RAD51D,RAD52,RAD54L,RECQL4,RUNX1T1,SDHAF2,SGK1,SH2B3,SMAD3,SMARCD1,STAT5A,STAT6,TBL1XR1,TCF7L2,TEK,TMEM127,TRAF2,VEGFA,WWTR1,XRCC2,ZFHX3,ABL2,ABRAXAS1,AGO2,ANKRD11,ARID5B,ASXL2,ATF1,BABAM1,BBC3,BCL2L11,BCL9,CARM1,CCNQ,CD276,CD58,CDC42,CENPA,COP1,CSDE1,CTLA4,CYSLTR2,DCUN1D1,DDIT3,DEK,DNMT3B,DTX1,DUSP22,DUSP4,E2F3,EGFL7,EIF1AX,EIF4E,ELF3,ELOC,EPCAM,ERF,ETNK1,EZH1,FANCG,FANCL,FEV,FLI1,FYN,GNA12,GNB1,GPS2,GREM1,H1-3,H1-4,H2AC11,H2AC16,H2AC17,H2AC6,H2BC11,H2BC12,H2BC17,H2BC4,H2BC5,H3-4,H3-5,H3C1,H3C10,H3C11,H3C12,H3C13,H3C14,H3C3,H3C4,H3C6,H3C7,H3C8,HDAC4,HDAC7,HIF1A,HLA-B,HOXB13,ICOSLG,IFNGR1,IGF1,IGF2,IKZF3,IL10,INHA,INPP4A,INPPL1,INSR,IRF1,IRF8,IRS1,JARID2,KAT6A,KMT2B,KMT5A,KNSTRN,LCK,LMO2,LZTR1,MAFB,MAPK3,MAPKAP1,MDC1,MECOM,MGA,MLLT10,MSI1,MST1,MTAP,MYB,NCOA3,NCSTN,NEGR1,NKX3-1,NOTCH4,NR4A3,NTHL1,NUF2,NUP98,NUTM1,PAK1,PAK5,PCBP1,PDGFB,PDPK1,PGR,PIK3C3,PIK3CD,PIK3R3,PLCG1,PLK2,PMAIP1,PMS1,PNRC1,PPP4R2,PRDM14,PRKD1,PTP4A1,PTPN2,PTPRD,PTPRS,RAB35,RAC2,RASA1,RBM15,RECQL,RHEB,RIT1,RPS6KA4,RPS6KB2,RRAGC,RRAS,RRAS2,RTEL1,RXRA,RYBP,SESN1,SESN2,SESN3,SETDB1,SH2D1A,SHOC2,SHQ1,SLX4,SMARCE1,SMC1A,SMC3,SMYD3,SOS1,SOX17,SPRED1,SS18,STK19,STK40,TAL1,TAP1,TAP2,TCL1A,TFE3,TGFBR1,TLX1,TLX3,TP53BP1,TRAF3,TRAF5,TYK2,U2AF2,UBR5,UPF1,USP8,VTCN1,XBP1,XIAP,YAP1,YES1,ABI1,ACTG1,ACVR1B,AFDN,AFF1,AFF4,AGO1,ALB,APLNR,ARFRP1,ARHGAP26,ARHGAP35,ARHGEF12,ARHGEF28,ARID3A,ARID3B,ARID3C,ARID4A,ARID4B,ARID5A,ARNT,ATIC,ATP6AP1,ATP6V1B2,ATXN2,ATXN7,BACH2,BCL11A,BCL2L2,BCL3,BCL7A,BTG2,CAMTA1,CARS1,CBFA2T3,CD22,CD28,CD70,CD74,CDX2,CEP43,CLTC,CLTCL1,CMTR2,CNTRL,COL1A1,CRBN,CREB1,CREB3L1,CREB3L2,CTNNA1,CTR9,CYP19A1,DDX10,DDX6,DNM2,EBF1,ECT2L,EGR1,ELF4,ELL,EML4,EMSY,EP400,EPOR,EPS15,ESCO2,ETAA1,EZHIP,EZR,FANCE,FANCF,FCGR2B,FCRL4,FGF10,FGF14,FGF23,FGF6,FHIT,FOXF1,FOXO3,FOXO4,FSTL3,FURIN,FUS,GAB1,GAB2,GAS7,GID4,GPHN,GTF2I,H1-5,H2BC8,H4C9,HERPUD1,HEY1,HIP1,HLA-C,HLF,HMGA1,HMGA2,HOXA11,HOXA13,HOXA9,HOXC11,HOXC13,HOXD11,HOXD13,HSP90AA1,HSP90AB1,IGH,IGK,IGL,IL21R,IL3,ITK,KBTBD4,KDSR,KIF5B,KLF5,KLHL6,KSR2,LASP1,LEF1,LPP,LRP1B,LTB,LYL1,MAD2L2,MGAM,MLF1,MLLT3,MLLT6,MN1,MOB3B,MPEG1,MRTFA,MSN,MUC1,MYH11,MYH9,NADK,NCOA2,NDRG1,NFE2,NFKB2,NIN,NRG1,NUMA1,NUP214,NUTM2A,PAFAH1B2,PAX3,PAX7,PAX8,PBX1,PCM1,PDE4DIP,PDK1,PDS5B,PER1,PGBD5,PICALM,PIGA,PLAG1,PML,POU2AF1,PPP2R2A,PRDM16,PRKACA,PRRX1,PSIP1,PTPN1,PTPRO,QKI,RABEP1,RAP1GDS1,RELN,REST,RHOH,RNF213,ROBO1,RPL22,RPN1,RSPO2,SAMHD1,SCG5,SDC4,SERPINB3,SERPINB4,SET,SETD1A,SETD1B,SETD3,SETD4,SETD5,SETD6,SETD7,SETDB2,SH3GL1,SLC34A2,SLFN11,SMARCA2,SMG1,SOCS3,SP140,SPRTN,SRSF3,SSX1,SSX2,SSX4,STAG1,TAF15,TAL2,TET3,TFG,TNFRSF17,TPM3,TPM4,TRA,TRB,TRD,TRG,TRIM24,TRIP11,TRIP13,USP6,VAV1,VAV2,WIF1,ZBTB16,ZMYM2,ZNF217,ZNF384,ZNF521,ZNF703,ZNRF3,ACKR3,ACSL3,ACSL6,ACTB,ACVR2A,ADGRA2,AFF3,AJUBA,APH1A,APOBEC3B,ASMTL,ASPSCR1,ATG5,ATP1A1,ATP2B3,BAX,BCL9L,BRD3,BRSK1,BTLA,BUB1B,CACNA1D,CAD,CANT1,CBLB,CBLC,CCDC6,CCN6,CCNB1IP1,CCNB3,CCT6B,CD36,CDH11,CHCHD7,CHD2,CHD4,CHIC2,CHN1,CILK1,CKS1B,CLIP1,CLP1,CNBP,CNOT3,COL2A1,CPS1,CRTC1,CRTC3,CSF1,CUL4A,CYP17A1,DAZAP1,DCTN1,DDB2,DDR1,DDX4,DDX41,DDX5,DKK1,DKK2,DKK3,DKK4,DUSP2,DUSP9,EIF3E,ELK4,ELN,ELP2,EPHB4,ERC1,ETS1,EXOSC6,EXT1,EXT2,FAF1,FAT4,FBXO31,FES,FGF12,FIP1L1,FLYWCH1,FNBP1,FRS2,GABRA6,GADD45B,GATA4,GATA6,GMPS,GOLGA5,GOPC,GPC3,GRM3,GTSE1,H3C15,H3P6,HIRA,HNRNPA2B1,HOOK3,HOXA3,HSD3B1,IKBKB,IKZF2,IL2,IL6ST,INPP5D,IRF2,IRS4,JAZF1,KAT6B,KCNJ5,KDM2B,KDM4C,KEL,KLF2,KLF3,KLF6,KLK2,KNL1,KTN1,LARP4B,LCP1,LIFR,LMNA,LRIG3,LRP5,LRP6,LRRK2,LTK,MAGED1,MAML2,MAP3K6,MAP3K7,MBD6,MDS2,MEF2C,MEF2D,MERTK,MIB1,MIDEAS,MKI67,MKNK1,MLLT11,MNX1,MTCP1,MYO18A,MYO5A,NAB2,NACA,NBEAP1,NCOA1,NCOA4,NFATC2,NFIB,NFKBIE,NOD1,NONO,NUTM2B,OLIG2,OMD,PAG1,PAK3,PARP2,PARP3,PASK,PATZ1,PC,PCLO,PCSK7,PDCD11,PHF1,PIK3C2B,POLQ,POU5F1,PPFIBP1,PPP1CB,PRCC,PRF1,PRKDC,PRSS1,PRSS8,PTK6,PTK7,PTPN13,PTPN6,PTPRB,PTPRC,PTPRK,RALGDS,RANBP2,RASGEF1A,RMI2,RNF217-AS1,RPL10,RPL5,RSPO3,RUNX2,S1PR2,SALL4,SBDS,SEC31A,SEPTIN5,SEPTIN6,SEPTIN9,SERP2,SFPQ,SFRP1,SFRP2,SFRP4,SIX1,SLC1A2,SLC45A3,SMARCA1,SNCAIP,SND1,SNX29,SOCS2,SOX10,SS18L1,STAT1,STAT2,STAT4,STIL,STRN,TAF1,TCEA1,TCF12,TCL1B,TEC,TERC,TFEB,TFPT,TFRC,TIPARP,TLE1,TLE2,TLE3,TLE4,TLL2,TMEM30A,TMSB4XP8,TNFRSF11A,TPR,TRIM27,TRIM33,TRRAP,TTL,TUSC3,TYRO3,WDCP,WDR90,WRN,XPA,XPC,YPEL5,YWHAE,YY1,YY1AP1,ZBTB20,ZBTB7A,ZFP36L1,ZMYM3,ZNF24,ZNF331,ZNF750".split(","));
        //articleService.runSearch("\"p53\"", 50000, 0);
        if (articleService.indexArticles()) {
            final Semaphore concurrentExecutions = new Semaphore(1);
            final Semaphore concurrentDFSExecutions = new Semaphore(16);
            genes.parallelStream().forEach((g) -> {
                concurrentExecutions.acquireUninterruptibly();
                String XMLFile = "";
                try {
                    XMLFile = articleService.runSearchPython('"' + g + '"', 600);
                } finally {
                    concurrentExecutions.release();
                }
                log.info(g);
                concurrentDFSExecutions.acquireUninterruptibly();
                try {
                    if (XMLFile.length() > 0) articleService.runSearchCompleteParallel(XMLFile);
                } finally {
                    concurrentDFSExecutions.release();
                }
            });
        }
    }
     */

    @Test
    public void indexNewItems() {
        UpdateItems updateItems = readItems();
        articleIndexService.indexNewItems(updateItems);
    }

    @Test
    public void addToMaps() {
        UpdateItems updateItems = readItems();
        mapService.addManyTerms(updateItems);
    }

    public static UpdateItems readItems() {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(UpdateItems.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            UpdateItems ui = (UpdateItems)jaxbUnmarshaller.unmarshal(new File("UpdateItems.xml"));
            log.info(ui.toString());
            return ui;
        }
        catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
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
        //if (article.getFulltext() != null) sb.append(article.getFulltext());
        return sb.toString();
    }

    @Test
    public void runDL() {
        MCNetwork mc = new MCNetwork("data.csv", 9353, 123, 1021, 1800).buffered("/scratch/czaplal/states.bin", 5000);
        //MCNetwork mc = new MCNetwork("MCoutput1.out");
        //mc.setupData("data.csv", 9353);
        mc.setName("demo1");
        mc.shuffle(321, 0.5);
        mc.normalize();
        mc.runNetwork("MCiteration.out", 50000);
        mc.tune();
        mc.resumeNetwork("MCiteration2.out", 50000);
        mc.tune();
        mc.resumeNetwork("MCiteration3.out", 50000);
        mc.runMC(100000, false);
        mc.resetEquilibrated();
        mc.runMC(300000, true);
        simulationMIND.saveSimulation(mc);
    }

}
