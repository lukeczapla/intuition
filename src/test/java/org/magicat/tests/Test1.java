package org.magicat.tests;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.model.changestream.UpdateDescription;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.biojava.bio.BioException;
import org.biojava.bio.symbol.Alphabet;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.carrot2.clustering.Cluster;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.magicat.MIND.SimulationMIND;
import org.magicat.config.ScheduledTasks;
import org.magicat.controller.ArticleController;
import org.magicat.controller.VariantController;
import org.magicat.model.*;
import org.magicat.model.xml.UpdateConfig;
import org.magicat.model.xml.UpdateItems;
import org.magicat.montecarlo.MCNetwork;
import org.magicat.pdf.PDFHighlighter;
import org.magicat.pdf.Section;
import org.magicat.repository.*;
import org.magicat.service.*;
import org.magicat.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class Test1 {

    private final static Logger log = LoggerFactory.getLogger(Test1.class);

    @Autowired
    SolrService solrService;
    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    SolrClientTool solrClientTool;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    TargetRepository targetRepository;
    @Autowired
    GeneMapRepository geneMapRepository;
    @Autowired
    MutationMapRepository mutationMapRepository;
    @Autowired
    CancerMapRepository cancerMapRepository;
    @Autowired
    DrugMapRepository drugMapRepository;
    @Autowired
    ProjectListRepository projectListRepository;
    @Autowired
    ArticleIndexRepository articleIndexRepository;
    @Autowired
    ArticleService articleService;
    @Autowired
    MapService mapService;
    @Autowired
    CancerTypeRepository cancerTypeRepository;
    @Autowired
    org.magicat.service.FullTextService fullTextService;
    @Autowired
    FullTextRepository fullTextRepository;
    @Autowired
    GlobalTimestampRepository globalTimestampRepository;
    @Autowired
    AnalyticsService analyticsService;
    @Autowired
    VariantRepository variantRepository;
    @Autowired
    MongoClient mongoClient;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    VariantService variantService;

    BsonDocument resumeToken = null;

    @Test
    public void resumeCuration() {
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");
        boolean pass = true;
        List<Variant> curationList = new ArrayList<>();
        for (Variant v: variants) {
            if (v.getCancerTypes() != null && v.getCancerTypes().equals("Acute Myeloid Leukemia")) {
                pass = false;
            }
            if (pass) continue;
            if (v.getMutation().equals("Oncogenic Mutations")) continue;
            curationList.add(v);
        }

        analyticsService.processVariants(curationList);

    }

    @Test
    public void journalTiers() {
        List<Variant> variantsList = variantRepository.findAll();
        //Set<String> journalSet = Collections.synchronizedSet(new TreeSet<>());
        Map<String, Integer> journalSet = Collections.synchronizedMap(new LinkedHashMap<>());
        variantsList.parallelStream().forEach(v -> {
            List<Article> articles = articleRepository.findAllByPmIdIn(v.getArticlesTier1());
            articles.addAll(articleRepository.findAllByPmIdIn(v.getArticlesTier2()));
            articles.parallelStream().filter(a -> a.getJournal() != null).forEach(a -> journalSet.merge(a.getJournal().toLowerCase(), 1, Integer::sum));
        });
        List<Map.Entry<String, Integer>> list = new ArrayList<>(journalSet.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        for (Map.Entry<String, Integer> journal : list) {
            System.out.println(journal.getKey() + " " + journal.getValue());
        }
    }

    @Test
    public void addGeneSynonyms() {
        //GeneMap was = new GeneMap("was", null);
        //geneMapRepository.insert(was);
        List<GeneMap> genes = geneMapRepository.findAll();
        genes.forEach(gene -> {
            List<Target> targets = targetRepository.findAllBySymbol(gene.getSymbol().toUpperCase());
            final Set<String> synonyms = new HashSet<>();
            targets.forEach(t -> {
                String[] names = t.getSynonyms().split(";");
                for (String syn : names) synonyms.add(syn.toUpperCase());
            });
            if (synonyms.size() > 0) {
                String result = "";
                Iterator<String> it = synonyms.iterator();
                while (it.hasNext()) {
                    result += it.next();
                    if (it.hasNext()) result += ";";
                }
                gene.setSynonyms(result);
                geneMapRepository.save(gene);
            }
        });
    }

    @Test
    public void testDismax() {
        SolrDocumentList list;
        try {
            list = solrClientTool.findDismax("knowledge", "+2*");
        } catch (IOException|SolrServerException e) {
            log.error("Error: {}", e.getMessage());
            return;
        }
        if (list.size() > 0) {
            log.info("{} items", list.size());
            for (SolrDocument sd : list) {
                ArrayList t = (ArrayList)sd.get("text");
                String text = (String)t.get(0);
                if (text.contains("2*")) System.out.println("FOUND: " + text);
                else System.out.println("Article doesn't have the item!");
            }
        }
        log.info("Finished");
    }

    @Test
    public void letterCheck() {
        System.out.println(analyticsService.processCodes("A5BGBCCDGCC"));
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");
        for (Variant v : variants) {
            if (v.getScoreCode1() != null && v.getScoreCode1().size() > 0)
                for (int i = 0; i < v.getScoreCode1().size(); i++) {
                    v.getScoreCode1().set(i, analyticsService.processCodes(v.getScoreCode1().get(i)));
                }
            if (v.getScoreCode2() != null && v.getScoreCode2().size() > 0)
                for (int i = 0; i < v.getScoreCode2().size(); i++) {
                    v.getScoreCode2().set(i, analyticsService.processCodes(v.getScoreCode2().get(i)));
                }
            variantRepository.save(v);
        }
        variants = variantRepository.findAllByKey("AKTtest");
        for (Variant v : variants) {
            if (v.getScoreCode1() != null && v.getScoreCode1().size() > 0)
                for (int i = 0; i < v.getScoreCode1().size(); i++) {
                    v.getScoreCode1().set(i, analyticsService.processCodes(v.getScoreCode1().get(i)));
                }
            if (v.getScoreCode2() != null && v.getScoreCode2().size() > 0)
                for (int i = 0; i < v.getScoreCode2().size(); i++) {
                    v.getScoreCode2().set(i, analyticsService.processCodes(v.getScoreCode2().get(i)));
                }
            variantRepository.save(v);
        }
        log.info("All done");
    }


    @Test
    public void AKTE91D() {
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");
        List<Variant> submit = new ArrayList<>();
        for (Variant v : variants) {
            if (v.getMutation().contains("Fusion")) submit.add(v);
        }
        analyticsService.processVariants(submit);
    }

    @Test
    public void testSearchCluster() {
        SolrService.SearchResult result = solrService.searchSolr(500, "BRAF", DateTime.now().minusYears(2), true);
        SolrDocumentList sdl = result.getDocs();
        System.out.println("Size is " + sdl.size() + " articles");
        List<String> pmids = result.getPmIds();
        Carrot2Util.carrot2Cluster(articleRepository.findAllByPmIdIn(pmids));
    }

    @Test
    public void testLucene() {
        SolrDocumentList sdl;
        try {
            sdl = solrClientTool.find("knowledge", "/(BRAF)/");
            log.info("{} documents found", sdl.size());
            SolrDocument s = sdl.get(0);
            String str = ((List<String>)s.get("text")).get(0);
            System.out.println(str);
        } catch (IOException|SolrServerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTika2() {
    /*    List<String> genes = Arrays.asList("ABL1,AKT1,ALK,AMER1,APC,AR,ARID1A,ASXL1,ATM,ATRX,AXIN1,BAP1,BCL2,BCOR,BRAF,BRCA1,BRCA2,CARD11,CBL,CDC73,CDH1,CDKN2A,CEBPA,CIC,CREBBP,CTNNB1,DAXX,DNMT3A,EGFR,EP300,ERBB2,EZH2,FBXW7,FGFR2,FGFR3,FLT3,FOXL2,GATA3,GNA11,GNAQ,GNAS,HNF1A,HRAS,IDH1,IDH2,JAK1,JAK2,JAK3,KDM5C,KDM6A,KIT,KMT2D,KRAS,MAP2K1,MAP3K1,MED12,MEN1,MET,MLH1,MPL,MSH2,MSH6,MYD88,NF1,NF2,NFE2L2,NOTCH1,NOTCH2,NPM1,NRAS,PAX5,PBRM1,PDGFRA,PIK3CA,PIK3R1,PPP2R1A,PRDM1,PTCH1,PTEN,PTPN11,RB1,RET,RNF43,SETD2,SF3B1,SMAD2,SMAD4,SMARCA4,SMARCB1,SMO,SOCS1,SPOP,STAG2,STK11,TET2,TNFAIP3,TP53,TSC1,U2AF1,VHL,WT1,AKT2,ARID2,ATR,B2M,BARD1,BCL6,BRD4,BRIP1,BTK,CALR,CASP8,CBFB,CCND1,CCND2,CCND3,CCNE1,CD274,CD79A,CD79B,CDK12,CDK4,CDK6,CDKN1B,CDKN2C,CHEK2,CRLF2,CSF1R,CSF3R,CTCF,CXCR4,DDR2,ERBB3,ERBB4,ERG,ESR1,ETV6,FANCA,FANCC,FGFR1,FGFR4,FLCN,FUBP1,GATA1,GATA2,H3-3A,H3C2,IKZF1,IRF4,JUN,KDM5A,KDR,KEAP1,KMT2A,KMT2C,MAP2K2,MAP2K4,MAPK1,MDM2,MDM4,MITF,MTOR,MUTYH,MYC,MYCL,MYCN,NKX2-1,NSD2,NSD3,NTRK1,NTRK3,PALB2,PDCD1LG2,PDGFRB,PHF6,PIM1,PRKAR1A,RAD21,RAF1,RARA,ROS1,RUNX1,SDHA,SDHB,SDHC,SDHD,SOX2,SPEN,SRC,SRSF2,STAT3,SUFU,SYK,TENT5C,TGFBR2,TMPRSS2,TNFRSF14,TSC2,TSHR,XPO1,AKT3,ARAF,ARID1B,AURKA,AURKB,AXL,BCL10,BCORL1,BCR,BIRC3,BLM,BTG1,CDK8,CDKN2B,CHEK1,CRKL,CYLD,DOT1L,EED,EIF4A2,EPHA3,EPHB1,ERCC4,ETV1,FAS,FGF19,FGF3,FGF4,FH,FLT1,FLT4,FOXO1,FOXP1,GRIN2A,GSK3B,HGF,ID3,IGF1R,IKBKE,IL7R,INPP4B,IRS2,KLF4,LMO1,MALT1,MAP3K13,MCL1,MEF2B,MRE11,MSH3,MSI2,NBN,NCOR1,NFKBIA,NSD1,NT5C2,NTRK2,P2RY8,PDCD1,PIK3CB,PMS2,POLD1,POLE,POT1,PPARG,RAC1,RAD51,RAD51B,RBM10,REL,RHOA,RICTOR,RPTOR,SETBP1,SOX9,STAT5B,SUZ12,TBX3,TCF3,TERT,TET1,TOP1,TP63,TRAF7,ZRSR2,ACVR1,ALOX12B,AXIN2,BCL11B,BCL2L1,BMPR1A,CDKN1A,CIITA,CUL3,CUX1,DDX3X,DICER1,DIS3,DNAJB1,DNMT1,DROSHA,EPAS1,EPHA5,EPHA7,ERCC2,ERCC3,ERCC5,ERRFI1,ETV4,ETV5,EWSR1,FANCD2,FAT1,FBXO11,FOXA1,GLI1,GNA13,H1-2,H3-3B,HDAC1,HLA-A,INHBA,LATS1,LATS2,LYN,MAF,MAP3K14,MAX,MLLT1,MST1R,MYOD1,NCOR2,NOTCH3,NUP93,PARP1,PHOX2B,PIK3C2G,PIK3CG,PIK3R2,PLCG2,PPM1D,PPP6C,PREX2,PRKCI,PRKN,PTPRT,RAD50,RAD51C,RAD51D,RAD52,RAD54L,RECQL4,RUNX1T1,SDHAF2,SGK1,SH2B3,SMAD3,SMARCD1,STAT5A,STAT6,TBL1XR1,TCF7L2,TEK,TMEM127,TRAF2,VEGFA,WWTR1,XRCC2,ZFHX3,ABL2,ABRAXAS1,AGO2,ANKRD11,ARID5B,ASXL2,ATF1,BABAM1,BBC3,BCL2L11,BCL9,CARM1,CCNQ,CD276,CD58,CDC42,CENPA,COP1,CSDE1,CTLA4,CYSLTR2,DCUN1D1,DDIT3,DEK,DNMT3B,DTX1,DUSP22,DUSP4,E2F3,EGFL7,EIF1AX,EIF4E,ELF3,ELOC,EPCAM,ERF,ETNK1,EZH1,FANCG,FANCL,FEV,FLI1,FYN,GNA12,GNB1,GPS2,GREM1,H1-3,H1-4,H2AC11,H2AC16,H2AC17,H2AC6,H2BC11,H2BC12,H2BC17,H2BC4,H2BC5,H3-4,H3-5,H3C1,H3C10,H3C11,H3C12,H3C13,H3C14,H3C3,H3C4,H3C6,H3C7,H3C8,HDAC4,HDAC7,HIF1A,HLA-B,HOXB13,ICOSLG,IFNGR1,IGF1,IGF2,IKZF3,IL10,INHA,INPP4A,INPPL1,INSR,IRF1,IRF8,IRS1,JARID2,KAT6A,KMT2B,KMT5A,KNSTRN,LCK,LMO2,LZTR1,MAFB,MAPK3,MAPKAP1,MDC1,MECOM,MGA,MLLT10,MSI1,MST1,MTAP,MYB,NCOA3,NCSTN,NEGR1,NKX3-1,NOTCH4,NR4A3,NTHL1,NUF2,NUP98,NUTM1,PAK1,PAK5,PCBP1,PDGFB,PDPK1,PGR,PIK3C3,PIK3CD,PIK3R3,PLCG1,PLK2,PMAIP1,PMS1,PNRC1,PPP4R2,PRDM14,PRKD1,PTP4A1,PTPN2,PTPRD,PTPRS,RAB35,RAC2,RASA1,RBM15,RECQL,RHEB,RIT1,RPS6KA4,RPS6KB2,RRAGC,RRAS,RRAS2,RTEL1,RXRA,RYBP,SESN1,SESN2,SESN3,SETDB1,SH2D1A,SHOC2,SHQ1,SLX4,SMARCE1,SMC1A,SMC3,SMYD3,SOS1,SOX17,SPRED1,SS18,STK19,STK40,TAL1,TAP1,TAP2,TCL1A,TFE3,TGFBR1,TLX1,TLX3,TP53BP1,TRAF3,TRAF5,TYK2,U2AF2,UBR5,UPF1,USP8,VTCN1,XBP1,XIAP,YAP1,YES1,ABI1,ACTG1,ACVR1B,AFDN,AFF1,AFF4,AGO1,ALB,APLNR,ARFRP1,ARHGAP26,ARHGAP35,ARHGEF12,ARHGEF28,ARID3A,ARID3B,ARID3C,ARID4A,ARID4B,ARID5A,ARNT,ATIC,ATP6AP1,ATP6V1B2,ATXN2,ATXN7,BACH2,BCL11A,BCL2L2,BCL3,BCL7A,BTG2,CAMTA1,CARS1,CBFA2T3,CD22,CD28,CD70,CD74,CDX2,CEP43,CLTC,CLTCL1,CMTR2,CNTRL,COL1A1,CRBN,CREB1,CREB3L1,CREB3L2,CTNNA1,CTR9,CYP19A1,DDX10,DDX6,DNM2,EBF1,ECT2L,EGR1,ELF4,ELL,EML4,EMSY,EP400,EPOR,EPS15,ESCO2,ETAA1,EZHIP,EZR,FANCE,FANCF,FCGR2B,FCRL4,FGF10,FGF14,FGF23,FGF6,FHIT,FOXF1,FOXO3,FOXO4,FSTL3,FURIN,FUS,GAB1,GAB2,GAS7,GID4,GPHN,GTF2I,H1-5,H2BC8,H4C9,HERPUD1,HEY1,HIP1,HLA-C,HLF,HMGA1,HMGA2,HOXA11,HOXA13,HOXA9,HOXC11,HOXC13,HOXD11,HOXD13,HSP90AA1,HSP90AB1,IGH,IGK,IGL,IL21R,IL3,ITK,KBTBD4,KDSR,KIF5B,KLF5,KLHL6,KSR2,LASP1,LEF1,LPP,LRP1B,LTB,LYL1,MAD2L2,MGAM,MLF1,MLLT3,MLLT6,MN1,MOB3B,MPEG1,MRTFA,MSN,MUC1,MYH11,MYH9,NADK,NCOA2,NDRG1,NFE2,NFKB2,NIN,NRG1,NUMA1,NUP214,NUTM2A,PAFAH1B2,PAX3,PAX7,PAX8,PBX1,PCM1,PDE4DIP,PDK1,PDS5B,PER1,PGBD5,PICALM,PIGA,PLAG1,PML,POU2AF1,PPP2R2A,PRDM16,PRKACA,PRRX1,PSIP1,PTPN1,PTPRO,QKI,RABEP1,RAP1GDS1,RELN,REST,RHOH,RNF213,ROBO1,RPL22,RPN1,RSPO2,SAMHD1,SCG5,SDC4,SERPINB3,SERPINB4,SET,SETD1A,SETD1B,SETD3,SETD4,SETD5,SETD6,SETD7,SETDB2,SH3GL1,SLC34A2,SLFN11,SMARCA2,SMG1,SOCS3,SP140,SPRTN,SRSF3,SSX1,SSX2,SSX4,STAG1,TAF15,TAL2,TET3,TFG,TNFRSF17,TPM3,TPM4,TRA,TRB,TRD,TRG,TRIM24,TRIP11,TRIP13,USP6,VAV1,VAV2,WIF1,ZBTB16,ZMYM2,ZNF217,ZNF384,ZNF521,ZNF703,ZNRF3,ACKR3,ACSL3,ACSL6,ACTB,ACVR2A,ADGRA2,AFF3,AJUBA,APH1A,APOBEC3B,ASMTL,ASPSCR1,ATG5,ATP1A1,ATP2B3,BAX,BCL9L,BRD3,BRSK1,BTLA,BUB1B,CACNA1D,CAD,CANT1,CBLB,CBLC,CCDC6,CCN6,CCNB1IP1,CCNB3,CCT6B,CD36,CDH11,CHCHD7,CHD2,CHD4,CHIC2,CHN1,CILK1,CKS1B,CLIP1,CLP1,CNBP,CNOT3,COL2A1,CPS1,CRTC1,CRTC3,CSF1,CUL4A,CYP17A1,DAZAP1,DCTN1,DDB2,DDR1,DDX4,DDX41,DDX5,DKK1,DKK2,DKK3,DKK4,DUSP2,DUSP9,EIF3E,ELK4,ELN,ELP2,EPHB4,ERC1,ETS1,EXOSC6,EXT1,EXT2,FAF1,FAT4,FBXO31,FES,FGF12,FIP1L1,FLYWCH1,FNBP1,FRS2,GABRA6,GADD45B,GATA4,GATA6,GMPS,GOLGA5,GOPC,GPC3,GRM3,GTSE1,H3C15,H3P6,HIRA,HNRNPA2B1,HOOK3,HOXA3,HSD3B1,IKBKB,IKZF2,IL2,IL6ST,INPP5D,IRF2,IRS4,JAZF1,KAT6B,KCNJ5,KDM2B,KDM4C,KEL,KLF2,KLF3,KLF6,KLK2,KNL1,KTN1,LARP4B,LCP1,LIFR,LMNA,LRIG3,LRP5,LRP6,LRRK2,LTK,MAGED1,MAML2,MAP3K6,MAP3K7,MBD6,MDS2,MEF2C,MEF2D,MERTK,MIB1,MIDEAS,MKI67,MKNK1,MLLT11,MNX1,MTCP1,MYO18A,MYO5A,NAB2,NACA,NBEAP1,NCOA1,NCOA4,NFATC2,NFIB,NFKBIE,NOD1,NONO,NUTM2B,OLIG2,OMD,PAG1,PAK3,PARP2,PARP3,PASK,PATZ1,PC,PCLO,PCSK7,PDCD11,PHF1,PIK3C2B,POLQ,POU5F1,PPFIBP1,PPP1CB,PRCC,PRF1,PRKDC,PRSS1,PRSS8,PTK6,PTK7,PTPN13,PTPN6,PTPRB,PTPRC,PTPRK,RALGDS,RANBP2,RASGEF1A,RMI2,RNF217-AS1,RPL10,RPL5,RSPO3,RUNX2,S1PR2,SALL4,SBDS,SEC31A,SEPTIN5,SEPTIN6,SEPTIN9,SERP2,SFPQ,SFRP1,SFRP2,SFRP4,SIX1,SLC1A2,SLC45A3,SMARCA1,SNCAIP,SND1,SNX29,SOCS2,SOX10,SS18L1,STAT1,STAT2,STAT4,STIL,STRN,TAF1,TCEA1,TCF12,TCL1B,TEC,TERC,TFEB,TFPT,TFRC,TIPARP,TLE1,TLE2,TLE3,TLE4,TLL2,TMEM30A,TMSB4XP8,TNFRSF11A,TPR,TRIM27,TRIM33,TRRAP,TTL,TUSC3,TYRO3,WAS,WDCP,WDR90,WRN,XPA,XPC,YPEL5,YWHAE,YY1,YY1AP1,ZBTB20,ZBTB7A,ZFP36L1,ZMYM3,ZNF24,ZNF331,ZNF750".split(","));
        List<String> synonyms = new ArrayList<>();
        for (String gene: genes) {
            List<Target> ot = targetRepository.findAllBySymbol(gene);
            if (ot != null && ot.size() > 0) synonyms.add(ot.get(ot.size()-1).getSynonyms());
            else synonyms.add("");
            /*if (ot.isPresent() && ot.get().getSynonyms() != null && ot.get().getSynonyms().length() > 0) {
                synonyms.add(ot.get().getSynonyms());
            } else synonyms.add("");
        }
        ProjectList pl = new ProjectList("geneSearchList", genes, synonyms);
        projectListRepository.save(pl);*/
        Variant v = variantRepository.findByDescriptor("BRAF:A598V::");
        analyticsService.processVariants(Collections.singletonList(v));
/*
        try {
            //System.out.println(TikaTool.parseToHTML("30398411.pdf"));
            //String a = TikaTool.parseToHTML("30398411.pdf");
            String b = TikaTool.parseToHTML("30398411.pdf");
            System.out.println(b);
            //System.out.println("\n\n\nPLAIN TEXT\n\n\n");
            //System.out.paaarintln(TikaTool.parseToPlainText("30398411.pdf"));
        } catch (Exception e) {
            e.printStackTrace();
        } */
    }

    @Test
    public void testJournals() {
        List<Article> articles = articleRepository.findAllWithJournalOnly(Pageable.unpaged());
        Set<String> journals = articles.parallelStream().map(a -> a.getJournal().toLowerCase()).collect(Collectors.toCollection(TreeSet::new));
        for (String journal : journals) {
            System.out.println(journal);
        }
        System.out.printf("There are %d unique journals\n", journals.size());
    }

    @Test
    public void testChange() {
        System.out.println("Setting up collection and pipeline");
        ConnectionString connectionString = new ConnectionString("mongodb://res_MIS_KnwP:qpka@plmongo1.mskcc.org:27117/pmg_knowledge?authSource=admin&replicaSet=rsP1edg1");
        CodecRegistry myCodecRegistry = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), myCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();
        try (MongoClient mongoClient1 = MongoClients.create(clientSettings)) {
            MongoDatabase db = mongoClient1.getDatabase("pmg_knowledge").withCodecRegistry(codecRegistry);
            MongoCollection<Article> collection = db.getCollection("Articles", Article.class);
            System.out.println(collection.countDocuments() + " documents");
            List<Bson> pipeline = Collections.singletonList(Aggregates.match(Filters.in("operationType", Arrays.asList("update", "insert", "delete"))));
            MongoCursor<ChangeStreamDocument<Article>> csi = collection.watch(pipeline).iterator();
            //if (resumeToken != null)
            System.out.println("Running code");
            while (csi.hasNext()) {
                ChangeStreamDocument<Article> x = csi.next();
                log.info("OperationType {}", x.getOperationType());
                resumeToken = x.getResumeToken();
                BsonDocument doc = x.getDocumentKey();
                OperationType op = x.getOperationType();
                BsonString key = doc.get("_id").asString();
                if (op.equals(OperationType.DELETE)) {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(key));
                    log.info("Removing item");
                    mongoTemplate.remove(query, Article.class, "Articles");
                }
                if (op.equals(OperationType.UPDATE)) {
                    UpdateDescription desc = x.getUpdateDescription();
                    BsonDocument updates = null;
                    if (desc != null) updates = desc.getUpdatedFields();
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(key));
                    Update update = new Update();
                    if (updates != null) for (String value : updates.keySet()) {
                        update.set(value, updates.get(value));
                    }
                    if (desc != null) {
                        List<String> removed = desc.getRemovedFields();

                        if (removed != null) for (String value : removed) {
                            update.unset(value);
                        }
                    }
                    log.info("Updating item");
                    mongoTemplate.updateFirst(query, update, Article.class, "Articles");
                }
                if (op.equals(OperationType.INSERT)) {
                    Article document = x.getFullDocument();
                    log.info("Inserting item");
                    mongoTemplate.insert(Collections.singletonList(document), "Articles");
                }
            }
        }
        /*csi.forEach(x -> {
            System.out.println(x.getOperationType());
            resumeToken = x.getResumeToken();
            BsonDocument doc = x.getDocumentKey();
            OperationType op = x.getOperationType();
            BsonString key = doc.get("_id").asString();
            if (op.equals(OperationType.DELETE)) {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(key));
                log.info("Removing item");
                mongoTemplate.remove(query, Article.class,"Articles");
            }
            if (op.equals(OperationType.UPDATE)) {
                UpdateDescription desc = x.getUpdateDescription();
                BsonDocument updates = null;
                if (desc != null) updates = desc.getUpdatedFields();
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(key));
                Update update = new Update();
                if (updates != null) for (String value : updates.keySet()) {
                    update.set(value, updates.get(value));
                }
                if (desc != null) {
                    List<String> removed = desc.getRemovedFields();

                    if (removed != null) for (String value : removed) {
                        update.unset(value);
                    }
                }
                log.info("Updating item");
                mongoTemplate.updateFirst(query, update, Article.class,"Articles");
            }
            if (op.equals(OperationType.INSERT)) {
                Article document = x.getFullDocument();
                log.info("Inserting item");
                mongoTemplate.insert(Collections.singletonList(document), "Articles");
            }
        });*/
    }

    class JodaCodec implements Codec<DateTime> {

        @Override
        public DateTime decode(BsonReader bsonReader, DecoderContext decoderContext) {
            return new DateTime(bsonReader.readDateTime());
        }

        @Override
        public void encode(BsonWriter bsonWriter, DateTime dateTime, EncoderContext encoderContext) {
            bsonWriter.writeDateTime(dateTime.getMillis());
        }

        @Override
        public Class<DateTime> getEncoderClass() {
            return DateTime.class;
        }
    }

    @Test
    public void syncArticles() {
        CodecRegistry articleCodec = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry dateCodec = CodecRegistries.fromCodecs(new JodaCodec());
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), dateCodec, articleCodec);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(System.getenv("mongoUri")))
                .codecRegistry(codecRegistry)
                .build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        MongoCollection<Document> articles = mongoClient.getDatabase("pmg_knowledge").getCollection("Articles").withCodecRegistry(codecRegistry);
        List<Bson> pipeline = Collections.singletonList(Aggregates.match(Filters.in("operationType", Arrays.asList("insert", "delete", "update"))));
        articles.watch(pipeline, Article.class).forEach(articleChangeStreamDocument -> {
            log.info("Operation: {}", articleChangeStreamDocument.getOperationType());
            BsonDocument resumeToken = articleChangeStreamDocument.getResumeToken();
            if (articleChangeStreamDocument.getOperationType().getValue().equals("insert") || articleChangeStreamDocument.getOperationType().getValue().equals("update")) {
                log.info("Inserting/Updating new document");
                Article a = articleChangeStreamDocument.getFullDocument();
                //Optional<Article> oa = articleRepository.findById(a.getId().toString());
                articleRepository.save(a);
            }
        });
    }

    @Test
    public void searchSolrNew() {
        SolrService.SearchResult result = solrService.searchSolr(1000, Collections.singletonList("KRAS"), Collections.singletonList(Arrays.asList("'C-K-RAS", "C-K-RAS", "CFC2", "K-RAS2A", "K-RAS2B")), Collections.singletonList("G12A"),
                Collections.singletonList(Collections.singletonList("Gly12Ala")), null, null, Collections.singletonList("Breast Cancer"), null, null, null, null);
        log.info(result.getDocs().toString());
        List<String> pmIds = result.getPmIds();
        List<Float> scores = result.getScores();
        log.info(pmIds.toString());
        log.info(scores.toString());
        log.info("{} items", pmIds.size());
    }

    @Test
    public void syncForward() {
        // overrideConnectionString should be false
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/pmg_knowledge");
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        MongoClient mc = MongoClients.create(mongoClientSettings);
        MongoTemplate mt = new MongoTemplate(mc, "pmg_knowledge");
        int page = 1000;
        Page<Article> pages;
        do {
            pages = articleRepository.findAll(PageRequest.of(page++, 10000));
            List<Article> articles = pages.getContent();
            articles.parallelStream().forEach(x -> {
                Article a = mt.findOne(new Query(Criteria.where("pmId").is(x.getPmId())), Article.class, "Articles");
                if (a == null) {
                    log.info("Not currently in DB: adding");
                    mt.insert(x, "Articles");
                } else {
                    log.info("FOUND ALREADY");
                    // mt.save(x, "Articles");
                }
            });
        } while (pages.hasNext());
    }

    @Test
    public void validateTest() {
        String text = "The BRAF V600E mutation is interesting";
        log.info("The result is {}", textService.validateText(text, "BRAF", null, "V600E", 1));
    }

    @Test
    public void testTikaOffice() {
        //TikaTool.iTextParse("30398411.pdf");
        FullText ft = fullTextRepository.findById("32540409").get();
        String resource = ft.getResourceIds()[0];
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resource)));
        GridFsResource res = gridFsTemplate.getResource(file);
        InputStream is = res.getContent();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(is.readAllBytes());
            FileOutputStream f = new FileOutputStream("test.pdf");
            out.writeTo(f);
            f.flush();
            f.close();
            out.flush();
            out.close();
            System.out.println(TikaTool.OfficeParseDocument("test.pdf"));
        } catch(IOException e) {
            return;
        }
        /*String s = TikaTool.OfficeParseDocument("30398411.pdf");
        Pattern p = Pattern.compile("[A-Za-z](- )[A-Za-z]");
        Matcher m = p.matcher(s);
        while (m.find()) {
           s = s.substring(0, m.start() + 1) + s.substring(m.end()-1);
           m = p.matcher(s);
        }
        System.out.println(s);*/
      /*  String s = "This is a sentence with G12C and V600E in it";
        Pattern p = Pattern.compile(AminoAcids.missenseMutation);
        Matcher m = p.matcher(s);
        while (m.find()) {
            System.out.println(s.substring(m.start(), m.end()));
        }*/
    }

    @Test
    public void addTimestamp() {
        GlobalTimestamp gt = new GlobalTimestamp();
        gt.setName("articles");
        gt.setAfter(DateTime.now());
        globalTimestampRepository.save(gt);
    }

    @Test
    public void searchSolr() {
        try {
            SolrDocumentList sdl = solrClientTool.find("knowledge", "((attr_content:\"P53\" OR attr_content:\"TP53\") AND attr_content:\"C135Y\") OR ((text:\"P53\" OR text:\"TP53\") AND text:\"C135Y\")");
            for (SolrDocument sd : sdl) {
                System.out.println(sd);
            }
        } catch (IOException|SolrServerException e) {
            log.error(e.getMessage());
        }
    }

    private static Consumer<ChangeStreamDocument<Article>> printEvent() {
        return System.out::println;
    }

    @Test
    public void deleteAddDuplicateSolr() {
        List<FullText> fullTexts = fullTextRepository.findAll();

        fullTexts.parallelStream().forEach(f -> {
            try {
                SolrDocumentList documentList = solrClientTool.find("knowledge", "attr_fileurl:\"https://aimlcoe.mskcc.org/knowledge/getPDF/" + f.getPmId() + ".pdf\"");
                if (documentList.size() > 1) {
                    log.info("Two duplicated items in URL {}", f.getPmId());
                    for (int i = documentList.size() - 1; i >= 1; i--) {
                        solrClientTool.delete("knowledge", (String) (documentList.get(i).get("id")));
                    }
                } else if (documentList.size() == 0) {
                    log.info("Missing Solr document!");
                }
            } catch (IOException|SolrServerException e) {
                log.error(e.getMessage());
            }
        });

    }


    @Test
    public void loadMutations() {

        try {
            Scanner scan = new Scanner(new File("AKT1-2-3-VUS-2column.csv"));
            scan.nextLine();
            while (scan.hasNextLine()) {
                String line[] = scan.nextLine().split(",");
                if (line[1].endsWith(" fusion")) line[1] = line[1].substring(0, line[1].indexOf(" fusion"));
                if (line[1].endsWith(" Fusion")) line[1] = line[1].substring(0, line[1].indexOf(" Fusion"));

                Variant variant = new Variant();
                variant.setGene(line[0]);
                variant.setMutation(line[1]);
                variant.setKey("AKTtest");
                variantRepository.save(variant);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void migrateFullText() {
        List<FullText> fullTexts = fullTextRepository.findAll();
        for (FullText ft : fullTexts) {
            Query query = new Query();
            query.addCriteria(Criteria.where("pmId").in(ft.getPmId()));
            Update update = new Update();
            update.set("fulltext", ft.getTextEntry());
            mongoTemplate.updateFirst(query, update, Article.class);
        }
    }

    @Test
    void testSuppl() {
        List<FullText> fullTexts = fullTextRepository.findAllSupplementary();
        for (FullText fullText : fullTexts) {
            System.out.println(fullText.getPmId());
        }
    }

    @Test
    public void cleanupTopics() {
        int i = 0;
        Page<Article> pages;
        String[] badCancerTopics = {"cancer:other", "cancer:t", "cancer:b"};
        do {
            System.out.println("page " + i);
            pages = articleRepository.findAll(PageRequest.of(i, 500000));
            for (Article a: pages) {
                if (a.getTopics() == null) {
                    System.out.println("Updating " + a.getPmId() + " to have non-null topics");
                    a.setTopics("");
                    articleRepository.save(a);
                    continue;
                }
                if (a.getTopics().length() == 0) continue;
                for (String cancerTopic: badCancerTopics) {
                    if (a.getTopics().equals(cancerTopic)) {
                        a.setTopics("");
                        Article saved = articleRepository.save(a);
                        System.out.println("1. NEW TOPICS: " + saved.getTopics());
                    } else if (a.getTopics().startsWith(cancerTopic + ";")) {
                        a.setTopics(a.getTopics().substring((cancerTopic+";").length()));
                        Article saved = articleRepository.save(a);
                        System.out.println("2. NEW TOPICS: " + saved.getTopics());
                    } else if (a.getTopics().endsWith(";"+cancerTopic)) {
                        a.setTopics(a.getTopics().substring(0, a.getTopics().indexOf(";"+cancerTopic)));
                        Article saved = articleRepository.save(a);
                        System.out.println("3. NEW TOPICS: " + saved.getTopics());
                    } else if (a.getTopics().contains(";"+cancerTopic+";")) {
                        a.setTopics(a.getTopics().substring(0, a.getTopics().indexOf(";"+cancerTopic+";"))+";"+
                                a.getTopics().substring(a.getTopics().indexOf(";"+cancerTopic+";") + (";"+cancerTopic+";").length()));
                        Article saved = articleRepository.save(a);
                        System.out.println("4. NEW TOPICS: " + saved.getTopics());
                    }
                }
            }
            i++;
        } while (pages.hasNext());
    }

    @Test
    public void migrateFullTextLocal() {
        List<FullText> fullTexts = fullTextRepository.findAll();
        for (FullText ft : fullTexts) {
            Article a = articleRepository.findByPmId(ft.getPmId());
            if (a != null) {
                a.setFulltext(ft.getTextEntry());
                articleRepository.save(a);
            } else {
                System.out.println("ERROR: Expected to see PMID in Pubmed Articles: " + ft.getPmId());
            }
        }
    }

    @Test
    public void testMongo() {
        //Map<String, List<ArticleQuery.WordPair>> items = ArticleQuery.tsvReaderSimple(new File("allAnnotatedVariants.txt"));
        //System.out.println(AnalyticsServerImpl.getOncogenicMutations("braf", items));
        MongoDatabase db = mongoClient.getDatabase("tmg_knowledge");
        MongoCollection<Document> collection = db.getCollection("Targets");
        System.out.println(collection.countDocuments()+"");

    }

    @Test
    void quickAdd() throws Exception {
        parser = new XMLParser("pubmed.xml");
        parser.setArticleRepository(articleRepository);
        parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
    }

    @Test
    public void testAnalyticSearch() throws IOException {
        Files.write(Paths.get("akt123.xlsx"), variantService.createVariantSpreadsheet(variantRepository.findAllByKey("AKTtest")));//.processSpreadsheet("oncokb_braf_tp53_ros1_pmids.xlsx"));
    }

    @Test
    public void setDescriptors() {
        List<Variant> variants = variantRepository.findAll();
        for (Variant v: variants) {
            v.setDescriptor(v.getGene() + ":" + v.getMutation() + ":" + (v.getCancerTypes() != null ? v.getCancerTypes(): "") + ":" + (v.getDrugs() != null ? v.getDrugs(): ""));
            variantRepository.save(v);
        }
    }

    @Test
    public void testAnalyticMissing() {
        System.out.println("Missing: " + analyticsService.missingFullTextArticles("oncokb_braf_tp53_ros1_pmids.xlsx"));
    }

    @Test
    public void updateTargetsLinkcBioPortal() {
        List<String> genes = Arrays.asList("ABL1,AKT1,ALK,AMER1,APC,AR,ARID1A,ASXL1,ATM,ATRX,AXIN1,BAP1,BCL2,BCOR,BRAF,BRCA1,BRCA2,CARD11,CBL,CDC73,CDH1,CDKN2A,CEBPA,CIC,CREBBP,CTNNB1,DAXX,DNMT3A,EGFR,EP300,ERBB2,EZH2,FBXW7,FGFR2,FGFR3,FLT3,FOXL2,GATA3,GNA11,GNAQ,GNAS,HNF1A,HRAS,IDH1,IDH2,JAK1,JAK2,JAK3,KDM5C,KDM6A,KIT,KMT2D,KRAS,MAP2K1,MAP3K1,MED12,MEN1,MET,MLH1,MPL,MSH2,MSH6,MYD88,NF1,NF2,NFE2L2,NOTCH1,NOTCH2,NPM1,NRAS,PAX5,PBRM1,PDGFRA,PIK3CA,PIK3R1,PPP2R1A,PRDM1,PTCH1,PTEN,PTPN11,RB1,RET,RNF43,SETD2,SF3B1,SMAD2,SMAD4,SMARCA4,SMARCB1,SMO,SOCS1,SPOP,STAG2,STK11,TET2,TNFAIP3,TP53,TSC1,U2AF1,VHL,WT1,AKT2,ARID2,ATR,B2M,BARD1,BCL6,BRD4,BRIP1,BTK,CALR,CASP8,CBFB,CCND1,CCND2,CCND3,CCNE1,CD274,CD79A,CD79B,CDK12,CDK4,CDK6,CDKN1B,CDKN2C,CHEK2,CRLF2,CSF1R,CSF3R,CTCF,CXCR4,DDR2,ERBB3,ERBB4,ERG,ESR1,ETV6,FANCA,FANCC,FGFR1,FGFR4,FLCN,FUBP1,GATA1,GATA2,H3-3A,H3C2,IKZF1,IRF4,JUN,KDM5A,KDR,KEAP1,KMT2A,KMT2C,MAP2K2,MAP2K4,MAPK1,MDM2,MDM4,MITF,MTOR,MUTYH,MYC,MYCL,MYCN,NKX2-1,NSD2,NSD3,NTRK1,NTRK3,PALB2,PDCD1LG2,PDGFRB,PHF6,PIM1,PRKAR1A,RAD21,RAF1,RARA,ROS1,RUNX1,SDHA,SDHB,SDHC,SDHD,SOX2,SPEN,SRC,SRSF2,STAT3,SUFU,SYK,TENT5C,TGFBR2,TMPRSS2,TNFRSF14,TSC2,TSHR,XPO1,AKT3,ARAF,ARID1B,AURKA,AURKB,AXL,BCL10,BCORL1,BCR,BIRC3,BLM,BTG1,CDK8,CDKN2B,CHEK1,CRKL,CYLD,DOT1L,EED,EIF4A2,EPHA3,EPHB1,ERCC4,ETV1,FAS,FGF19,FGF3,FGF4,FH,FLT1,FLT4,FOXO1,FOXP1,GRIN2A,GSK3B,HGF,ID3,IGF1R,IKBKE,IL7R,INPP4B,IRS2,KLF4,LMO1,MALT1,MAP3K13,MCL1,MEF2B,MRE11,MSH3,MSI2,NBN,NCOR1,NFKBIA,NSD1,NT5C2,NTRK2,P2RY8,PDCD1,PIK3CB,PMS2,POLD1,POLE,POT1,PPARG,RAC1,RAD51,RAD51B,RBM10,REL,RHOA,RICTOR,RPTOR,SETBP1,SOX9,STAT5B,SUZ12,TBX3,TCF3,TERT,TET1,TOP1,TP63,TRAF7,ZRSR2,ACVR1,ALOX12B,AXIN2,BCL11B,BCL2L1,BMPR1A,CDKN1A,CIITA,CUL3,CUX1,DDX3X,DICER1,DIS3,DNAJB1,DNMT1,DROSHA,EPAS1,EPHA5,EPHA7,ERCC2,ERCC3,ERCC5,ERRFI1,ETV4,ETV5,EWSR1,FANCD2,FAT1,FBXO11,FOXA1,GLI1,GNA13,H1-2,H3-3B,HDAC1,HLA-A,INHBA,LATS1,LATS2,LYN,MAF,MAP3K14,MAX,MLLT1,MST1R,MYOD1,NCOR2,NOTCH3,NUP93,PARP1,PHOX2B,PIK3C2G,PIK3CG,PIK3R2,PLCG2,PPM1D,PPP6C,PREX2,PRKCI,PRKN,PTPRT,RAD50,RAD51C,RAD51D,RAD52,RAD54L,RECQL4,RUNX1T1,SDHAF2,SGK1,SH2B3,SMAD3,SMARCD1,STAT5A,STAT6,TBL1XR1,TCF7L2,TEK,TMEM127,TRAF2,VEGFA,WWTR1,XRCC2,ZFHX3,ABL2,ABRAXAS1,AGO2,ANKRD11,ARID5B,ASXL2,ATF1,BABAM1,BBC3,BCL2L11,BCL9,CARM1,CCNQ,CD276,CD58,CDC42,CENPA,COP1,CSDE1,CTLA4,CYSLTR2,DCUN1D1,DDIT3,DEK,DNMT3B,DTX1,DUSP22,DUSP4,E2F3,EGFL7,EIF1AX,EIF4E,ELF3,ELOC,EPCAM,ERF,ETNK1,EZH1,FANCG,FANCL,FEV,FLI1,FYN,GNA12,GNB1,GPS2,GREM1,H1-3,H1-4,H2AC11,H2AC16,H2AC17,H2AC6,H2BC11,H2BC12,H2BC17,H2BC4,H2BC5,H3-4,H3-5,H3C1,H3C10,H3C11,H3C12,H3C13,H3C14,H3C3,H3C4,H3C6,H3C7,H3C8,HDAC4,HDAC7,HIF1A,HLA-B,HOXB13,ICOSLG,IFNGR1,IGF1,IGF2,IKZF3,IL10,INHA,INPP4A,INPPL1,INSR,IRF1,IRF8,IRS1,JARID2,KAT6A,KMT2B,KMT5A,KNSTRN,LCK,LMO2,LZTR1,MAFB,MAPK3,MAPKAP1,MDC1,MECOM,MGA,MLLT10,MSI1,MST1,MTAP,MYB,NCOA3,NCSTN,NEGR1,NKX3-1,NOTCH4,NR4A3,NTHL1,NUF2,NUP98,NUTM1,PAK1,PAK5,PCBP1,PDGFB,PDPK1,PGR,PIK3C3,PIK3CD,PIK3R3,PLCG1,PLK2,PMAIP1,PMS1,PNRC1,PPP4R2,PRDM14,PRKD1,PTP4A1,PTPN2,PTPRD,PTPRS,RAB35,RAC2,RASA1,RBM15,RECQL,RHEB,RIT1,RPS6KA4,RPS6KB2,RRAGC,RRAS,RRAS2,RTEL1,RXRA,RYBP,SESN1,SESN2,SESN3,SETDB1,SH2D1A,SHOC2,SHQ1,SLX4,SMARCE1,SMC1A,SMC3,SMYD3,SOS1,SOX17,SPRED1,SS18,STK19,STK40,TAL1,TAP1,TAP2,TCL1A,TFE3,TGFBR1,TLX1,TLX3,TP53BP1,TRAF3,TRAF5,TYK2,U2AF2,UBR5,UPF1,USP8,VTCN1,XBP1,XIAP,YAP1,YES1,ABI1,ACTG1,ACVR1B,AFDN,AFF1,AFF4,AGO1,ALB,APLNR,ARFRP1,ARHGAP26,ARHGAP35,ARHGEF12,ARHGEF28,ARID3A,ARID3B,ARID3C,ARID4A,ARID4B,ARID5A,ARNT,ATIC,ATP6AP1,ATP6V1B2,ATXN2,ATXN7,BACH2,BCL11A,BCL2L2,BCL3,BCL7A,BTG2,CAMTA1,CARS1,CBFA2T3,CD22,CD28,CD70,CD74,CDX2,CEP43,CLTC,CLTCL1,CMTR2,CNTRL,COL1A1,CRBN,CREB1,CREB3L1,CREB3L2,CTNNA1,CTR9,CYP19A1,DDX10,DDX6,DNM2,EBF1,ECT2L,EGR1,ELF4,ELL,EML4,EMSY,EP400,EPOR,EPS15,ESCO2,ETAA1,EZHIP,EZR,FANCE,FANCF,FCGR2B,FCRL4,FGF10,FGF14,FGF23,FGF6,FHIT,FOXF1,FOXO3,FOXO4,FSTL3,FURIN,FUS,GAB1,GAB2,GAS7,GID4,GPHN,GTF2I,H1-5,H2BC8,H4C9,HERPUD1,HEY1,HIP1,HLA-C,HLF,HMGA1,HMGA2,HOXA11,HOXA13,HOXA9,HOXC11,HOXC13,HOXD11,HOXD13,HSP90AA1,HSP90AB1,IGH,IGK,IGL,IL21R,IL3,ITK,KBTBD4,KDSR,KIF5B,KLF5,KLHL6,KSR2,LASP1,LEF1,LPP,LRP1B,LTB,LYL1,MAD2L2,MGAM,MLF1,MLLT3,MLLT6,MN1,MOB3B,MPEG1,MRTFA,MSN,MUC1,MYH11,MYH9,NADK,NCOA2,NDRG1,NFE2,NFKB2,NIN,NRG1,NUMA1,NUP214,NUTM2A,PAFAH1B2,PAX3,PAX7,PAX8,PBX1,PCM1,PDE4DIP,PDK1,PDS5B,PER1,PGBD5,PICALM,PIGA,PLAG1,PML,POU2AF1,PPP2R2A,PRDM16,PRKACA,PRRX1,PSIP1,PTPN1,PTPRO,QKI,RABEP1,RAP1GDS1,RELN,REST,RHOH,RNF213,ROBO1,RPL22,RPN1,RSPO2,SAMHD1,SCG5,SDC4,SERPINB3,SERPINB4,SET,SETD1A,SETD1B,SETD3,SETD4,SETD5,SETD6,SETD7,SETDB2,SH3GL1,SLC34A2,SLFN11,SMARCA2,SMG1,SOCS3,SP140,SPRTN,SRSF3,SSX1,SSX2,SSX4,STAG1,TAF15,TAL2,TET3,TFG,TNFRSF17,TPM3,TPM4,TRA,TRB,TRD,TRG,TRIM24,TRIP11,TRIP13,USP6,VAV1,VAV2,WIF1,ZBTB16,ZMYM2,ZNF217,ZNF384,ZNF521,ZNF703,ZNRF3,ACKR3,ACSL3,ACSL6,ACTB,ACVR2A,ADGRA2,AFF3,AJUBA,APH1A,APOBEC3B,ASMTL,ASPSCR1,ATG5,ATP1A1,ATP2B3,BAX,BCL9L,BRD3,BRSK1,BTLA,BUB1B,CACNA1D,CAD,CANT1,CBLB,CBLC,CCDC6,CCN6,CCNB1IP1,CCNB3,CCT6B,CD36,CDH11,CHCHD7,CHD2,CHD4,CHIC2,CHN1,CILK1,CKS1B,CLIP1,CLP1,CNBP,CNOT3,COL2A1,CPS1,CRTC1,CRTC3,CSF1,CUL4A,CYP17A1,DAZAP1,DCTN1,DDB2,DDR1,DDX4,DDX41,DDX5,DKK1,DKK2,DKK3,DKK4,DUSP2,DUSP9,EIF3E,ELK4,ELN,ELP2,EPHB4,ERC1,ETS1,EXOSC6,EXT1,EXT2,FAF1,FAT4,FBXO31,FES,FGF12,FIP1L1,FLYWCH1,FNBP1,FRS2,GABRA6,GADD45B,GATA4,GATA6,GMPS,GOLGA5,GOPC,GPC3,GRM3,GTSE1,H3C15,H3P6,HIRA,HNRNPA2B1,HOOK3,HOXA3,HSD3B1,IKBKB,IKZF2,IL2,IL6ST,INPP5D,IRF2,IRS4,JAZF1,KAT6B,KCNJ5,KDM2B,KDM4C,KEL,KLF2,KLF3,KLF6,KLK2,KNL1,KTN1,LARP4B,LCP1,LIFR,LMNA,LRIG3,LRP5,LRP6,LRRK2,LTK,MAGED1,MAML2,MAP3K6,MAP3K7,MBD6,MDS2,MEF2C,MEF2D,MERTK,MIB1,MIDEAS,MKI67,MKNK1,MLLT11,MNX1,MTCP1,MYO18A,MYO5A,NAB2,NACA,NBEAP1,NCOA1,NCOA4,NFATC2,NFIB,NFKBIE,NOD1,NONO,NUTM2B,OLIG2,OMD,PAG1,PAK3,PARP2,PARP3,PASK,PATZ1,PC,PCLO,PCSK7,PDCD11,PHF1,PIK3C2B,POLQ,POU5F1,PPFIBP1,PPP1CB,PRCC,PRF1,PRKDC,PRSS1,PRSS8,PTK6,PTK7,PTPN13,PTPN6,PTPRB,PTPRC,PTPRK,RALGDS,RANBP2,RASGEF1A,RMI2,RNF217-AS1,RPL10,RPL5,RSPO3,RUNX2,S1PR2,SALL4,SBDS,SEC31A,SEPTIN5,SEPTIN6,SEPTIN9,SERP2,SFPQ,SFRP1,SFRP2,SFRP4,SIX1,SLC1A2,SLC45A3,SMARCA1,SNCAIP,SND1,SNX29,SOCS2,SOX10,SS18L1,STAT1,STAT2,STAT4,STIL,STRN,TAF1,TCEA1,TCF12,TCL1B,TEC,TERC,TFEB,TFPT,TFRC,TIPARP,TLE1,TLE2,TLE3,TLE4,TLL2,TMEM30A,TMSB4XP8,TNFRSF11A,TPR,TRIM27,TRIM33,TRRAP,TTL,TUSC3,TYRO3,WDCP,WDR90,WRN,XPA,XPC,YPEL5,YWHAE,YY1,YY1AP1,ZBTB20,ZBTB7A,ZFP36L1,ZMYM3,ZNF24,ZNF331,ZNF750".split(","));
        System.out.println(genes.size() + " genes");
        try {
            Scanner scan = new Scanner(new FileInputStream("targets.csv"));
            String line = scan.nextLine();
            while (scan.hasNextLine()) {
                String[] vals = scan.nextLine().split(",");
                String oldSymbol = vals[0];
                String newSymbol = vals[1];

                /*
                if (!newSymbol.equals(oldSymbol) || genes.contains(newSymbol)) {
                    Iterable<Target> results = targetRepository.findAllBySymbol(oldSymbol);
                    Iterator<Target> targetIterator = results.iterator();
                    Target target = null;
                    if (targetIterator.hasNext()) target = targetIterator.next();
                    if (target != null) {
                        target.setSymbol(newSymbol);
                        if (genes.contains(newSymbol)) target.setcBioPortal(true);
                        targetRepository.save(target);
                    }
                } */
                Iterable<Target> results = targetRepository.findAllBySymbol(newSymbol);
                Iterator<Target> targetIterator = results.iterator();
                Target target = null;
                if (targetIterator.hasNext()) target = targetIterator.next();
                if (target != null) {
                    if (genes.contains(newSymbol)) {
                        target.setcBioPortal(true);
                        targetRepository.save(target);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void updateTargetsSynonyms() {
        try {
            Scanner scan = new Scanner(new FileInputStream("targets.csv"));
            String line = scan.nextLine();
            while (scan.hasNextLine()) {
                String[] vals = scan.nextLine().split(",");
                String oldSymbol = vals[0];
                String newSymbol = vals[1];

                if (!newSymbol.equals(oldSymbol)) {
                    Iterable<Target> results = targetRepository.findAllBySymbol(newSymbol);
                    Iterator<Target> targetIterator = results.iterator();
                    Target target = null;
                    if (targetIterator.hasNext()) target = targetIterator.next();
                    if (target != null) {
                        String synonyms = target.getSynonyms();
                        if (synonyms.length() == 0) synonyms = oldSymbol;
                        else synonyms = oldSymbol + ";" + synonyms;
                        target.setSynonyms(synonyms);
                        targetRepository.save(target);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void addFullText() {
        List<FullText> fullTexts = fullTextRepository.findAll();
        fullTexts.parallelStream().filter(f -> !f.getPmId().contains("S")).forEach(f -> {
            Article a = articleRepository.findByPmId(f.getPmId());
            if (a.getFulltext() == null) {
                log.info("Setting PMID " + a.getPmId());
                a.setFulltext(f.getTextEntry());
                articleRepository.save(a);
            }
        });
    }

    @Test
    public void testCitation() {
        Optional<FullText> oft = fullTextRepository.findById("26072686");
        oft.ifPresent(ft -> {
            String text = ft.getTextEntry();
            int refstart = textService.getReferencesPosition(text);
            log.info("References start at {} out of {}", refstart, text.length());
            System.out.println(text.substring(refstart));
        });
    }

    @Test
    public void addWilma() {
        solrService.updateSolrArticles(195, 50000);
        /*String[] pmids = {"26657898", "24926260", "30398411", "33507258", "22510884", "26732095", "30683711", "29899858"};
        List<Article> articles = new ArrayList<>();
        List<FullText> fullTexts = new ArrayList<>();
        for (String pmid: pmids) {
            articles.add(articleRepository.findByPmId(pmid));
            fullTexts.add(fullTextRepository.findFullTextFor(pmid));
        }
        Gson gson = new Gson();
        System.out.println(gson.toJson(articles));
        System.out.println(gson.toJson(fullTexts));
        Page<Article> page = articleRepository.findAllFullText(PageRequest.of(0, 1000));
        System.out.println(page.getTotalElements());
        int i = 0;
        do {
            page = articleRepository.findAllFullText(PageRequest.of(i++, 1000));
            List<Article> articles = page.getContent();

            articles.parallelStream().forEach(a -> {
               FullText fts = fullTextRepository.findFullTextFor(a.getPmId());
               if (fts == null) {
                   a.setHasFullText(false);
                   a.setFulltext(null);
                   articleRepository.save(a);
                   log.info("Removed fulltext and updating solr for {}", a.getPmId());
                   solrService.addArticle(a.getPmId());

               }
            });
        } while (page.hasNext());*/
            //System.out.println(new Gson().toJson(articles.toArray()));

        //articleService.runSearch("Wilma Olson", 2000, 0);
        //articleService.runSearch("DNA structural biology", 2000, 1);
        //articleService.updateCitations(177, 50000);
        //articleService.addCitationRecords(177, 50000);
        //solrService.updateSolrArticles();
    }

    @Test
    public void solrTestSets() {
       /* try {
            SolrDocumentList docs = solrClientTool.find("knowledge", "pmid:16474404 AND -pmid_supporting:* AND text:\"BRAF G469E\"~10");
            for (SolrDocument doc : docs) {
                System.out.println((String)doc.getFieldValue("id"));
            }
        } catch (IOException|SolrServerException e) {
            log.error("Something went wrong");
            e.printStackTrace();
            return;
        }*/
        solrService.addArticle("324045");
        solrService.addArticle("310861");

        List<Variant> variants = variantRepository.findAllByGeneAndMutation("AKT1", "D44N");
        analyticsService.processVariants(variants);
    }

    @Test
    public void adder() {
        fullTextService.addArticle("1711486");
    }

    @Test
    public void addItem() {
        try {
            solrClientTool.delete("knowledge", "e9be501e");
        } catch (IOException|SolrServerException e) {
            e.printStackTrace();
        }
        //solrService.addArticle("33514736");
    }

    @Test
    public void testTierArticles() {
        /*try {
            solrClientTool.delete("knowledge", "cd5f4b55", true);
            solrClientTool.delete("knowledge", "724098dd", true);
            solrClientTool.delete("knowledge", "42c91d68", true);
            //solrClientTool.refreshCollection("knowledge");
        } catch (IOException|SolrServerException e) {
            e.printStackTrace();
            return;
        }*/

        List<Variant> variants = variantRepository.findAll();
        for (Variant v : variants) {
            if (v.getArticlesTier1() == null) v.setArticlesTier1(new ArrayList<>());
            if (v.getArticlesTier2() == null) v.setArticlesTier2(new ArrayList<>());
            v.setTotal(v.getArticlesTier1().size() + v.getArticlesTier2().size());
            variantRepository.save(v);
        }
    }

    @Test
    public void addAbraxane() {
        //mapService.addOneTerm("abraxane", List.of("nab-paclitaxel", "nanoparticle albumin–bound paclitaxel"), MapService.CalculationType.DRUG);
        solrService.addArticle("27880995");
    }

    @Test
    public void BRAFFunctionalData() {
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");
        variants = variants.stream().filter(v -> v.getGene().equals("BRAF") && v.getKeywordScores().size() > 0).collect(Collectors.toList());
        System.out.println("PMID BRAF_alteration keyword_fscore alteration_score");
        for (Variant v: variants) {
            for (int i = 0; i < v.getKeywordScores().size(); i++) {
                //System.out.println(i + " " + v.getKeywordScores().size() + " " + v.getMutation());
                if (v.getKeywordScores().get(i) == 0) continue;
                if (i < 5) {
                    System.out.printf("%s %s %d %d\n", v.getArticlesTier1().get(i), (v.getMutation().endsWith(" Fusion") ? v.getMutation().split(" ")[0] : v.getMutation().replaceAll(" ", "_")), v.getKeywordScores().get(i), v.getScores1().get(i)+8);
                } else {
                    System.out.printf("%s %s %d %d\n", v.getArticlesTier2().get(i-5), (v.getMutation().endsWith(" Fusion") ? v.getMutation().split(" ")[0] : v.getMutation().replaceAll(" ", "_")), v.getKeywordScores().get(i), v.getScores2().get(i-5)+8);
                }
            }
        }
    }

    @Test
    public void addDrugTypes() {
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader("drugs.csv"));) {
            String[] values = null;
            System.out.println(csvReader.readNext()[0]);
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
            for (List<String> items: records) {
                log.info(items.get(0));
                DrugMap dm = drugMapRepository.findByDrug(items.get(0).toLowerCase());
                if (dm == null) {
                    log.info("No item {} in DrugMaps, creating", items.get(0));
                    dm = new DrugMap();
                    dm.setDrug(items.get(0).toLowerCase());
                }
                dm.setCancerDrug(true);
                String[] synonyms = items.get(2).split("\\|");
                log.info("{} synonyms", synonyms.length);
                for (String syn: synonyms) {
                    if (syn.length() > 50 || syn.equalsIgnoreCase(items.get(0))) continue;
                    if (dm.getSynonyms() == null || dm.getSynonyms().equals("")) dm.setSynonyms(syn);
                    else if (dm.getSynonyms().length() > 0) dm.setSynonyms(dm.getSynonyms() + "," + syn);
                }

                drugMapRepository.save(dm);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void testProcessVariants() {
        List<Variant> variants = variantRepository.findAllByGene("AKT1");
        List<Variant> variant2 = variantRepository.findAllByGene("AKT2");
        List<Variant> variant3 = variantRepository.findAllByGene("AKT3");
        variants.addAll(variant2);
        variants.addAll(variant3);
        variants = variants.stream().filter(v -> v.getKey() != null && v.getKey().equals("AKTtest")).collect(Collectors.toList());
        System.out.println(variants);
        byte[] result = analyticsService.processVariants(variants);
        try {
            File file = new File("output.xlsx");
            OutputStream os = new FileOutputStream(file);
            os.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void addSup() {
        fullTextService.addSupplementary();
    }

    @Test
    public void addFullTextGene() {
        List<Variant> variants = variantRepository.findAllByGene("AKT1");
        GeneMap gm = geneMapRepository.findBySymbol("akt1");
        for (Variant variant: variants) {
            String mutation = variant.getMutation().toLowerCase();
            if (mutation.endsWith(" fusion")) mutation = mutation.substring(0, mutation.indexOf(" fusion")).trim();
            MutationMap mm = mutationMapRepository.findBySymbol(mutation);
            Set<Integer> PMIDs = gm.getListAsSet();
            //PMIDs.retainAll(mm.getListAsSet());
            for (Integer PMID : PMIDs) {
                Article a = articleRepository.findByPmId(PMID+"");
                if (a.getFulltext() == null) {
                    log.info("Attempting full-text add");
                    fullTextService.addArticle(a);
                }
            }
        }
    }

    @Test
    public void measureFullTextGene() {
        int total = 0, total2 = 0;
        int count = 0, count2 = 0;
        List<String> synonyms = null;
        //List<Variant> variants = variantRepository.findAllByGene("BRAF");
        Optional<Target> ot = targetRepository.findBySymbol("AKT3");
        if (ot.isPresent()) synonyms = Arrays.asList(ot.get().getSynonyms().split(";"));
        List<String> pmids = solrService.searchSolr(10000, Collections.singletonList("AKT3"), Collections.singletonList(synonyms), null, null, null, null, null, null, null, null, DateTime.parse("2010-01-01T00:48:00.000Z")).getPmIds();
        for (String pmid: pmids) {
            if (pmid.contains("S")) pmid = pmid.substring(0, pmid.indexOf("S"));
            Article a = articleRepository.findByPmId(pmid);
            if (a == null) {
                log.error("Missing article {}", pmid);
                articleService.addArticlePMID(pmid);
            }
            if (a.getFulltext() == null) {
                log.info("Attempting full-text add");
                if (a.getPmcId() == null) {
                    total++;
                    if (fullTextService.addArticle(a)) count++;
                } else {
                    total2++;
                    if (fullTextService.addArticle(a)) count2++;
                }
            }
        }
        log.info("Fraction found of DOI is {}, overall fraction of PDFs found is {}", (double)count/(double)total, (double)(count+count2)/(total+total2));
    }

    @Test
    public void addFullTextGeneAll() {
        //List<Variant> variants = variantRepository.findAllByGene("ROS1");
        GeneMap gm = geneMapRepository.findBySymbol("ros1");
        List<Integer> PMIDs = gm.getListAsSet().parallelStream().collect(Collectors.toList());
        Collections.shuffle(PMIDs);
        for (Integer PMID : PMIDs) {
            Article a = articleRepository.findByPmId(PMID+"");
            if (a.getPmcId() != null && a.getFulltext() == null) {
                log.info("Attempting full-text add");
                fullTextService.addArticle(a);
            }
        }

    }

    @Test
    public void countSuccesses() {
        List<Variant> variants = variantRepository.findAllByKey("AAVariants");
        int text = (int)variants.stream().filter(v -> v.getConsensusPMIds() != null && !v.getConsensusPMIds().equals("")).count();
        int text2 = (int)variants.stream().filter(v -> v.getAutomatedPMIds() != null && !v.getAutomatedPMIds().equals("")).count();
        int text3 = (int)variants.stream().filter(v -> v.getConsensusPMIds() != null && v.getConsensusPMIds().split(",").length > 9).count();
        int text4 = (int)variants.stream().filter(v -> v.getConsensusPMIds() != null && v.getConsensusPMIds().split(",").length > 4).count();
        int count = variants.size();
        System.out.println(text + " out of " + count);
        System.out.println(text2 + " out of " + count);
        System.out.println((double)text / (double)count);
        System.out.println((double)text2 / (double)count);
        System.out.println((double)text3 / (double)count);
        System.out.println((double)text4 / (double)count);
    }

    @Test
    public void finalSolrAdds() {
        solrService.updateSolrArticles(1048, 10000);
    }

    @Test
    public void testSpell() {
        SpellChecking spellChecking = new SpellChecking();
        System.out.println(spellChecking.checkSpelling("hello"));
    }

    @Test
    public void testRestore() {
        org.magicat.pdf.Document document = org.magicat.pdf.Document.readDocument(articleRepository.findByPmId("19376813"), fullTextRepository, gridFsTemplate);
        List<Section> sections = document.getSections();
        for (Section section: sections) {
            if (section.getImage() != null) section.getImage().restoreImage();
            System.out.println(section);
        }
    }

    @Test
    void testLevenshtein() {
        String s1 = "HelloSystem", s2 = "World";
        int x = StringUtils.getLevenshteinDistance(s1, s2);
        int len = Math.max(s1.length(), s2.length());
        System.out.println("Score is: " + (100*(len - x) / (double)len) + "%");
    }

    @Test
    public void multiHighlight() {
        final SpellChecking spellChecking = new SpellChecking();
        Page<FullText> page = fullTextRepository.findAll(PageRequest.of(0, 24));
        List<FullText> fullTexts = page.getContent();
        fullTexts.parallelStream().forEach(ft -> {
            try {
                PDFHighlighter hl = new PDFHighlighter(); //28947956
                hl.setFullTextService(fullTextService);
                hl.analyze(articleRepository.findByPmId(ft.getPmId()), spellChecking);
                org.magicat.pdf.Document document = hl.getDocument();
                document.stripArticleData();
                document.annotate();
                document.writeDocument(fullTextRepository, gridFsTemplate);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testHighlight() {
        try {
            PDFHighlighter hl = new PDFHighlighter(); //28947956
            SpellChecking spellChecking = new SpellChecking();
            //hl.setArticleRepository(articleRepository);
            //hl.setFullTextService(fullTextService);
            //hl.highlight("26698910.pdf", "outHighlight.pdf", Arrays.asList("RT-PCR Reactions", "disease relapses", "small-molecule inhibitor", "ALK", "next-generation sequencing", "Applied Biosystems"));
            //System.out.println(hl.analyze("31514305"));
            long startTime = System.nanoTime();
            hl.setFullTextService(fullTextService);
            hl.analyze(articleRepository.findByPmId("34301786"), spellChecking);
            org.magicat.pdf.Document document = hl.getDocument();
            document.stripArticleData();
            document.annotate();
            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000000.0; // in milliseconds;
            System.out.printf("Parsing article took %8.2f milliseconds\n", duration);
            List<Section> sections = document.getSections();
            for (Section section: sections) {
                System.out.println(section);
                if (section.getImage() != null) {
                    section.getImage().saveImage(section.getHeading().replace(".", "") + ".png");
                }
            }
            document.writeDocument(fullTextRepository, gridFsTemplate);
            //Map<Integer, List<PDFHighlighter.PageText>> item = hl.getPageMap();
            //for (Integer val: item.keySet()) {
            //    System.out.println(val);
            //}
            //Integer val = 1;
            //System.out.println(item.get(val));
            //val = 2;
            //System.out.println(item.get(val));
            //val = 7;
            //System.out.println(item.get(val));
            //val = 8;
            //System.out.println(item.get(val));
            //String txt = fullTextRepository.findFullTextFor("26698910").getTextEntry();
            //List<String> result = textService.nameFinder(txt);
            //System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCrummyText() {
       // new PDFTextStripperByArea().getText()
    }

    @Test
    public void testGetCuratedPDFs() {
        variantService.getArticlePDFs(10000, "AKT");
    }

    @Test
    public void articlesToSolr() {
        System.out.println("Running import to Solr");
        Page<Article> page;
        int i = 15;
        do {
            System.out.println("Iteration " + i);
            page = articleRepository.findAll(PageRequest.of(i++, 500000));
            List<Article> articles = page.getContent();
            System.out.println(articles.size() + " items");
            articles.parallelStream().forEach(a -> {
                Map<String, List<String>> articleMap = new HashMap<>();
                String text = Article.toText(a);
                articleMap.put("text", Collections.singletonList(text));
                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                try {
                    solrClientTool.add("knowledge", articleMap);
                } catch (IOException|SolrServerException e) {
                    e.printStackTrace();
                }
            });
        } while (page.hasNext());
    }

    @Test
    public void refreshSolr() {
        solrClientTool.refreshCollection("knowledge");
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
    public void TestAck() {
        System.out.println(StringUtils.getLevenshteinDistance("ACKNOWLEDGEMENTS", "ACKOWLEDGEMENTS"));
    }

    @Test
    public void setSolrId() {
        System.out.println("Running Solr Hashcode update for MongoDB");
        Page<Article> page;
        int i = 0;
        do {
            System.out.println("Iteration " + i);
            page = articleRepository.findAll(PageRequest.of(i++, 500000));
            List<Article> articles = page.getContent();
            System.out.println(articles.size() + " items");
            articles.parallelStream().forEach(a -> {
                Map<String, List<String>> articleMap = new HashMap<>();
                String text = Article.toText(a);
                articleMap.put("text", Collections.singletonList(text));
                articleMap.put("authors", Collections.singletonList(a.getAuthors()));
                articleMap.put("pmid", Collections.singletonList(a.getPmId()));
                a.setSolrId(Integer.toHexString(articleMap.hashCode()));
                try {
                    if (!solrClientTool.exists("knowledge", Integer.toHexString(articleMap.hashCode()))) {
                        String oldText = toText(a);
                        Map<String, List<String>> articleMapOld = new HashMap<>();
                        articleMapOld.put("text", Collections.singletonList(oldText));
                        articleMapOld.put("authors", Collections.singletonList(a.getAuthors()));
                        articleMapOld.put("pmid", Collections.singletonList(a.getPmId()));
                        if (a.getFulltext() != null && solrClientTool.exists("knowledge", Integer.toHexString(articleMapOld.hashCode()))) {
                            log.info("Found a record to update " + a.getPmId());
                            solrClientTool.delete("knowledge", Integer.toHexString(articleMapOld.hashCode()));
                        }
                        solrClientTool.add("knowledge", articleMap);
                    }
                } catch (SolrServerException|IOException e) {
                    e.printStackTrace();
                }
                articleRepository.save(a);
            });
        } while (page.hasNext());
    }

    @Test
    public void runNewVariant() throws IOException {
        Variant v = variantRepository.findAllByGeneAndMutation("EGFR", "KIF5B-EGFR fusion").get(0);
        //v.setGene("EGFR");
        //v.setMutation("KIF5B-EGFR fusion");
        variantRepository.save(v);
        Files.write(Paths.get("kif5b-egfr.xlsx"), analyticsService.processVariants(Collections.singletonList(v)));
    }

    @Test
    public void runNewSpreadsheetVariant() {
        List<Variant> variants = variantRepository.findAllByKey("AKTtest");
        analyticsService.processVariants(variants);
    }


    @Test
    public void readPMIDs() {
        String data = readTSV();
        String[] data2 = data.split(", ");
        Set<String> pmids = new HashSet<>();
        for (String p : data2) {
            System.out.println(p);
            pmids.add(p);
        }
        for (String pmid : pmids) {
            try {
                if (articleService.addArticlePDF(pmid)) {
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
                            fullTextService.addArticle(pmid);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public String readTSV() {
        StringBuffer result = new StringBuffer(10000);
        try (BufferedReader TSVReader = new BufferedReader(new FileReader(new File("allAnnotatedVariants.txt")))) {
            String line;
            TSVReader.readLine();
            int i = 0;
            while ((line = TSVReader.readLine()) != null) {
                //System.out.println(i++);
                String[] items = line.split("\t");
                Variant variant = new Variant();
                variant.setGene(items[5]);
                variant.setMutation(items[7]);
                variant.setOncogenicity(items[9]);
                variant.setMutationEffect(items[10]);
                variant.setCuratedPMIds(items[11]);
                if (variantRepository.findAllByGeneAndMutation(items[5], items[7]) == null) {
                    variant = variantRepository.save(variant);
                    System.out.println(variant + " added!");
                }

                if (i == 0) result.append(items[11]);
                else result.append(", ").append(items[11]);
                    //System.out.println(items[11]);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    @Test
    public void populateVariants() {
        try (BufferedReader TSVReader = new BufferedReader(new FileReader(new File("allAnnotatedVariants.txt")))) {
            String line;
            TSVReader.readLine();
            int i = 0;
            while ((line = TSVReader.readLine()) != null) {
                //System.out.println(i++);
                String[] items = line.split("\t");
                Variant variant = new Variant();
                variant.setGene(items[5]);
                variant.setMutation(items[7]);
                variant.setOncogenicity(items[9]);
                variant.setMutationEffect(items[10]);
                if (items.length > 11) variant.setCuratedPMIds(items[11]);
                else variant.setCuratedPMIds("");
                if (variantRepository.findAllByGeneAndMutation(items[5], items[7]) == null) {
                    variant = variantRepository.save(variant);
                    System.out.println(variant + " added!");
                }

                //System.out.println(items[11]);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void addArticle() {
        if (!articleService.addArticlePMID("9917397")) log.info("Already existed");
    }

    @Test
    public void articlesDeleteSolr() throws IOException, SolrServerException {
        Map<String, List<String>> map;
        Page<Article> page;
        int i = 0;
        do {
            map = new HashMap<>();
            page = articleRepository.findAll(PageRequest.of(i, 500000));
            List<Article> articles = page.getContent();
            for (Article a: articles) {
                SolrDocumentList results = solrClientTool.find("knowledge", "pmid:\"" + a.getPmId() + "\"");
                for (int j = 0; j < results.size(); j++) {
                    SolrDocument d = results.get(j);
                    solrClientTool.delete("knowledge", d.getFieldValue("id").toString());
                }
                String text = Article.toText(a);
                map.put("text", Collections.singletonList(text));
                map.put("authors", Collections.singletonList(a.getAuthors()));
                map.put("pmid", Collections.singletonList(a.getPmId()));
                solrClientTool.add("knowledge", map);
            }
            i++;
        } while (page.hasNext());
    }

    @Test
    public void testFullText() throws IOException, SolrServerException {
        //List<FullText> items = fullTextRepository.findAll();
        //for (FullText f : items) {
        //    System.out.println(f.getPmId() + " " + f.getTextEntry());
        //}
        fullTextService.addAllArticles();
    }

    @Test
    public void testFullText2() throws IOException, SolrServerException {
        //List<FullText> items = fullTextRepository.findAll();
        //for (FullText f : items) {
        //    System.out.println(f.getPmId() + " " + f.getTextEntry());
        //}
        fullTextService.addSupplementary();
    }

    @Test
    public double[][] articleMatrixGenesFractionSymmetric() {
        List<GeneMap> geneMapList = geneMapRepository.findAll();
        double[][] result = new double[geneMapList.size()][geneMapList.size()];
        for (int i = 0; i < geneMapList.size(); i++) {
            for (int j = i; j < geneMapList.size(); j++) {
                if (i == j) result[i][j] = 1.0;
                else {
                    Set<Integer> primary = geneMapList.get(i).getListAsSet();
                    Set<Integer> secondary = geneMapList.get(j).getListAsSet();
                    GeneMap primaryCopy = geneMapList.get(i).dup();
                    Set<Integer> intersection = primaryCopy.getListAsSet();
                    intersection.retainAll(secondary);
                    primary.addAll(secondary);              // <------- extra line!
                    if (primary.size() == 0) {
                        result[i][j] = 0.0;
                        result[j][i] = 0.0;
                    } else {
                        result[i][j] = intersection.size() / (double) (primary.size());
                        result[j][i] = result[i][j];
                    }
                }
                System.out.print(result[i][j] + " ");
            }
            System.out.println();
        }
        return result;
    }

    @Test
    public double[][] articleMatrixGenesFraction() {
        List<GeneMap> geneMapList = geneMapRepository.findAll();
        double[][] result = new double[geneMapList.size()][geneMapList.size()];
        for (int i = 0; i < geneMapList.size(); i++) {
            for (int j = 0; j < geneMapList.size(); j++) {
                if (i == j) result[i][j] = 1.0;
                else {
                    Set<Integer> primary = geneMapList.get(i).getListAsSet();
                    Set<Integer> secondary = geneMapList.get(j).getListAsSet();
                    GeneMap primaryCopy = geneMapList.get(i).dup();
                    Set<Integer> intersection = primaryCopy.getListAsSet();
                    intersection.retainAll(secondary);
                    if (primary.size() == 0) result[i][j] = 0.0;
                    else result[i][j] = intersection.size() / (double)primary.size();
                }
                System.out.print(result[i][j] + " ");
            }
            System.out.println();
        }
        return result;
    }

    @Test
    void annotateMAPK1() {
        List<Variant> variants = variantRepository.findAllByGene("MAPK1");
        variantService.getArticlePDFs(5000, "MAPK1");
        variantService.getArticlePDFs(5000, "ERK2");
        analyticsService.processVariants(variants);
    }

    @Test
    void annotateBRCA() {
        variantService.getArticlePDFs(10000, "BRCA1");
        variantService.getArticlePDFs(10000, "BRCA2");
        variantService.getArticlePDFs(10000, "ERBB2");
        variantService.getArticlePDFs(10000, "HER2");
    }

    @Test
    void testArticleIndexRepository() {
        List<String> pmIds = Arrays.asList("33603171", "33750796", "33837205", "33867902");
        System.out.println(articleIndexRepository.findAllByPmIdIn(pmIds));
        System.out.println(articleIndexRepository.findAllById(pmIds));
    }

    @Test
    void runMapSteps() {
        mapService.updateMaps(UpdateConfig.builder().genes(true).mutations(true)
                        .cancers(true).drugs(true).page(0).pageSize(1000000).build());
    }

    @Test
    void readConfig() {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(UpdateConfig.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            UpdateConfig uc = (UpdateConfig)jaxbUnmarshaller.unmarshal(new File("UpdateConfig.xml"));
            System.out.println(uc.getGenes() + " " + uc.getMutations() + " " + uc.getCancers() + " " + uc.getDrugs());
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    void readItems() {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(UpdateItems.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            UpdateItems ui = (UpdateItems)jaxbUnmarshaller.unmarshal(new File("UpdateItems.xml"));
            System.out.println(ui);
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    void runArticleTest() {
        final int pageLimit = 50;
        int pageNumber = 0;
        Page<Article> pages;
        for (int i = 0; i < 20; i++) {
            pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
            processArticles(pages.getContent());
            long t = pages.stream().parallel().filter((a) -> a.getPublicationDate() != null).count();
            pages.stream().filter((a) -> a.getPublicationDate() != null).forEach(System.out::println);
            System.out.println(t + " articles");
        }
    }

    @Test
    void readNewCSV() {
        Set<String> genes = new TreeSet<>();
        try (Scanner scan = new Scanner(new FileInputStream("OncoKB_genes_with_aliases.csv"));) {
            String line = scan.nextLine();
            while (scan.hasNextLine()) {
                line = scan.nextLine();
                genes.add(line.split(",")[1]);
            }
        } catch (IOException e) {

        }
        for (String gene : genes) {
            System.out.println(gene);
        }
    }

    @Test
    void printTime() {
        Article a = articleRepository.findByPmId("34420429");
        log.info(a.getPublicationDate().toDateTime(DateTimeZone.UTC).toString());
    }

    @Test
    void testAddSupplementary() {
        fullTextService.addSupplementary();
    }

    @Test
    void nameVariants() {
        List<Variant> variants = variantRepository.findAllKeyless();
        for (Variant v : variants) {
            v.setKey("AAVariants");
        }
        variantRepository.saveAll(variants);
    }

    @Test
    void runSearches() {
        int size = 500;
        String XMLFile = "test-123.xml";
        String term = "\"BRAF\" OR \"G469\"";
        try {
            String r = ProcessUtil.runScript("python3 python/pubmed.py " + size + " " + XMLFile + " " + term.trim());
            System.out.println(r);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void addTestProjectLists() {
        /*
        List<ProjectList> keywords = projectListRepository.findIdExpression("keywordListM");
        for (int i = 0; i < keywords.size(); i++) {
            boolean found = false;
            if (keywords.get(i).getNames().get(0).contains("express")) {
                keywords.get(i).setSynonyms(Arrays.asList("expression", "expressed", "expresses"));
                found = true;
            }
            if (keywords.get(i).getNames().get(0).contains("transfect")) {
                keywords.get(i).setSynonyms(Arrays.asList("transfection", "transfected"));
                found = true;
            }
            if (keywords.get(i).getNames().get(0).contains("activ")) {
                keywords.get(i).setSynonyms(Arrays.asList("activates", "activate", "activating", "inactivate", "inactivates", "inactivating"));
                found = true;
            }
            if (keywords.get(i).getNames().get(0).contains("cell line")) {
                keywords.get(i).setSynonyms(List.of("cell line", "cell lines"));
                found = true;
            }
            if (keywords.get(i).getNames().get(0).contains("prolifer")) {
                keywords.get(i).setSynonyms(Arrays.asList("proliferate", "proliferates", "proliferation"));
                found = true;
            }
            if (keywords.get(i).getNames().get(0).contains("repress")) {
                keywords.get(i).setSynonyms(Arrays.asList("represses", "repressed", "repression"));
                found = true;
            }
            if (found) projectListRepository.save(keywords.get(i));
            else {
                log.info("MISSING ELEMENT {}", keywords.get(i).getNames());
                return;
            }
        }*/

        /*ProjectList newp1 = new ProjectList("keywordListM1", Collections.singletonList("\"express*\""), null);
        ProjectList newp2 = new ProjectList("keywordListM2", Collections.singletonList("\"transfect*\""), null);
        ProjectList newp3 = new ProjectList("keywordListM3", Collections.singletonList("\"*activat*\""), null);
        ProjectList newp4 = new ProjectList("keywordListM4", Collections.singletonList("\"cell line\""), null);
        ProjectList newp5 = new ProjectList("keywordListM5", Collections.singletonList("\"proliferat*\""), null);
        projectListRepository.saveAll(Arrays.asList(newp1, newp2, newp3, newp4, newp5));
        ProjectList newp6 = new ProjectList("keywordListM6", Collections.singletonList("\"repress*\""), null);
        projectListRepository.save(newp6);*/

        //List<ProjectList> pl = projectListRepository.findIdExpression("keywordListM");
        //log.info("{} items pulled from projectLists records", pl.size());
                  //Variant v = variantRepository.findByDescriptor("BRAF:G469A::");  // G469A D594G T599I
        List<Variant> variants = variantRepository.findAll();
        //variants.addAll(variantRepository.findAllByKey("AKTtest"));
        //variants.addAll(variantRepository.findAllByKey("AAVariants"));
        //analyticsService.scoreKeywords(variants, pl, false, true);
        variantService.rescoreArticlesHongxin(variants);
        int[] countAlt = new int[11];
        int[] countF = new int[11];
        int[] countTotal = new int[41];
        int total = 0;
        for (Variant v : variants) {
            if (v.getNewScores() == null || v.getNewScores().size() == 0) continue;
            for (Variant.NewScore score : v.getNewScores()) {
                countAlt[score.getAltScore()]++;
                countF[score.getFScore()]++;
                countTotal[score.getTotal()]++;
                total++;
            }
        }
        System.out.println(total + " articles scored");
        System.out.println("Alteration Score Histogram");
        for (int i = 0; i < 11; i++) {
            System.out.printf("%d %d\n", i, countAlt[i]);
        }
        System.out.println("\n");
        System.out.println("Functional Score Histogram");
        for (int i = 0; i < 11; i++) {
            System.out.printf("%d %d\n", i, countF[i]);
        }
        System.out.println("\n");
        System.out.println("Total Score Histogram");
        for (int i = 0; i < 41; i++) {
            System.out.printf("%d %d\n", i, countTotal[i]);
        }
        System.out.println("\n");
    }

    @Test
    void badPDF() {
        System.out.println(TikaTool.parseDocument("300.pdf") + "\n\n\n\n\n" + TikaTool.parseDocumentHTML("300.pdf"));

    }

    @Test
    void ERBB2Curate() {
        List<Variant> variants = variantRepository.findAll().stream().filter(x -> x.getGene().equals("ERBB2")).collect(Collectors.toList());
        analyticsService.processVariants(variants);
    }

    @Test
    void readGSON() {
        try (Reader in = new InputStreamReader(new FileInputStream("cancerGeneList"))) {
            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<Map<String,Object>>>(){}.getType();
            List<Map<String, Object>> item = gson.fromJson(in, collectionType);
            System.out.println(item);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testHighlightSentences() {
        List<ProjectList> pl = projectListRepository.findIdExpression("keywordListM");
        textService.findKeywordSentences("D594G", pl, "18794803", false);
        textService.findKeywordOnlySentences("D594G", pl, "18794803", false);

    }

    @Test
    void explanationMap() {
        try {
            SolrClientTool.ResultMap resultMap = solrClientTool.queryHighlight("knowledge", "\"cell line\"", 10);
            //System.out.println(resultMap.getExplainMap());
            List<SolrItem> items = SolrClientTool.documentsToItems(resultMap.getDocs());
            //System.out.println(resultMap.getHighlightingMap());
            for (SolrItem s : items) {
                System.out.println(s.getId() + " is " + s.getPmid());
                System.out.println(resultMap.getHighlightingMap().get(s.getId()));
                for (String key : resultMap.getHighlightingMap().get(s.getId()).keySet()) {
                    System.out.println("key = " + key);
                    System.out.println("value = " + resultMap.getHighlightingMap().get(s.getId()).get(key));
                }
                System.out.println(s.getPmid() + ": " + s.getText());
                System.out.println(resultMap.getExplainMap().get(s.getId()));
            }
        } catch (IOException|SolrServerException e) {
            e.printStackTrace();
        }
    }

    @Test
    void benchmarkHongxin1() {
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");

        long startTime = System.nanoTime();
        analyticsService.processVariants(variants);
        long finalTime = System.nanoTime();
        double duration = (finalTime - startTime) / 1000000.0; // in milliseconds;
        double durationS = duration / 1000.0;  // in seconds
        System.out.printf("Running hongxin1 took %8.2f milliseconds\n", duration);
        System.out.printf("Running hongxin1 took %8.2f seconds\n", durationS);
    }

    public String spacify(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (i != 0 && i % 5 == 0) sb.append(" ");
            sb.append(input.charAt(i));
        }
        return sb.toString();
    }

    @Test
    void readFasta() {
        ScheduledTasks.updateArticles = false;
        solrClientTool.setCollection("t2t");
        solrClientTool.setReloadRate(30);
        // 22 chromosomes, x, y, mtDNA
        String[][] gb2rs = {{"CP068277.2", "NC_060925.1"}, {"CP068276.2", "NC_060926.1"}, {"CP068275.2", "NC_060927.1"}, {"CP068274.2", "NC_060928.1"}, {"CP068273.2", "NC_060929.1"}, {"CP068272.2", "NC_060930.1"}, {"CP068271.2", "NC_060931.1"}, {"CP068270.2", "NC_060932.1"}, {"CP068269.2", "NC_060933.1"}, {"CP068268.2", "NC_060934.1"}, {"CP068267.2", "NC_060935.1"}, {"CP068266.2", "NC_060936.1"}, {"CP068265.2", "NC_060937.1"}, {"CP068264.2", "NC_060938.1"}, {"CP068263.2", "NC_060939.1"}, {"CP068262.2", "NC_060940.1"}, {"CP068261.2", "NC_060941.1"}, {"CP068260.2", "NC_060942.1"}, {"CP068259.2", "NC_060943.1"}, {"CP068258.2", "NC_060944.1"}, {"CP068257.2", "NC_060945.1"}, {"CP068256.2", "NC_060946.1"}, {"CP068255.2", "NC_060947.1"}, {"CP086569.2", "NC_060948.1"}, {"CP068254.1", "NA (mtDNA)"}};
        Map<String, String> GB2RS = new HashMap<>();
        Map<String, Object> itemMap = new HashMap<>();
        for (String[] pair: gb2rs)
            GB2RS.put(pair[0], pair[1]);

        try (BufferedReader is = new BufferedReader(new FileReader("GCA_009914755.4_T2T-CHM13v2.0_genomic.fna"));) {
            //PrintWriter out = new PrintWriter("letters.txt");
            Alphabet alphabet = null;
            RichSequenceIterator it = RichSequence.IOTools.readFastaDNA(is, new SimpleNamespace("knowledge"));
            int x = 0;
            List<Map<String, Object>> items = new ArrayList<>();
            while (it.hasNext()) {
                RichSequence rs = it.nextRichSequence();
                String name = rs.getName();
                String refSeq = GB2RS.get(name);
                String version = rs.getName() + "." + rs.getVersion();
                String description = rs.getDescription();
                alphabet = rs.getAlphabet();
                //System.out.println(x + ".\tname="+name+"\tversion="+version+"\tt");
                System.out.println(x + ".  Circular: " + rs.getCircular() + ", Length = " + rs.length() + ", description: " + rs.getDescription() + "   name:" + rs.getName() + "\n\taccession:" + rs.getAccession() + ", GenBank URN:" + rs.getURN() + ", RefSeq:" + refSeq + ", version=" + version + ", alphabet = " + alphabet.getName());
                //System.out.println(rs);
                Iterator<String> chunks = Splitter.fixedLength(200).split(rs.seqString()).iterator();
                Iterator<String> chunks2 = Splitter.fixedLength(200).split(rs.seqString().substring(100)).iterator();
                int i = 0;
                while (chunks.hasNext()) {//for (int i = 0; i < rs.seqString().length() / 200; i++) {
                    log.info("Adding item {} for {} to {}", i, i * 200, (i + 1) * 200);
                    //itemMap = new HashMap<>();
                    itemMap.put("seq", spacify(chunks.next()));
                    itemMap.put("position", i * 200 + 1);
                    itemMap.put("name", name);
                    itemMap.put("genbank", gb2rs[x][0]);
                    itemMap.put("refseq", gb2rs[x][1]);
                    if (x < 22) {
                        itemMap.put("chromosome", "Chr " + (x + 1));
                    } else if (x == 22) itemMap.put("chromosome", "X");
                    else if (x == 23) itemMap.put("chromosome", "Y");
                    else if (x == 24) itemMap.put("chromosome", "mtDNA");
                    items.add(new HashMap<>(itemMap));
                    if (chunks2.hasNext()) {
                        itemMap.put("seq", spacify(chunks2.next()));
                        itemMap.put("position", i * 200 + 101);
                        items.add(new HashMap<>(itemMap));
                        //solrClientTool.addItem(itemMap);
                    }
                    i++;

                    /*try {
                        items.add(itemMap);
                        solrClientTool.addItem(itemMap);
                        if (i*200+100 < rs.seqString().length() && ((i+1)*200 < rs.seqString().length())) {
                            itemMap.put("seq", spacify(rs.seqString(), i*200+100, Math.min((i+1)*200+100, rs.seqString().length())));
                            itemMap.put("position", i*200+101);
                            solrClientTool.addItem(itemMap);
                        }
                    } catch (SolrServerException|IOException e) {
                        log.error("Error adding item: {}", e.getMessage());
                    }*/
                }

                try {
                    log.info("Adding {} items to Solr", items.size());
                    solrClientTool.addItems(items);
                    items = new ArrayList<>();
                    solrClientTool.commit();
                } catch (SolrServerException | IOException e) {
                    log.error("Error on commit: {}", e.getMessage());
                }
                //out.println(x + ". " + rs.seqString());
                x++;

                //out.close();
            }
        } catch(IOException | BioException e){
            e.printStackTrace();
        }

    }


    @Test
    void clusterTexts() {
        List<Variant> variants = variantRepository.findAllByKey("hongxin1");
        Optional<Variant> ovt = variants.stream().filter(v -> v.getGene().equals("BRAF") && v.getMutation().equals("")).findAny();
        Variant variant = null;
        if (ovt.isPresent()) {
            variant = ovt.get();
            List<String> pmids = solrService.searchSolr(10000, Collections.singletonList("BRAF"), Collections.singletonList(Arrays.asList("B-RAF", "BRAF1", "B-RAF1")),
            null, null, null, null, null, null, null, null, null).getPmIds();//DateTime.now().minusDays(13));
            if (variant.getAutomatedPMIds() != null) {
                log.info("There are {} articles", pmids.size());
                List<Article> articles = new ArrayList<>();
                if (pmids.size() > 1024) for (List<String> partition: Lists.partition(pmids, 1024))
                  articles.addAll(articleRepository.findAllByPmIdIn(partition));
                else articles.addAll(articleRepository.findAllByPmIdIn(pmids));
                List<Cluster<org.carrot2.clustering.Document>> clusters = Carrot2Util.carrot2Cluster(articles);
            }
        } else {
            log.error("FAILED TO FIND VARIANT!");
        }
    }

    @Test
    void runQueryTest() throws IOException, SolrServerException {
        SolrDocumentList sdl = solrClientTool.find("knowledge", "attr_content:kras");
        System.out.println("Max match: " + sdl.getMaxScore());
        System.out.println("Ndocuments = " + sdl.size());
        for (int i = 0; i < sdl.size(); i++) {
            for (String name : sdl.get(i).getFieldNames()) {
                System.out.println(name + ": " + sdl.get(i).getFieldValue(name));
            }
        }
    }

    @Test
    void cleanupMaps() {
        List<DrugMap> drugMaps = drugMapRepository.findAll();
        for (DrugMap m : drugMaps) {
            if (m.getDrug().contains("tumor") || m.getDrug().contains("leukemia") || m.getDrug().contains("cancer")
            || m.getDrug().contains("lymophoma") || m.getDrug().contains("carcinoma") || m.getDrug().contains("sarcoma")
            || m.getDrug().contains("neoplasm") || m.getDrug().contains("disease") || m.getDrug().contains("melanoma")) drugMapRepository.delete(m);

        }
        for (GeneMap m : geneMapRepository.findAll()) {
            if (!m.getSymbol().equals(m.getSymbol().toLowerCase())) geneMapRepository.delete(m);
        }

    }

    @Test
    void cleanupSolr() throws IOException, SolrServerException {
        solrClientTool.delete("knowledge", "AQE3F9KBJT7V90QUNP00");
    }

    @Test
    void indexArticles() {
        List<CancerMap> cancerMaps = cancerMapRepository.findAll();
        List<MutationMap> mutationMaps = mutationMapRepository.findAll();
        List<GeneMap> geneMaps = geneMapRepository.findAll();
        List<DrugMap> drugMaps = drugMapRepository.findAll();
        cancerMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().forEach(pmid -> {
                ArticleIndex ai;
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setTopics("cancer:" + c.getCancerType());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("cancer:" + c.getCancerType()))
                        ai.setTopics(ai.getTopics() + ";" + "cancer:" + c.getCancerType());
                }
                articleIndexRepository.save(ai);
            });
            });
        drugMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("drug:" + c.getDrug());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("drug:" + c.getDrug()))
                        ai.setTopics(ai.getTopics() + ";" + "drug:" + c.getDrug());
                }
                articleIndexRepository.save(ai);
            });
            });
        mutationMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("mutation:" + c.getSymbol());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("mutation:" + c.getSymbol()))
                        ai.setTopics(ai.getTopics() + ";" + "mutation:" + c.getSymbol());
                }
                articleIndexRepository.save(ai);
            });
            });
        geneMaps.forEach(c -> {
            Set<Integer> pmids = c.getListAsSet();
            pmids.parallelStream().forEach(pmid -> {
                Optional<ArticleIndex> oai = articleIndexRepository.findById(pmid + "");
                ArticleIndex ai;
                if (oai.isEmpty()) {
                    ai = new ArticleIndex();
                    ai.setPmId(pmid + "");
                    ai.setTopics("gene:" + c.getSymbol());
                } else {
                    ai = oai.get();
                    if (!ai.getTopics().contains("gene:" + c.getSymbol()))
                        ai.setTopics(ai.getTopics() + ";" + "gene:" + c.getSymbol());
                }
                articleIndexRepository.save(ai);
            });
            });
    }

    @Test
    void runWeeklyUpdate() {
        articleService.updateArticlesPubmed();
    }

    @Test
    void MutationMapper() {
        AminoAcids.populate();
        Map<String, List<ArticleQuery.WordPair>> mut = ArticleQuery.readVariants();
        Set<ArticleQuery.WordPair> mutations = new HashSet<>();
        for (String gene: mut.keySet()) {
            System.out.print(gene+",");

/*            for (ArticleQuery.WordPair wordPair : mut.get(gene)) {
                //System.out.println(gene + " " + wordPair.word1 + " " + wordPair.word2);
                mutations.add(wordPair);
            }
*/
        }
        System.out.println();
        System.out.println("Total number: " + mutations.size());
    }


    @Test
    void AllChangeMapper() {
        Map<String, List<ArticleQuery.WordPair>> mut = ArticleQuery.readVariantsAll();
        Set<ArticleQuery.WordPair> mutations = new HashSet<>();
        for (String gene: mut.keySet()) {
            for (ArticleQuery.WordPair wordPair : mut.get(gene)) {
                System.out.println(gene + " \"" + wordPair.word1 + "\" \"" + wordPair.word2 + "\"");
                mutations.add(wordPair);
            }
        }
        System.out.println("Total number: " + mutations.size());
    }

    @Test
    void updateCitations() {
        final int pageLimit = 50000;
        int pageNumber = 169;
        Page<Article> pages;
        do {
            pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
            processArticles(pages.getContent());
        } while (pages.hasNext());
        System.out.println("Total items: " + pages.getTotalElements());
        if (processArticles.size() > 0) {
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < processArticles.size(); i++) {
                if (i == 0) items.append(processArticles.get(i).getPmId().trim());
                else items.append(",").append(processArticles.get(i).getPmId().trim());
            }
            try {
                ProcessUtil.runScript("python3 python/pubmed_list.py " + items.toString());
                parser.reload("pubmed_list.xml");
                parser.DFS(parser.getRoot(), Tree.articleTreeNoCitations(), null);
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(-1);
            }
        }
    }


    @Test
    void populateSolr() throws IOException, SolrServerException {
        List<FullText> fullTexts = fullTextRepository.findAll();
        for (FullText f : fullTexts) {
            Article a = articleRepository.findByPmId(f.getPmId());
            if (a == null) {
                System.out.println("Something went wrong");
                throw new IOException("No database item for Article");
            }
            System.out.println("Adding Item");
            UpdateResponse updateResponse = solrClientTool.add("research", SolrClientTool.randomId(), a.getTitle(), a.getAuthors(), "https://aimlcoe.mskcc.org/knowledge/getPDF/"+f.getPmId()+".pdf", null, f.getTextEntry());
            System.out.println(updateResponse.toString());
        }
    }

    @Test
    void testFindClustering() throws IOException, SolrServerException {
        solrClientTool.findClustering("text:\"KRAS\"");
    }

    @Test
    void testSolr() throws IOException, SolrServerException {
        Map<String, Integer> fieldMap = new HashMap<>();
        SolrDocumentList list = solrClientTool.deepPage("*", 1000);
        for (SolrDocument d : list) {
            for (String s : d.getFieldNames()) {
                fieldMap.merge(s, 1, Integer::sum);
                System.out.println(s + " " + d.getFieldValue(s));
                System.out.println(d.getFieldValue(s).getClass().getSimpleName());
            }
            System.out.println("ID = " + d.getFieldValue("id"));
        }
        System.out.println("LOOKING FOR CONSISTENT ITEMS:");
        for (String s: fieldMap.keySet()) {
            if (fieldMap.get(s) == list.size()) System.out.println(s);
        }
        System.out.println("QUERYING...");
        solrClientTool.find("knowledge", "KRAS");
    }

    @Test
    void countMe() {
        String genes = "ABL1,ABRAXAS1,ACAT1,ACO2,ACSF2,ACVR1,AGK,AGO2,AKAP10,AKT1,AKT2,AKT3,ALB,ALDH2,ALDOA,ALG9,ALK,ALOX12B,AMER1,AMPH,ANK3,ANKRD11,APC,APLNR,APOOL,AR,ARAF,ARHGAP35,ARHGEF11,ARHGEF17,ARID1A,ARID1B,ARID2,ARID5B,ARNTL2,ASPSCR1,ASXL1,ASXL2,ATF1,ATF6B,ATM,ATR,ATRX,ATXN7,AURKA,AURKB,AXIN1,AXIN2,AXL,B2M,BABAM1,BAD,BAI3,BAIAP2,BAIAP2L1,BAP1,BARD1,BBC3,BCL10,BCL2,BCL2L1,BCL2L11,BCL2L14,BCL6,BCOR,BIRC3,BLM,BMP2K,BMPR1A,BRAF,BRCA1,BRCA2,BRD4,BRIP1,BTBD11,BTK,CA10,CALR,CAMK2A,CARD11,CARM1,CASP8,CBFB,CBL,CCDC171,CCDC6,CCND1,CCND2,CCND3,CCNE1,CCNQ,CCNT1,CD274,CD276,CD58,CD74,CD79A,CD79B,CDC42,CDC73,CDH1,CDK12,CDK4,CDK5RAP2,CDK6,CDK8,CDKN1A,CDKN1B,CDKN2A,CDKN2B,CDKN2C,CEACAM7,CEBPA,CECR2,CENPA,CEP89,CFDP1,CHAF1A,CHEK1,CHEK2,CIC,CIT,CLEC1A,CLIP2,CLMN,CLSPN,COL15A1,COP1,CREBBP,CRKL,CRLF2,CSDE1,CSF1R,CSF2RA,CSF3R,CTCF,CTLA4,CTNNB1,CTR9,CTTNBP2,CUL1,CUL3,CXCR4,CYB5R3,CYLD,CYP19A1,CYSLTR2,DAXX,DCTN1,DCUN1D1,DDR2,DEPDC7,DERA,DGKH,DHX35,DIAPH1,DICER1,DIS3,DLG1,DNAJB1,DNAJC8,DNM2,DNMT1,DNMT3A,DNMT3B,DOCK1,DOT1L,DROSHA,DTNB,DUSP4,E2F3,E4F1,EED,EGFL7,EGFR,EIF1AX,EIF2C1,EIF4A2,EIF4E,ELF3,ELOC,EMILIN1,EML4,EP300,EPAS1,EPB41,EPCAM,EPHA3,EPHA5,EPHA7,EPHB1,ERBB2,ERBB3,ERBB4,ERCC2,ERCC3,ERCC4,ERCC5,ERF,ERG,ERRFI1,ESR1,ETV1,ETV6,EWSR1,EXOC4,EZH1,EZH2,EZHIP,EZR,FAM175A,FAM211A,FAM46C,FAM58A,FANCA,FANCC,FAT1,FBXL7,FBXW7,FER1L6,FES,FGF19,FGF3,FGF4,FGFR1,FGFR2,FGFR3,FGFR4,FH,FLCN,FLT1,FLT3,FLT4,FMN1,FOXA1,FOXF1,FOXL2,FOXO1,FOXP1,FUBP1,FYN,GAB1,GAB2,GAS6,GATA1,GATA2,GATA3,GBAS,GLI1,GNA11,GNAI2,GNAQ,GNAS,GNB1,GNL2,GPS2,GREB1L,GREM1,GRIN2A,GRIPAP1,GRM8,GSDMA,GSK3B,H1-2,H3-3B,H3-5,H3C1,H3C10,H3C13,H3C14,H3C2,H3C3,H3C4,H3C6,H3C7,H3C8,H3F3A,H3F3B,H3F3C,HGF,HIST1H1C,HIST1H2BD,HIST1H3A,HIST1H3B,HIST1H3C,HIST1H3D,HIST1H3E,HIST1H3F,HIST1H3G,HIST1H3H,HIST1H3I,HIST1H3J,HIST2H3C,HIST2H3D,HIST3H3,HLA-A,HLA-B,HLA-C,HMBOX1,HMGA2,HNF1A,HOXB13,HRAS,ICOSLG,ID3,IDH1,IDH2,IFNGR1,IFT140,IGF1,IGF1R,IGF2,IGHMBP2,IKBKE,IKZF1,IKZF3,IL10,IL7R,INHA,INHBA,INPP4A,INPP4B,INPPL1,INSR,IRF4,IRS1,IRS2,JAK1,JAK2,JAK3,JUN,KBTBD4,KCNK13,KCNMA1,KCNMB3,KDM5A,KDM5C,KDM6A,KDR,KEAP1,KIAA1244,KIAA1549,KIAA1731,KIF13A,KIF5B,KIT,KLF4,KLF5,KLHL12,KMT2A,KMT2B,KMT2C,KMT2D,KMT5A,KNSTRN,KRAS,KREMEN1,LAMA4,LATS1,LATS2,LETM2,LGR5,LMNA,LMO1,LRRC16A,LYN,LZTR1,LZTS1,MAD2L2,MALT1,MAP2K1,MAP2K2,MAP2K4,MAP3K1,MAP3K13,MAP3K14,MAPK1,MAPK3,MAPKAP1,MAX,MCL1,MDC1,MDM2,MDM4,MED12,MED20,MEF2B,MEN1,MET,MFSD11,MGA,MICU1,MITF,MKLN1,MKRN1,MLH1,MLL,MLL2,MLL3,MLL4,MLLT1,MPL,MRE11,MRE11A,MSH2,MSH3,MSH6,MSI1,MSI2,MST1,MST1R,MTAP,MTOR,MUTYH,MYC,MYCL,MYCL1,MYCN,MYD88,MYL6B,MYOD1,NAB2,NADK,NAV1,NBN,NCOA3,NCOA4,NCOR1,NEFH,NEGR1,NF1,NF2,NFE2L2,NFKBIA,NKX2,NKX2-1,NKX3,NKX3-1,NLGN3,NLRX1,NOS1AP,NOTCH1,NOTCH2,NOTCH3,NOTCH4,NPAS1,NPM1,NR4A3,NRAS,NRG1,NRG3,NSD1,NSD3,NT5C1B,NTHL1,NTN1,NTRK1,NTRK2,NTRK3,NUDC,NUF2,NUP210L,NUP93,OSGEP,OXSR1,PAK1,PAK7,PALB2,PAPSS2,PARK2,PARP1,PAWR,PAX5,PAX8,PBRM1,PDCD1,PDCD1LG2,PDGFRA,PDGFRB,PDPK1,PEMT,PGBD5,PGR,PHLDB3,PHOX2B,PIEZO1,PIK3C2G,PIK3C3,PIK3CA,PIK3CB,PIK3CD,PIK3CG,PIK3R1,PIK3R2,PIK3R3,PIM1,PLCD3,PLCG2,PLEKHA7,PLK2,PMAIP1,PMS1,PMS2,PNRC1,POLD1,POLE,POT1,PPARG,PPM1D,PPM1G,PPP2R1A,PPP4R2,PPP6C,PRCC,PRDM1,PRDM14,PREX2,PRKACA,PRKAR1A,PRKCE,PRKCI,PRKD1,PRKN,PTCH1,PTEN,PTP4A1,PTPN11,PTPRD,PTPRS,PTPRT,PXN,QARS,RAB11FIP3,RAB11FIP4,RAB35,RAB5C,RABGAP1,RAC1,RAC2,RAD21,RAD50,RAD51,RAD51B,RAD51C,RAD51D,RAD52,RAD54L,RAF1,RARA,RASA1,RB1,RBM10,RBPMS,RCBTB2,RECQL,RECQL4,REL,RELA,RERE,REST,RET,RFWD2,RHEB,RHOA,RICTOR,RIN2,RIT1,RNF43,ROS1,RPGRIP1L,RPS6KA4,RPS6KB2,RPTOR,RRAGC,RRAS,RRAS2,RRBP1,RTEL1,RUFY1,RUFY2,RUNX1,RXRA,RYBP,SBF2,SCG5,SDC4,SDHA,SDHAF2,SDHB,SDHC,SDHD,SEC16A,SEC23B,SERPINB3,SERPINB4,SESN1,SESN2,SESN3,SETD2,SETD8,SETDB1,SF3B1,SFXN2,SH2B3,SH2D1A,SHMT1,SHOC2,SHQ1,SIPA1L3,SIRT2,SLC24A3,SLC47A1,SLC5A5,SLFN11,SLIT2,SLX4,SMAD2,SMAD3,SMAD4,SMARCA2,SMARCA4,SMARCB1,SMARCD1,SMARCE1,SMO,SMYD3,SOCS1,SOS1,SOX17,SOX2,SOX9,SPECC1L,SPEN,SPOP,SPRED1,SPRTN,SQSTM1,SRC,SRSF2,ST7,STAG2,STAT3,STAT5A,STAT5B,STK11,STK19,STK40,STMN3,STRN,SUFU,SUGP1,SUZ12,SYK,SYMPK,SYNE1,TAP1,TAP2,TBX3,TCEB1,TCF3,TCF7L2,TEF,TEK,TENM2,TENT5C,TERT,TET1,TET2,TFE3,TFG,TGFBR1,TGFBR2,THSD4,TIMM8B,TLK2,TMBIM4,TMEM127,TMEM145,TMPRSS2,TNFAIP3,TNFRSF14,TNFRSF21,TOP1,TP53,TP53BP1,TP63,TPM3,TPR,TRAF2,TRAF7,TRAP1,TRAPPC9,TRIP13,TRMT1,TSC1,TSC2,TSEN2,TSHR,U2AF1,UPF1,USP6,USP8,VEGFA,VHL,VPS9D1,VTCN1,WHSC1,WHSC1L1,WIBG,WT1,WWOX,WWTR1,XIAP,XPO1,XRCC2,YAP1,YBX2,YES1,ZFHX3,ZFPM2,ZNF429,ZNF532,ZRSR2";
        System.out.println(genes.split(",").length);
    }

    @Test
    void testXML() {
        try {
            XMLParser p = new XMLParser("srep27891.nxml");
            //NodeList list = p.getTag("article-meta");

            //p.visitChildNodes(list);
            //p.visitChildNodes(p.getTag("mixed-citation"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testEntrezXML() {
        try {
            XMLParser p = new XMLParser("pubmed.xml");
            {
                final int pageLimit = 50000;
                int pageNumber = 0;
                Page<Article> pages = articleRepository.findAll(PageRequest.of(pageNumber, pageLimit));
                int total = pages.getSize();
                p.setDb(pages.getContent());
                while (pages.hasNext()) {
                    pages = articleRepository.findAll(PageRequest.of(++pageNumber, pageLimit));
                    total += pages.getSize();
                    p.setDb(pages.getContent());
                }
                System.out.println("Total items: " + pages.getTotalElements());
            }
            p.setArticleRepository(articleRepository);
            p.DFS(p.getRoot(), Tree.articleTree(), null);
            System.out.println(XMLParser.count + " items found and " + XMLParser.dupCount + " merged duplicates");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    XMLParser parser;

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
                System.out.println("Total items: " + pages.getTotalElements());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        parser.setArticleRepository(articleRepository);
    }

    @Test
    void addNewArticles() {
        List<Article> articles = articleRepository.findByPublicationDateGreaterThan(DateTime.parse("2021-01-01T00:00:00.000Z"));
        for (Article a : articles) {
            if (a.getPmcId() != null) {
                System.out.println("Trying to add " + a.getTitle());
                fullTextService.addArticle(a.getPmId());
            }
        }
    }

    @Test
    void fullGenes() throws IOException {
      Scanner scan = new Scanner(new File("cancerSubTypes.txt"));
      List<String> genes = new ArrayList<>();
      while (scan.hasNextLine()) {
          String line = scan.nextLine();
          if (line.length() > 0) genes.add(line);
      }
      int i = 0;
      for (String gene: genes) {
          if (i != 0) System.out.print(",");
          System.out.print(gene);
          i++;
      }
    }

    @Test
    void addTP53Articles() throws IOException, SolrServerException {
        Set<Integer> articleIds = geneMapRepository.findById("brca1").get().getListAsSet();
        for (Integer i : articleIds) {
            Article a = articleRepository.findByPmId(i + "");
            if (a != null && a.getPmcId() != null && a.getPublicationDate() != null && a.getPublicationDate().getYear() > 2013) {
                System.out.println("Trying to add " + a.getTitle() + " with year " + a.getPublicationDate().getYear());
                fullTextService.addArticle(a.getPmId());
            }
        }
    }

    /*
    @Test
    void updateAllGeneSearches() {
        List<String> genes = Arrays.asList("P21,P53,ABL1,AKT1,ALK,AMER1,APC,AR,ARID1A,ASXL1,ATM,ATRX,AXIN1,BAP1,BCL2,BCOR,BRAF,BRCA1,BRCA2,CARD11,CBL,CDC73,CDH1,CDKN2A,CEBPA,CIC,CREBBP,CTNNB1,DAXX,DNMT3A,EGFR,EP300,ERBB2,EZH2,FBXW7,FGFR2,FGFR3,FLT3,FOXL2,GATA3,GNA11,GNAQ,GNAS,HNF1A,HRAS,IDH1,IDH2,JAK1,JAK2,JAK3,KDM5C,KDM6A,KIT,KMT2D,KRAS,MAP2K1,MAP3K1,MED12,MEN1,MET,MLH1,MPL,MSH2,MSH6,MYD88,NF1,NF2,NFE2L2,NOTCH1,NOTCH2,NPM1,NRAS,PAX5,PBRM1,PDGFRA,PIK3CA,PIK3R1,PPP2R1A,PRDM1,PTCH1,PTEN,PTPN11,RB1,RET,RNF43,SETD2,SF3B1,SMAD2,SMAD4,SMARCA4,SMARCB1,SMO,SOCS1,SPOP,STAG2,STK11,TET2,TNFAIP3,TP53,TSC1,U2AF1,VHL,WT1,AKT2,ARID2,ATR,B2M,BARD1,BCL6,BRD4,BRIP1,BTK,CALR,CASP8,CBFB,CCND1,CCND2,CCND3,CCNE1,CD274,CD79A,CD79B,CDK12,CDK4,CDK6,CDKN1B,CDKN2C,CHEK2,CRLF2,CSF1R,CSF3R,CTCF,CXCR4,DDR2,ERBB3,ERBB4,ERG,ESR1,ETV6,FANCA,FANCC,FGFR1,FGFR4,FLCN,FUBP1,GATA1,GATA2,H3-3A,H3C2,IKZF1,IRF4,JUN,KDM5A,KDR,KEAP1,KMT2A,KMT2C,MAP2K2,MAP2K4,MAPK1,MDM2,MDM4,MITF,MTOR,MUTYH,MYC,MYCL,MYCN,NKX2-1,NSD2,NSD3,NTRK1,NTRK3,PALB2,PDCD1LG2,PDGFRB,PHF6,PIM1,PRKAR1A,RAD21,RAF1,RARA,ROS1,RUNX1,SDHA,SDHB,SDHC,SDHD,SOX2,SPEN,SRC,SRSF2,STAT3,SUFU,SYK,TENT5C,TGFBR2,TMPRSS2,TNFRSF14,TSC2,TSHR,XPO1,AKT3,ARAF,ARID1B,AURKA,AURKB,AXL,BCL10,BCORL1,BCR,BIRC3,BLM,BTG1,CDK8,CDKN2B,CHEK1,CRKL,CYLD,DOT1L,EED,EIF4A2,EPHA3,EPHB1,ERCC4,ETV1,FAS,FGF19,FGF3,FGF4,FH,FLT1,FLT4,FOXO1,FOXP1,GRIN2A,GSK3B,HGF,ID3,IGF1R,IKBKE,IL7R,INPP4B,IRS2,KLF4,LMO1,MALT1,MAP3K13,MCL1,MEF2B,MRE11,MSH3,MSI2,NBN,NCOR1,NFKBIA,NSD1,NT5C2,NTRK2,P2RY8,PDCD1,PIK3CB,PMS2,POLD1,POLE,POT1,PPARG,RAC1,RAD51,RAD51B,RBM10,REL,RHOA,RICTOR,RPTOR,SETBP1,SOX9,STAT5B,SUZ12,TBX3,TCF3,TERT,TET1,TOP1,TP63,TRAF7,ZRSR2,ACVR1,ALOX12B,AXIN2,BCL11B,BCL2L1,BMPR1A,CDKN1A,CIITA,CUL3,CUX1,DDX3X,DICER1,DIS3,DNAJB1,DNMT1,DROSHA,EPAS1,EPHA5,EPHA7,ERCC2,ERCC3,ERCC5,ERRFI1,ETV4,ETV5,EWSR1,FANCD2,FAT1,FBXO11,FOXA1,GLI1,GNA13,H1-2,H3-3B,HDAC1,HLA-A,INHBA,LATS1,LATS2,LYN,MAF,MAP3K14,MAX,MLLT1,MST1R,MYOD1,NCOR2,NOTCH3,NUP93,PARP1,PHOX2B,PIK3C2G,PIK3CG,PIK3R2,PLCG2,PPM1D,PPP6C,PREX2,PRKCI,PRKN,PTPRT,RAD50,RAD51C,RAD51D,RAD52,RAD54L,RECQL4,RUNX1T1,SDHAF2,SGK1,SH2B3,SMAD3,SMARCD1,STAT5A,STAT6,TBL1XR1,TCF7L2,TEK,TMEM127,TRAF2,VEGFA,WWTR1,XRCC2,ZFHX3,ABL2,ABRAXAS1,AGO2,ANKRD11,ARID5B,ASXL2,ATF1,BABAM1,BBC3,BCL2L11,BCL9,CARM1,CCNQ,CD276,CD58,CDC42,CENPA,COP1,CSDE1,CTLA4,CYSLTR2,DCUN1D1,DDIT3,DEK,DNMT3B,DTX1,DUSP22,DUSP4,E2F3,EGFL7,EIF1AX,EIF4E,ELF3,ELOC,EPCAM,ERF,ETNK1,EZH1,FANCG,FANCL,FEV,FLI1,FYN,GNA12,GNB1,GPS2,GREM1,H1-3,H1-4,H2AC11,H2AC16,H2AC17,H2AC6,H2BC11,H2BC12,H2BC17,H2BC4,H2BC5,H3-4,H3-5,H3C1,H3C10,H3C11,H3C12,H3C13,H3C14,H3C3,H3C4,H3C6,H3C7,H3C8,HDAC4,HDAC7,HIF1A,HLA-B,HOXB13,ICOSLG,IFNGR1,IGF1,IGF2,IKZF3,IL10,INHA,INPP4A,INPPL1,INSR,IRF1,IRF8,IRS1,JARID2,KAT6A,KMT2B,KMT5A,KNSTRN,LCK,LMO2,LZTR1,MAFB,MAPK3,MAPKAP1,MDC1,MECOM,MGA,MLLT10,MSI1,MST1,MTAP,MYB,NCOA3,NCSTN,NEGR1,NKX3-1,NOTCH4,NR4A3,NTHL1,NUF2,NUP98,NUTM1,PAK1,PAK5,PCBP1,PDGFB,PDPK1,PGR,PIK3C3,PIK3CD,PIK3R3,PLCG1,PLK2,PMAIP1,PMS1,PNRC1,PPP4R2,PRDM14,PRKD1,PTP4A1,PTPN2,PTPRD,PTPRS,RAB35,RAC2,RASA1,RBM15,RECQL,RHEB,RIT1,RPS6KA4,RPS6KB2,RRAGC,RRAS,RRAS2,RTEL1,RXRA,RYBP,SESN1,SESN2,SESN3,SETDB1,SH2D1A,SHOC2,SHQ1,SLX4,SMARCE1,SMC1A,SMC3,SMYD3,SOS1,SOX17,SPRED1,SS18,STK19,STK40,TAL1,TAP1,TAP2,TCL1A,TFE3,TGFBR1,TLX1,TLX3,TP53BP1,TRAF3,TRAF5,TYK2,U2AF2,UBR5,UPF1,USP8,VTCN1,XBP1,XIAP,YAP1,YES1,ABI1,ACTG1,ACVR1B,AFDN,AFF1,AFF4,AGO1,ALB,APLNR,ARFRP1,ARHGAP26,ARHGAP35,ARHGEF12,ARHGEF28,ARID3A,ARID3B,ARID3C,ARID4A,ARID4B,ARID5A,ARNT,ATIC,ATP6AP1,ATP6V1B2,ATXN2,ATXN7,BACH2,BCL11A,BCL2L2,BCL3,BCL7A,BTG2,CAMTA1,CARS1,CBFA2T3,CD22,CD28,CD70,CD74,CDX2,CEP43,CLTC,CLTCL1,CMTR2,CNTRL,COL1A1,CRBN,CREB1,CREB3L1,CREB3L2,CTNNA1,CTR9,CYP19A1,DDX10,DDX6,DNM2,EBF1,ECT2L,EGR1,ELF4,ELL,EML4,EMSY,EP400,EPOR,EPS15,ESCO2,ETAA1,EZHIP,EZR,FANCE,FANCF,FCGR2B,FCRL4,FGF10,FGF14,FGF23,FGF6,FHIT,FOXF1,FOXO3,FOXO4,FSTL3,FURIN,FUS,GAB1,GAB2,GAS7,GID4,GPHN,GTF2I,H1-5,H2BC8,H4C9,HERPUD1,HEY1,HIP1,HLA-C,HLF,HMGA1,HMGA2,HOXA11,HOXA13,HOXA9,HOXC11,HOXC13,HOXD11,HOXD13,HSP90AA1,HSP90AB1,IGH,IGK,IGL,IL21R,IL3,ITK,KBTBD4,KDSR,KIF5B,KLF5,KLHL6,KSR2,LASP1,LEF1,LPP,LRP1B,LTB,LYL1,MAD2L2,MGAM,MLF1,MLLT3,MLLT6,MN1,MOB3B,MPEG1,MRTFA,MSN,MUC1,MYH11,MYH9,NADK,NCOA2,NDRG1,NFE2,NFKB2,NIN,NRG1,NUMA1,NUP214,NUTM2A,PAFAH1B2,PAX3,PAX7,PAX8,PBX1,PCM1,PDE4DIP,PDK1,PDS5B,PER1,PGBD5,PICALM,PIGA,PLAG1,PML,POU2AF1,PPP2R2A,PRDM16,PRKACA,PRRX1,PSIP1,PTPN1,PTPRO,QKI,RABEP1,RAP1GDS1,RELN,REST,RHOH,RNF213,ROBO1,RPL22,RPN1,RSPO2,SAMHD1,SCG5,SDC4,SERPINB3,SERPINB4,SET,SETD1A,SETD1B,SETD3,SETD4,SETD5,SETD6,SETD7,SETDB2,SH3GL1,SLC34A2,SLFN11,SMARCA2,SMG1,SOCS3,SP140,SPRTN,SRSF3,SSX1,SSX2,SSX4,STAG1,TAF15,TAL2,TET3,TFG,TNFRSF17,TPM3,TPM4,TRA,TRB,TRD,TRG,TRIM24,TRIP11,TRIP13,USP6,VAV1,VAV2,WIF1,ZBTB16,ZMYM2,ZNF217,ZNF384,ZNF521,ZNF703,ZNRF3,ACKR3,ACSL3,ACSL6,ACTB,ACVR2A,ADGRA2,AFF3,AJUBA,APH1A,APOBEC3B,ASMTL,ASPSCR1,ATG5,ATP1A1,ATP2B3,BAX,BCL9L,BRD3,BRSK1,BTLA,BUB1B,CACNA1D,CAD,CANT1,CBLB,CBLC,CCDC6,CCN6,CCNB1IP1,CCNB3,CCT6B,CD36,CDH11,CHCHD7,CHD2,CHD4,CHIC2,CHN1,CILK1,CKS1B,CLIP1,CLP1,CNBP,CNOT3,COL2A1,CPS1,CRTC1,CRTC3,CSF1,CUL4A,CYP17A1,DAZAP1,DCTN1,DDB2,DDR1,DDX4,DDX41,DDX5,DKK1,DKK2,DKK3,DKK4,DUSP2,DUSP9,EIF3E,ELK4,ELN,ELP2,EPHB4,ERC1,ETS1,EXOSC6,EXT1,EXT2,FAF1,FAT4,FBXO31,FES,FGF12,FIP1L1,FLYWCH1,FNBP1,FRS2,GABRA6,GADD45B,GATA4,GATA6,GMPS,GOLGA5,GOPC,GPC3,GRM3,GTSE1,H3C15,H3P6,HIRA,HNRNPA2B1,HOOK3,HOXA3,HSD3B1,IKBKB,IKZF2,IL2,IL6ST,INPP5D,IRF2,IRS4,JAZF1,KAT6B,KCNJ5,KDM2B,KDM4C,KEL,KLF2,KLF3,KLF6,KLK2,KNL1,KTN1,LARP4B,LCP1,LIFR,LMNA,LRIG3,LRP5,LRP6,LRRK2,LTK,MAGED1,MAML2,MAP3K6,MAP3K7,MBD6,MDS2,MEF2C,MEF2D,MERTK,MIB1,MIDEAS,MKI67,MKNK1,MLLT11,MNX1,MTCP1,MYO18A,MYO5A,NAB2,NACA,NBEAP1,NCOA1,NCOA4,NFATC2,NFIB,NFKBIE,NOD1,NONO,NUTM2B,OLIG2,OMD,PAG1,PAK3,PARP2,PARP3,PASK,PATZ1,PC,PCLO,PCSK7,PDCD11,PHF1,PIK3C2B,POLQ,POU5F1,PPFIBP1,PPP1CB,PRCC,PRF1,PRKDC,PRSS1,PRSS8,PTK6,PTK7,PTPN13,PTPN6,PTPRB,PTPRC,PTPRK,RALGDS,RANBP2,RASGEF1A,RMI2,RNF217-AS1,RPL10,RPL5,RSPO3,RUNX2,S1PR2,SALL4,SBDS,SEC31A,SEPTIN5,SEPTIN6,SEPTIN9,SERP2,SFPQ,SFRP1,SFRP2,SFRP4,SIX1,SLC1A2,SLC45A3,SMARCA1,SNCAIP,SND1,SNX29,SOCS2,SOX10,SS18L1,STAT1,STAT2,STAT4,STIL,STRN,TAF1,TCEA1,TCF12,TCL1B,TEC,TERC,TFEB,TFPT,TFRC,TIPARP,TLE1,TLE2,TLE3,TLE4,TLL2,TMEM30A,TMSB4XP8,TNFRSF11A,TPR,TRIM27,TRIM33,TRRAP,TTL,TUSC3,TYRO3,WDCP,WDR90,WRN,XPA,XPC,YPEL5,YWHAE,YY1,YY1AP1,ZBTB20,ZBTB7A,ZFP36L1,ZMYM3,ZNF24,ZNF331,ZNF750".split(","));
        //articleService.runSearch("\"p53\"", 50000, 0);
        if (articleService.indexArticles()) {
            //final Semaphore concurrentExecutions = new Semaphore(1);
            final Semaphore concurrentDFSExecutions = new Semaphore(16);
            genes.parallelStream().forEach((g) -> {
                //concurrentExecutions.acquireUninterruptibly();
                String XMLFile = "";
                //try {
                synchronized (this) { XMLFile = articleService.runSearchPython('"' + g + '"', 400); }
                //} finally {
                //    concurrentExecutions.release();
                //}
                System.out.println(g);
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
    void testHumanCurated() {
        variantService.getHumanCuratedPDFs(variantRepository.findAllByKey("AAVariants"));
        variantService.getHumanCuratedPDFs(variantRepository.findAllByKey("hongxin1"));
    }


    @Autowired
    Epitope epidope;

    @Test
    void epitopes() {
        epidope.fire();
    }

    @Test
    void quickCurate() {
       List<Variant> variants = variantRepository.findAllByKey("hongxin1").stream().filter(v -> v.getMutation() != null && !v.getMutation().equals("")).collect(Collectors.toList());
       Collections.reverse(variants);
       analyticsService.processVariants(variants);
    }

    @Test
    public void cleanup() {
        gridFsTemplate.delete(new Query(GridFsCriteria.whereContentType().is("DL4j/Keras Model")));
        gridFsTemplate.delete(new Query(GridFsCriteria.whereContentType().is("Nd4j Array")));
    }

    @Test
    public void testRescore() {
        List<Variant> variants = variantRepository.findAll();
        for (Variant v : variants) variantService.rescoreArticles(v);
    }


    @Autowired
    SimulationMIND simulationMIND;

    @Test
    void nameFinder() {
        textService.nameFinder("Schlacher K, Christ N, Siaud N, Egashira A, Wu H, Jasin M. Double-strand\n" +
                "break repair-independent role for BRCA2 in blocking stalled replication fork\n" +
                "degradation by MRE11. Cell. 2011;145(4):529–542.\n" +
                "37. Davies OR, Pellegrini L. Interaction with the BRCA2 C terminus protects\n" +
                "RAD51-DNA filaments from disassembly by BRC repeats. Nat Struct Mol Biol.\n" +
                "2007;14(6):475–483.");
    }

    @Test
    void findReferences() {
        Page<FullText> page = fullTextRepository.findAll(PageRequest.of(0, 5000));
        List<FullText> fts = page.getContent();
        List<String> texts = fts.stream().filter(ft -> ft.getTextEntry() != null).map(FullText::getTextEntry).collect(Collectors.toList());
        List<String> pmids = fts.stream().filter(ft -> ft.getTextEntry() != null).map(FullText::getPmId).collect(Collectors.toList());
        List<String> missed = new ArrayList<>();
        int n = 0, total = 5000, q = 0, r = 0;
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null) {
                total--;
                continue;
            }
            int pos = textService.getReferencesPosition(text);
            if (pos != -1) {
                n++;
            } else missed.add(pmids.get(i));
        }
        log.info("the matches are {} out of {}", n, 5000);
        for (String pmid: missed) {
            System.out.println(pmid);
        }
    }

    @Autowired
    VariantController variantController;

    @Test
    void checkPDBController() {
        ResponseEntity<String> response = variantController.getPDB("1j5n");
        if (response.getStatusCodeValue() == HttpStatus.OK.value()) {
            System.out.println(response.getBody());
        }
    }

    @Test
    void testDL() {
        MCNetwork mc = new MCNetwork("data.csv", 9353, 123, 1021, 180);
        //MCNetwork mc = new MCNetwork("MCoutput1.out");
        //mc.setupData("data.csv", 9353);
        mc.setName("demo1");
        mc.shuffle(321, 0.5);
        mc.normalize();
        mc.runNetwork("MCoutputDemo1.out", 200);
        mc.runMC(200, true);
        mc.resetEquilibrated();
        mc.runMC(200, true);
        simulationMIND.saveSimulation(mc);
    }

    @Test
    void testDLContinue() {
        //MCNetwork mc = new MCNetwork("data.csv", 9353, 123, 1021, 180);
        MCNetwork mc = new MCNetwork("MCoutput1.out").buffered("samples-");
        mc.setupData("data.csv", 9353);
        mc.shuffle(321, 0.5);
        mc.normalize();
        //mc.runNetwork("MCoutput1.out", 50000);
        mc.runMC(50000, true);
        mc.resetEquilibrated();
        mc.runMC(50000, true);
        simulationMIND.saveSimulation(mc);
    }

    /*
    @Test
    void updateAllCancerSearches() {
        List<CancerType> cancerTypes = cancerTypeRepository.findAll();
        Collections.shuffle(cancerTypes);
        Set<String> mainTypes = new LinkedHashSet<>();
        for (CancerType c : cancerTypes) mainTypes.add(c.getMainType());
        //articleService.runSearch("\"p53\"", 50000, 0);
        if (articleService.indexArticles()) {
            //final Semaphore concurrentExecutions = new Semaphore(1);
            final Semaphore concurrentDFSExecutions = new Semaphore(16);
            mainTypes.parallelStream().forEach((g) -> {
                if (g.endsWith(", NOS")) g = g.substring(0, g.indexOf(", NOS"));
                //concurrentExecutions.acquireUninterruptibly();
                String XMLFile = "";
                //try {
                synchronized (this) { XMLFile = articleService.runSearchPython('"' + g + '"', 400); }
                //} finally {
                //    concurrentExecutions.release();
                //}
                System.out.println(g);
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
    Map<String, String> readPMIDsBRAFTest() throws IOException {
        Scanner scan = new Scanner(new FileReader(("pmids")));
        Set<String> PMIDs = new HashSet<>();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] values = line.split("[\\s,]+");
            for (String p : values) {
                PMIDs.add(p.trim());
            }
        }
        System.out.println(PMIDs);
        System.out.println(PMIDs.size());
        int count = 0;
        int count2 = 0;
        Set<String> missingPMIDs = new HashSet<>();
        Map<String, Article> pmlist = new HashMap<>();
        Map<String, String> PMID2PMCID = new HashMap<>();
        Map<String, String> PMID2DOI = new HashMap<>();
        Map<String, String> PMID2PII = new HashMap<>();

        for (String pmid : PMIDs) {
            Article a = articleRepository.findByPmId(pmid);
            if (a == null) {
                System.out.println("NO ARTICLE AT ALL FOUND FOR " + pmid);
                count2++;
                missingPMIDs.add(pmid);
                PMID2PMCID.put(pmid, "No PMCID for this collection item");
                continue;
            }
            if (a.getPmcId() != null && a.getPmcId().length() > 2) {
                System.out.println(a.getPmcId());

                PMID2PMCID.put(pmid, (a.getPmcId().startsWith("PMC") ? a.getPmcId().substring(3) : a.getPmcId()));
                count++;
            } else {
                PMID2PMCID.put(pmid, "PMC_ID_NOT_FOUND");
                System.out.println("NO PMC ID FOUND");
            }
            if (a.getDoi() != null && a.getDoi().length() > 1) {
                System.out.println(a.getDoi());

                PMID2DOI.put(pmid, a.getDoi());
                //count++;
            } else {
                PMID2DOI.put(pmid, "DOI_NOT_FOUND");
                System.out.println("NO DOI FOUND");
            }
            if (a.getPii() != null && a.getPii().length() > 1) {
                System.out.println(a.getPii());

                PMID2PII.put(pmid, a.getPii());
                //count++;
            } else {
                PMID2PII.put(pmid, "PII_NOT_FOUND");
                System.out.println("NO PII FOUND");
            }
        }
        PrintWriter writer = new PrintWriter("OncoKBArticles.csv", StandardCharsets.UTF_8);
        writer.println("PMID,PMC,doi,pii");
        for (String key : PMID2PMCID.keySet()) {
            writer.println(key + "," + PMID2PMCID.get(key) + "," + PMID2DOI.get(key) + "," + PMID2PII.get(key));
        }
        writer.close();
        System.out.println(count + " articles out of " + PMIDs.size() + " with " + count2 + " missing");
        System.out.println(missingPMIDs);
        return PMID2PMCID;
    }

    public void setupPMCId() throws IOException {
        Map<String, String> PMID2PMCID = readPMIDsBRAFTest();
        File f = new File("PMC");
        f.mkdir();
        for (String PMId : PMID2PMCID.keySet()) {
            String PMCId = PMID2PMCID.get(PMId);
            if (PMCId.equals("PMC_ID_NOT_FOUND") || PMCId.equals("No PMCID for this collection item")) continue;
            ProcessUtil.runScript("python3 python/PMC.py " + PMCId);
            f = new File("PMC/" + PMId);
            f.mkdir();
            f = new File(PMCId + ".tar.gz");
            if (f.exists()) {
                Path path = Files.move(Paths.get(PMCId + ".tar.gz"), Paths.get("PMC/" + PMId
                        + "/" + PMCId + ".tar.gz"));
                if (path == null) {
                    System.out.println("Failed to move anything");
                }
            } else System.out.println("No .tar.gz file");
            f = new File(PMCId + ".pdf");
            if (f.exists()) {
                Path path = Files.move(Paths.get(PMCId + ".pdf"), Paths.get("PMC/" + PMId
                        + "/" + PMCId + ".pdf"));
                if (path == null) {
                    System.out.println("Failed to move anything");
                }
            } else System.out.println("No .pdf file");
            f = new File("PMC" + PMCId + ".pdf");
            if (f.exists()) {
                Path path = Files.move(Paths.get("PMC" + PMCId + ".pdf"), Paths.get("PMC/" + PMId
                        + "/PMC" + PMCId + ".pdf"));
                if (path == null) {
                    System.out.println("Failed to move anything");
                }
            } else System.out.println("No PMC.pdf file for non-open-access");
        }
    }

    @Test
    void expandPMCIds() throws IOException {
        Map<String, String> PMID2PMCID = readPMIDsBRAFTest();
        for (String PMId : PMID2PMCID.keySet()) {
            String PMCId = PMID2PMCID.get(PMId);
            if (PMCId.equals("PMC_ID_NOT_FOUND") || PMCId.equals("No PMCID for this collection item")) continue;
            File f = new File("PMC/"+PMId+"/"+PMCId + ".tar.gz");
            if (f.exists()) ProcessUtil.runScript("./expand " + PMId + " " + PMCId);
        }
    }

    @Test
    void runAllGenes() throws IOException {
        String[] genes = "ABL1,ABRAXAS1,ACAT1,ACO2,ACSF2,ACVR1,AGK,AGO2,AKAP10,AKT1,AKT2,AKT3,ALB,ALDH2,ALDOA,ALG9,ALK,ALOX12B,AMER1,AMPH,ANK3,ANKRD11,APC,APLNR,APOOL,AR,ARAF,ARHGAP35,ARHGEF11,ARHGEF17,ARID1A,ARID1B,ARID2,ARID5B,ARNTL2,ASPSCR1,ASXL1,ASXL2,ATF1,ATF6B,ATM,ATR,ATRX,ATXN7,AURKA,AURKB,AXIN1,AXIN2,AXL,B2M,BABAM1,BAD,BAI3,BAIAP2,BAIAP2L1,BAP1,BARD1,BBC3,BCL10,BCL2,BCL2L1,BCL2L11,BCL2L14,BCL6,BCOR,BIRC3,BLM,BMP2K,BMPR1A,BRAF,BRCA1,BRCA2,BRD4,BRIP1,BTBD11,BTK,CA10,CALR,CAMK2A,CARD11,CARM1,CASP8,CBFB,CBL,CCDC171,CCDC6,CCND1,CCND2,CCND3,CCNE1,CCNQ,CCNT1,CD274,CD276,CD58,CD74,CD79A,CD79B,CDC42,CDC73,CDH1,CDK12,CDK4,CDK5RAP2,CDK6,CDK8,CDKN1A,CDKN1B,CDKN2A,CDKN2B,CDKN2C,CEACAM7,CEBPA,CECR2,CENPA,CEP89,CFDP1,CHAF1A,CHEK1,CHEK2,CIC,CIT,CLEC1A,CLIP2,CLMN,CLSPN,COL15A1,COP1,CREBBP,CRKL,CRLF2,CSDE1,CSF1R,CSF2RA,CSF3R,CTCF,CTLA4,CTNNB1,CTR9,CTTNBP2,CUL1,CUL3,CXCR4,CYB5R3,CYLD,CYP19A1,CYSLTR2,DAXX,DCTN1,DCUN1D1,DDR2,DEPDC7,DERA,DGKH,DHX35,DIAPH1,DICER1,DIS3,DLG1,DNAJB1,DNAJC8,DNM2,DNMT1,DNMT3A,DNMT3B,DOCK1,DOT1L,DROSHA,DTNB,DUSP4,E2F3,E4F1,EED,EGFL7,EGFR,EIF1AX,EIF2C1,EIF4A2,EIF4E,ELF3,ELOC,EMILIN1,EML4,EP300,EPAS1,EPB41,EPCAM,EPHA3,EPHA5,EPHA7,EPHB1,ERBB2,ERBB3,ERBB4,ERCC2,ERCC3,ERCC4,ERCC5,ERF,ERG,ERRFI1,ESR1,ETV1,ETV6,EWSR1,EXOC4,EZH1,EZH2,EZHIP,EZR,FAM175A,FAM211A,FAM46C,FAM58A,FANCA,FANCC,FAT1,FBXL7,FBXW7,FER1L6,FES,FGF19,FGF3,FGF4,FGFR1,FGFR2,FGFR3,FGFR4,FH,FLCN,FLT1,FLT3,FLT4,FMN1,FOXA1,FOXF1,FOXL2,FOXO1,FOXP1,FUBP1,FYN,GAB1,GAB2,GAS6,GATA1,GATA2,GATA3,GBAS,GLI1,GNA11,GNAI2,GNAQ,GNAS,GNB1,GNL2,GPS2,GREB1L,GREM1,GRIN2A,GRIPAP1,GRM8,GSDMA,GSK3B,H1-2,H3-3B,H3-5,H3C1,H3C10,H3C13,H3C14,H3C2,H3C3,H3C4,H3C6,H3C7,H3C8,H3F3A,H3F3B,H3F3C,HGF,HIST1H1C,HIST1H2BD,HIST1H3A,HIST1H3B,HIST1H3C,HIST1H3D,HIST1H3E,HIST1H3F,HIST1H3G,HIST1H3H,HIST1H3I,HIST1H3J,HIST2H3C,HIST2H3D,HIST3H3,HLA-A,HLA-B,HLA-C,HMBOX1,HMGA2,HNF1A,HOXB13,HRAS,ICOSLG,ID3,IDH1,IDH2,IFNGR1,IFT140,IGF1,IGF1R,IGF2,IGHMBP2,IKBKE,IKZF1,IKZF3,IL10,IL7R,INHA,INHBA,INPP4A,INPP4B,INPPL1,INSR,IRF4,IRS1,IRS2,JAK1,JAK2,JAK3,JUN,KBTBD4,KCNK13,KCNMA1,KCNMB3,KDM5A,KDM5C,KDM6A,KDR,KEAP1,KIAA1244,KIAA1549,KIAA1731,KIF13A,KIF5B,KIT,KLF4,KLF5,KLHL12,KMT2A,KMT2B,KMT2C,KMT2D,KMT5A,KNSTRN,KRAS,KREMEN1,LAMA4,LATS1,LATS2,LETM2,LGR5,LMNA,LMO1,LRRC16A,LYN,LZTR1,LZTS1,MAD2L2,MALT1,MAP2K1,MAP2K2,MAP2K4,MAP3K1,MAP3K13,MAP3K14,MAPK1,MAPK3,MAPKAP1,MAX,MCL1,MDC1,MDM2,MDM4,MED12,MED20,MEF2B,MEN1,MET,MFSD11,MGA,MICU1,MITF,MKLN1,MKRN1,MLH1,MLL,MLL2,MLL3,MLL4,MLLT1,MPL,MRE11,MRE11A,MSH2,MSH3,MSH6,MSI1,MSI2,MST1,MST1R,MTAP,MTOR,MUTYH,MYC,MYCL,MYCL1,MYCN,MYD88,MYL6B,MYOD1,NAB2,NADK,NAV1,NBN,NCOA3,NCOA4,NCOR1,NEFH,NEGR1,NF1,NF2,NFE2L2,NFKBIA,NKX2,NKX2-1,NKX3,NKX3-1,NLGN3,NLRX1,NOS1AP,NOTCH1,NOTCH2,NOTCH3,NOTCH4,NPAS1,NPM1,NR4A3,NRAS,NRG1,NRG3,NSD1,NSD3,NT5C1B,NTHL1,NTN1,NTRK1,NTRK2,NTRK3,NUDC,NUF2,NUP210L,NUP93,OSGEP,OXSR1,PAK1,PAK7,PALB2,PAPSS2,PARK2,PARP1,PAWR,PAX5,PAX8,PBRM1,PDCD1,PDCD1LG2,PDGFRA,PDGFRB,PDPK1,PEMT,PGBD5,PGR,PHLDB3,PHOX2B,PIEZO1,PIK3C2G,PIK3C3,PIK3CA,PIK3CB,PIK3CD,PIK3CG,PIK3R1,PIK3R2,PIK3R3,PIM1,PLCD3,PLCG2,PLEKHA7,PLK2,PMAIP1,PMS1,PMS2,PNRC1,POLD1,POLE,POT1,PPARG,PPM1D,PPM1G,PPP2R1A,PPP4R2,PPP6C,PRCC,PRDM1,PRDM14,PREX2,PRKACA,PRKAR1A,PRKCE,PRKCI,PRKD1,PRKN,PTCH1,PTEN,PTP4A1,PTPN11,PTPRD,PTPRS,PTPRT,PXN,QARS,RAB11FIP3,RAB11FIP4,RAB35,RAB5C,RABGAP1,RAC1,RAC2,RAD21,RAD50,RAD51,RAD51B,RAD51C,RAD51D,RAD52,RAD54L,RAF1,RARA,RASA1,RB1,RBM10,RBPMS,RCBTB2,RECQL,RECQL4,REL,RELA,RERE,REST,RET,RFWD2,RHEB,RHOA,RICTOR,RIN2,RIT1,RNF43,ROS1,RPGRIP1L,RPS6KA4,RPS6KB2,RPTOR,RRAGC,RRAS,RRAS2,RRBP1,RTEL1,RUFY1,RUFY2,RUNX1,RXRA,RYBP,SBF2,SCG5,SDC4,SDHA,SDHAF2,SDHB,SDHC,SDHD,SEC16A,SEC23B,SERPINB3,SERPINB4,SESN1,SESN2,SESN3,SETD2,SETD8,SETDB1,SF3B1,SFXN2,SH2B3,SH2D1A,SHMT1,SHOC2,SHQ1,SIPA1L3,SIRT2,SLC24A3,SLC47A1,SLC5A5,SLFN11,SLIT2,SLX4,SMAD2,SMAD3,SMAD4,SMARCA2,SMARCA4,SMARCB1,SMARCD1,SMARCE1,SMO,SMYD3,SOCS1,SOS1,SOX17,SOX2,SOX9,SPECC1L,SPEN,SPOP,SPRED1,SPRTN,SQSTM1,SRC,SRSF2,ST7,STAG2,STAT3,STAT5A,STAT5B,STK11,STK19,STK40,STMN3,STRN,SUFU,SUGP1,SUZ12,SYK,SYMPK,SYNE1,TAP1,TAP2,TBX3,TCEB1,TCF3,TCF7L2,TEF,TEK,TENM2,TENT5C,TERT,TET1,TET2,TFE3,TFG,TGFBR1,TGFBR2,THSD4,TIMM8B,TLK2,TMBIM4,TMEM127,TMEM145,TMPRSS2,TNFAIP3,TNFRSF14,TNFRSF21,TOP1,TP53,TP53BP1,TP63,TPM3,TPR,TRAF2,TRAF7,TRAP1,TRAPPC9,TRIP13,TRMT1,TSC1,TSC2,TSEN2,TSHR,U2AF1,UPF1,USP6,USP8,VEGFA,VHL,VPS9D1,VTCN1,WHSC1,WHSC1L1,WIBG,WT1,WWOX,WWTR1,XIAP,XPO1,XRCC2,YAP1,YBX2,YES1,ZFHX3,ZFPM2,ZNF429,ZNF532,ZRSR2".split(",");
        List<String> seen = Arrays.asList("BRCA1", "TERT", "KRAS", "HER2", "TP53", "EGFR", "MAP2K1");
        for (String gene: genes) {
            if (seen.contains(gene)) continue;
            ProcessUtil.runScript("python3 python/pubmed.py " + gene);
            testEntrezXML();
        }
    }

    private List<Article> processArticles = new ArrayList<>();
    static int runCount = 0;

    @Test
    void processArticles(List<Article> articles) {
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
                        System.out.println("Error occurred: " + e.toString());
                        //System.exit(-1);
                    }
                    processArticles = new ArrayList<>();
                }
            }
        }
    }

    @Test
    void fixCancers() {
        for (String cancer: ArticleQuery.readCancerNames()) {
            DrugMap dm = drugMapRepository.findByDrug(cancer);
            // the lowercase version is the correct one!
            CancerMap cm = cancerMapRepository.findByCancerType(cancer.toLowerCase());
            if (cm == null) {
                cm = new CancerMap(cancer.toLowerCase(), new Integer[0], null);
            }
            Set<Integer> cancerItems = new HashSet<>(Arrays.asList(cm.getPmIds()));
            if (dm != null) cancerItems.addAll(Arrays.asList(dm.getPmIds()));
            dm = drugMapRepository.findByDrug(cancer.toLowerCase());
            if (dm != null) cancerItems.addAll(Arrays.asList(dm.getPmIds()));
            CancerMap cm2 = cancerMapRepository.findByCancerType(cancer);
            if (cm2 != null) cancerItems.addAll(Arrays.asList(cm2.getPmIds()));
            cm.setListAsSet(cancerItems);
            cancerMapRepository.save(cm);
        }
    }

    @Test
    void fixGenes() {
        ArticleQuery q = new ArticleQuery();
        for (String gene: q.getGenes()) {
            GeneMap gm = geneMapRepository.findBySymbol(gene.toLowerCase());
            if (gm == null) {
                gm = new GeneMap(gene.toLowerCase(), new Integer[0]);
            }
            Set<Integer> geneItems = new HashSet<>(Arrays.asList(gm.getPmIds()));
            GeneMap gm2 = geneMapRepository.findBySymbol(gene);
            if (gm2 != null) continue;
            gm.setListAsSet(geneItems);
            geneMapRepository.save(gm);
            //if (!gene.toLowerCase().equals(gene) && gm2 != null) geneMapRepository.delete(gm2);
        }
    }

    @Test
    void deleteGenes() {
        ArticleQuery q = new ArticleQuery();
        for (String gene: q.getGenes()) {
            GeneMap gm2 = geneMapRepository.findBySymbol(gene);
            //geneMapRepository.save(gm);
            if (!gene.toLowerCase().equals(gene)) geneMapRepository.delete(gm2);
        }
    }


    @Test
    void runSearch(String term, int count, int articleCount) {
        try {
            System.out.println("RUNNING PYTHON");
            ProcessUtil.runScript("python3 python/pubmed.py " + articleCount + " " + term.trim());
            System.out.println("DONE RUNNING PYTHON");
            if (count == 0) {
                parser = new XMLParser("pubmed.xml");
                parser.setArticleRepository(articleRepository);
                final int pageLimit = 500000;
                int pageNumber = 0;
                Page<Article> pages;
                System.out.println("READING ALL ARTICLES");
                do {
                    pages = articleRepository.findAll(PageRequest.of(pageNumber++, pageLimit));
                    parser.setDb(pages.getContent());
                    System.out.println(pageNumber);
                } while (pages.hasNext());
                System.out.println(pages.getTotalElements() + " elements read in");
                System.out.println("FIRST DFS");
                parser.DFS(parser.getRoot(), Tree.articleTree(), null);
                System.out.println("DONE DFS");
            } else {
                parser.reload("pubmed.xml");
                parser.DFS(parser.getRoot(), Tree.articleTree(), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void runSearchJSON(String term, int count, int articleCount) {
        try {
            System.out.println("RUNNING PYTHON");
            ProcessUtil.runScript("python3 python/pubmed.py " + articleCount + " " + term.trim());
            System.out.println("DONE RUNNING PYTHON");
            if (count == 0) {
                parser = new XMLParser("pubmed.xml");
                parser.setArticleRepository(articleRepository);

                Scanner scan = new Scanner(new FileReader("Articles.json"));
                System.out.println("READING ALL ARTICLES");
                int total = 0;
                do {
                    List<Article> articles = new ArrayList<>();
                    for (int i = 0; i < 100000 && scan.hasNextLine(); i++) {
                        articles.add(Article.fromJSON(scan.nextLine()));
                        total++;
                    }
                    parser.setDb(articles);
                    System.out.println(total + " and still going");
                } while (scan.hasNextLine());
                System.out.println(total + " articles read in");
                System.out.println("FIRST DFS");
                parser.DFS(parser.getRoot(), Tree.articleTree(), null);
                System.out.println("DONE DFS");
            } else {
                parser.reload("pubmed.xml");
                parser.DFS(parser.getRoot(), Tree.articleTree(), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void populateOncoBase() {
        int count = 0;
        for (int i = 0; i < TumorTypes.basicTypes.length; i++) {
            String item = TumorTypes.basicTypes[i];
            String searchTerm = "";
            String searchTerm2 = null;
            if (item.endsWith(", NOS")) item = item.substring(0, item.length()-5);
            String[] words = item.split(" ");
            boolean nextWord = false;
            for (String word: words) {
                if (word.contains("/")) {
                    String[] w = word.split("/");
                    searchTerm2 = searchTerm;
                    searchTerm += (searchTerm.length() == 0 ? w[0]: " "+w[0]);
                    searchTerm2 += (searchTerm2.length() == 0 ? w[1]: " "+w[1]);
                    if (w[1].equals("Fallopian") || w[1].equals("Urinary")) nextWord = true;
                } else {
                    if (!nextWord) {
                        searchTerm += (searchTerm.length() == 0 ? word : " " + word);
                        if (searchTerm2 != null) searchTerm2 += (searchTerm2.length() == 0 ? word: " "+word);
                    } else {
                        searchTerm2 += (searchTerm2.length() == 0 ? word : " " + word);
                        nextWord = false;
                    }
                }
            }
            System.out.println("Searching: " + searchTerm);
            runSearch(searchTerm, count++, 25000);
            if (searchTerm2 != null) {
                System.out.println("Searching: " + searchTerm2);
                runSearch(searchTerm2, count++, 25000);
            }
        }
    }

    @Test
    void populateOncoBaseJSON() {
        int count = 0;
        for (int i = 71; i < TumorTypes.basicTypes.length; i++) {
            String item = TumorTypes.basicTypes[i];
            String searchTerm = "";
            String searchTerm2 = null;
            if (item.endsWith(", NOS")) item = item.substring(0, item.length()-5);
            String[] words = item.split(" ");
            boolean nextWord = false;
            for (String word: words) {
                if (word.contains("/")) {
                    String[] w = word.split("/");
                    searchTerm2 = searchTerm;
                    searchTerm += (searchTerm.length() == 0 ? w[0]: " "+w[0]);
                    searchTerm2 += (searchTerm2.length() == 0 ? w[1]: " "+w[1]);
                    if (w[1].equals("Fallopian") || w[1].equals("Urinary")) nextWord = true;
                } else {
                    if (!nextWord) {
                        searchTerm += (searchTerm.length() == 0 ? word : " " + word);
                        if (searchTerm2 != null) searchTerm2 += (searchTerm2.length() == 0 ? word: " "+word);
                    } else {
                        searchTerm2 += (searchTerm2.length() == 0 ? word : " " + word);
                        nextWord = false;
                    }
                }
            }
            System.out.println("Searching: " + searchTerm);
            runSearchJSON(searchTerm, count++, 25000);
            if (searchTerm2 != null) {
                System.out.println("Searching: " + searchTerm2);
                runSearchJSON(searchTerm2, count++, 25000);
            }
        }
    }

    @Test
    void populateGenes() {
        int count = 0;
        String[] genes = "ABL1,ABRAXAS1,ACAT1,ACO2,ACSF2,ACVR1,AGK,AGO2,AKAP10,AKT1,AKT2,AKT3,ALB,ALDH2,ALDOA,ALG9,ALK,ALOX12B,AMER1,AMPH,ANK3,ANKRD11,APC,APLNR,APOOL,AR,ARAF,ARHGAP35,ARHGEF11,ARHGEF17,ARID1A,ARID1B,ARID2,ARID5B,ARNTL2,ASPSCR1,ASXL1,ASXL2,ATF1,ATF6B,ATM,ATR,ATRX,ATXN7,AURKA,AURKB,AXIN1,AXIN2,AXL,B2M,BABAM1,BAD,BAI3,BAIAP2,BAIAP2L1,BAP1,BARD1,BBC3,BCL10,BCL2,BCL2L1,BCL2L11,BCL2L14,BCL6,BCOR,BIRC3,BLM,BMP2K,BMPR1A,BRAF,BRCA1,BRCA2,BRD4,BRIP1,BTBD11,BTK,CA10,CALR,CAMK2A,CARD11,CARM1,CASP8,CBFB,CBL,CCDC171,CCDC6,CCND1,CCND2,CCND3,CCNE1,CCNQ,CCNT1,CD274,CD276,CD58,CD74,CD79A,CD79B,CDC42,CDC73,CDH1,CDK12,CDK4,CDK5RAP2,CDK6,CDK8,CDKN1A,CDKN1B,CDKN2A,CDKN2B,CDKN2C,CEACAM7,CEBPA,CECR2,CENPA,CEP89,CFDP1,CHAF1A,CHEK1,CHEK2,CIC,CIT,CLEC1A,CLIP2,CLMN,CLSPN,COL15A1,COP1,CREBBP,CRKL,CRLF2,CSDE1,CSF1R,CSF2RA,CSF3R,CTCF,CTLA4,CTNNB1,CTR9,CTTNBP2,CUL1,CUL3,CXCR4,CYB5R3,CYLD,CYP19A1,CYSLTR2,DAXX,DCTN1,DCUN1D1,DDR2,DEPDC7,DERA,DGKH,DHX35,DIAPH1,DICER1,DIS3,DLG1,DNAJB1,DNAJC8,DNM2,DNMT1,DNMT3A,DNMT3B,DOCK1,DOT1L,DROSHA,DTNB,DUSP4,E2F3,E4F1,EED,EGFL7,EGFR,EIF1AX,EIF2C1,EIF4A2,EIF4E,ELF3,ELOC,EMILIN1,EML4,EP300,EPAS1,EPB41,EPCAM,EPHA3,EPHA5,EPHA7,EPHB1,ERBB2,ERBB3,ERBB4,ERCC2,ERCC3,ERCC4,ERCC5,ERF,ERG,ERRFI1,ESR1,ETV1,ETV6,EWSR1,EXOC4,EZH1,EZH2,EZHIP,EZR,FAM175A,FAM211A,FAM46C,FAM58A,FANCA,FANCC,FAT1,FBXL7,FBXW7,FER1L6,FES,FGF19,FGF3,FGF4,FGFR1,FGFR2,FGFR3,FGFR4,FH,FLCN,FLT1,FLT3,FLT4,FMN1,FOXA1,FOXF1,FOXL2,FOXO1,FOXP1,FUBP1,FYN,GAB1,GAB2,GAS6,GATA1,GATA2,GATA3,GBAS,GLI1,GNA11,GNAI2,GNAQ,GNAS,GNB1,GNL2,GPS2,GREB1L,GREM1,GRIN2A,GRIPAP1,GRM8,GSDMA,GSK3B,H1-2,H3-3B,H3-5,H3C1,H3C10,H3C13,H3C14,H3C2,H3C3,H3C4,H3C6,H3C7,H3C8,H3F3A,H3F3B,H3F3C,HGF,HIST1H1C,HIST1H2BD,HIST1H3A,HIST1H3B,HIST1H3C,HIST1H3D,HIST1H3E,HIST1H3F,HIST1H3G,HIST1H3H,HIST1H3I,HIST1H3J,HIST2H3C,HIST2H3D,HIST3H3,HLA-A,HLA-B,HLA-C,HMBOX1,HMGA2,HNF1A,HOXB13,HRAS,ICOSLG,ID3,IDH1,IDH2,IFNGR1,IFT140,IGF1,IGF1R,IGF2,IGHMBP2,IKBKE,IKZF1,IKZF3,IL10,IL7R,INHA,INHBA,INPP4A,INPP4B,INPPL1,INSR,IRF4,IRS1,IRS2,JAK1,JAK2,JAK3,JUN,KBTBD4,KCNK13,KCNMA1,KCNMB3,KDM5A,KDM5C,KDM6A,KDR,KEAP1,KIAA1244,KIAA1549,KIAA1731,KIF13A,KIF5B,KIT,KLF4,KLF5,KLHL12,KMT2A,KMT2B,KMT2C,KMT2D,KMT5A,KNSTRN,KRAS,KREMEN1,LAMA4,LATS1,LATS2,LETM2,LGR5,LMNA,LMO1,LRRC16A,LYN,LZTR1,LZTS1,MAD2L2,MALT1,MAP2K1,MAP2K2,MAP2K4,MAP3K1,MAP3K13,MAP3K14,MAPK1,MAPK3,MAPKAP1,MAX,MCL1,MDC1,MDM2,MDM4,MED12,MED20,MEF2B,MEN1,MET,MFSD11,MGA,MICU1,MITF,MKLN1,MKRN1,MLH1,MLL,MLL2,MLL3,MLL4,MLLT1,MPL,MRE11,MRE11A,MSH2,MSH3,MSH6,MSI1,MSI2,MST1,MST1R,MTAP,MTOR,MUTYH,MYC,MYCL,MYCL1,MYCN,MYD88,MYL6B,MYOD1,NAB2,NADK,NAV1,NBN,NCOA3,NCOA4,NCOR1,NEFH,NEGR1,NF1,NF2,NFE2L2,NFKBIA,NKX2,NKX2-1,NKX3,NKX3-1,NLGN3,NLRX1,NOS1AP,NOTCH1,NOTCH2,NOTCH3,NOTCH4,NPAS1,NPM1,NR4A3,NRAS,NRG1,NRG3,NSD1,NSD3,NT5C1B,NTHL1,NTN1,NTRK1,NTRK2,NTRK3,NUDC,NUF2,NUP210L,NUP93,OSGEP,OXSR1,PAK1,PAK7,PALB2,PAPSS2,PARK2,PARP1,PAWR,PAX5,PAX8,PBRM1,PDCD1,PDCD1LG2,PDGFRA,PDGFRB,PDPK1,PEMT,PGBD5,PGR,PHLDB3,PHOX2B,PIEZO1,PIK3C2G,PIK3C3,PIK3CA,PIK3CB,PIK3CD,PIK3CG,PIK3R1,PIK3R2,PIK3R3,PIM1,PLCD3,PLCG2,PLEKHA7,PLK2,PMAIP1,PMS1,PMS2,PNRC1,POLD1,POLE,POT1,PPARG,PPM1D,PPM1G,PPP2R1A,PPP4R2,PPP6C,PRCC,PRDM1,PRDM14,PREX2,PRKACA,PRKAR1A,PRKCE,PRKCI,PRKD1,PRKN,PTCH1,PTEN,PTP4A1,PTPN11,PTPRD,PTPRS,PTPRT,PXN,QARS,RAB11FIP3,RAB11FIP4,RAB35,RAB5C,RABGAP1,RAC1,RAC2,RAD21,RAD50,RAD51,RAD51B,RAD51C,RAD51D,RAD52,RAD54L,RAF1,RARA,RASA1,RB1,RBM10,RBPMS,RCBTB2,RECQL,RECQL4,REL,RELA,RERE,REST,RET,RFWD2,RHEB,RHOA,RICTOR,RIN2,RIT1,RNF43,ROS1,RPGRIP1L,RPS6KA4,RPS6KB2,RPTOR,RRAGC,RRAS,RRAS2,RRBP1,RTEL1,RUFY1,RUFY2,RUNX1,RXRA,RYBP,SBF2,SCG5,SDC4,SDHA,SDHAF2,SDHB,SDHC,SDHD,SEC16A,SEC23B,SERPINB3,SERPINB4,SESN1,SESN2,SESN3,SETD2,SETD8,SETDB1,SF3B1,SFXN2,SH2B3,SH2D1A,SHMT1,SHOC2,SHQ1,SIPA1L3,SIRT2,SLC24A3,SLC47A1,SLC5A5,SLFN11,SLIT2,SLX4,SMAD2,SMAD3,SMAD4,SMARCA2,SMARCA4,SMARCB1,SMARCD1,SMARCE1,SMO,SMYD3,SOCS1,SOS1,SOX17,SOX2,SOX9,SPECC1L,SPEN,SPOP,SPRED1,SPRTN,SQSTM1,SRC,SRSF2,ST7,STAG2,STAT3,STAT5A,STAT5B,STK11,STK19,STK40,STMN3,STRN,SUFU,SUGP1,SUZ12,SYK,SYMPK,SYNE1,TAP1,TAP2,TBX3,TCEB1,TCF3,TCF7L2,TEF,TEK,TENM2,TENT5C,TERT,TET1,TET2,TFE3,TFG,TGFBR1,TGFBR2,THSD4,TIMM8B,TLK2,TMBIM4,TMEM127,TMEM145,TMPRSS2,TNFAIP3,TNFRSF14,TNFRSF21,TOP1,TP53,TP53BP1,TP63,TPM3,TPR,TRAF2,TRAF7,TRAP1,TRAPPC9,TRIP13,TRMT1,TSC1,TSC2,TSEN2,TSHR,U2AF1,UPF1,USP6,USP8,VEGFA,VHL,VPS9D1,VTCN1,WHSC1,WHSC1L1,WIBG,WT1,WWOX,WWTR1,XIAP,XPO1,XRCC2,YAP1,YBX2,YES1,ZFHX3,ZFPM2,ZNF429,ZNF532,ZRSR2".split(",");
        for (int i = 202; i < genes.length; i++) {
            String searchTerm = genes[i];
            System.out.println(genes[i]);
            runSearch(searchTerm, count++, 50000);
        }
    }

    @Test
    void carrot2Test() {
        final int pageLimit = 10000;
        Page<Article> pages;
        pages = articleRepository.findAllFullText(PageRequest.of(0, pageLimit));
        Carrot2Util.carrot2ClusterArticles(pages.getContent());
    }



    // find quoted genes
    public Map<String, Set<Integer>> genePMID = new HashMap<>();
    public Map<String, Target> geneMap = new HashMap<>();

    @Test
    void buildGeneMap() throws IOException {
        ArticleQuery q = new ArticleQuery();
        List<Target> allTargets = targetRepository.findAll();
        for (String s : q.getMutations().keySet()) {
            if (s.equals("Other Biomarkers")) continue;
            System.out.println(s);
            Iterable<Target> t = targetRepository.findAllBySymbol(s);
            if (!t.iterator().hasNext()) {
                for (Target targ: allTargets) {
                    if (targ.getSynonyms().length() > 0 && (targ.getSynonyms().split(";")[0].equals(s) || (targ.getAntibodypediaURL() != null && targ.getAntibodypediaURL().substring(targ.getAntibodypediaURL().lastIndexOf("/")+1).equals(s)))) {
                        geneMap.put(s, targ);
                        break;
                    }
                }
                if (geneMap.get(s) == null) throw new IOException("FAILED BECAUSE ITEM NOT FOUND " + s);
            }
            else geneMap.put(s, t.iterator().next());
        }
    }

    String currentContent = "help";
    Pattern pattern;

    @Test
    public int containsRCount(String input, String other) {
        int count = 0;
        if (!other.equals(currentContent)) {
            currentContent = other;
            pattern = Pattern.compile("([\\W\\s^_]|^)(" + other.toLowerCase() + ")([\\W\\s^_]|$)");
            while (pattern.matcher(input).find()) {
                count++;
            }
            return count;
        }
        while (pattern.matcher(input).find()) {
            count++;
        }
        return count;
    }


    @Test
    public boolean containsR(String input, String other) {
        return pattern.matcher(input).find();
    }

    @Test
    void testPopulateGenes(int page) throws IOException {
        final int pageLimit = 500000;
        Page<Article> pages;
        pages = articleRepository.findAll(PageRequest.of(page, pageLimit));
        ArticleQuery q = new ArticleQuery();
        String current = null;
        for (Article a : pages) {
            String text = a.getTitle() + " " + a.getPubAbstract() + " " + a.getKeywords() + " " + a.getMeshTerms();
            text = text.toLowerCase();
            for (String s : q.getMutations().keySet()) {
                if (s.equals("Other Biomarkers")) continue;
                current = s;
                Target t = geneMap.get(s);
                boolean found = false;
                if (containsR(text, s))
                    found = true;
                else if (t.getSynonyms() != null && !t.getSynonyms().contains(";") && t.getSynonyms().length() > 0 && containsR(text, t.getSynonyms()))
                    found = true;
                else if (t.getSynonyms() != null && t.getSynonyms().contains(";"))
                    for (String syn: t.getSynonyms().split(";")) {
                        if (containsR(text, syn)) {
                            found = true;
                            break;
                        }
                    }
                if (found) {
                    if (genePMID.get(s) == null) {
                        HashSet<Integer> hs = new HashSet<>();
                        hs.add(Integer.parseInt(a.getPmId()));
                        genePMID.put(s, hs);
                    } else {
                        genePMID.get(s).add(Integer.parseInt(a.getPmId()));
                    }
                }
            }
;
        }
        for (String key : genePMID.keySet()) {
            System.out.println(key + ": " + (genePMID.get(key) != null ? genePMID.get(key).size() : "0"));
        }
    }

    @Test void writeRecords() {
        for (String s: genePMID.keySet()) {
            GeneMap result = geneMapRepository.save(GeneMap.builder().symbol(s).pmIds(genePMID.get(s).parallelStream().toArray(Integer[]::new)).build());
        }
    }

    @Test
    void annotateGenes() throws IOException {
        buildGeneMap();
        testPopulateGenes(0);
        writeRecords();
    }

    @Test Map<String, Set<Integer>> translateGenesOverlap() {
        List<GeneMap> geneMaps = geneMapRepository.findAll();
        genePMID = geneMaps.parallelStream().collect(Collectors.toMap(GeneMap::getSymbol, GeneMap::getListAsSet));
        return genePMID;
    }

    @Test
    void patternMatch() {
        String expression = "#$_KRAS_world";
        String expression2 = " KRASS";
        String expression3 = " KRAS";
        String expression4 = "KRAS$";
        String expression5 = "KKRAS";
        String expression6 = "Saying KRAS-world";

        Pattern p = Pattern.compile("([\\W\\s^_]|^)(kras)([\\W\\s^_]|$)");
//        p.matcher("").find()
        System.out.println(p.matcher(expression.toLowerCase()).find());
        System.out.println(p.matcher(expression2.toLowerCase()).find());
        System.out.println(p.matcher(expression3.toLowerCase()).find());
        System.out.println(p.matcher(expression4.toLowerCase()).find());
        System.out.println(p.matcher(expression5.toLowerCase()).find());
//        p = Pattern.compile("([\\W\\s_]|^)(kras)([\\W\\s_]|$)");
        System.out.println(p.matcher(expression6.toLowerCase()).find());

    }

    @Autowired
    ArticleController articleController;
    @Autowired
    TextService textService;

    @Test
    public void testSentences() {
        String PMID = "28947956";
        String mutation = "D594V";
        Map<String, Integer> sentences = textService.getSentences(PMID, mutation);
        System.out.println(sentences.toString());
        int snippetLength = 0;
        int i = 1;
        int lastIsland = 0;
        for (String s : sentences.keySet()) {
            String s2 = s.replaceAll("\n", " ").replaceAll("-\n", "").replaceAll("- \n", "-").replaceAll("  ", " ");
            if (sentences.get(s) != lastIsland) {
                System.out.println("\n\n");
                snippetLength = 0;
                lastIsland = sentences.get(s);
            }
            System.out.println(i + ".  " + s2);
            snippetLength++;
            i++;
        }
    }

    @Test
    public void testParagraphs() {
        String PMID = "28947956";

        String mutation = "D594V";
        List<String> paragraphs = textService.getParagraphs(PMID, mutation);
        int i = 1;
        log.info(textService.getPageNumbers().toString());
        for (String p : paragraphs) {
            System.out.println(i + ":  " + p.replaceAll("\n", " ").replaceAll("-\n", "").replaceAll("- \n", "").replaceAll("  ", " ") + "\n\n");
            i++;
        }
        System.out.println("\n\n\n" + textService.getPageNumbers());
    }

    @Test
    public void testImport() {
        fullTextService.addHTMLTextAll();
    }

}
