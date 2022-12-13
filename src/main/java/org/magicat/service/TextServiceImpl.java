package org.magicat.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import opennlp.tools.namefind.*;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.magicat.model.*;
import org.magicat.repository.ArticleRepository;
import org.magicat.repository.FullTextRepository;
import org.magicat.repository.GeneMapRepository;
import org.magicat.util.AminoAcids;
import org.magicat.util.SolrClientTool;
import org.magicat.util.TikaTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextServiceImpl implements TextService {

    private static final Logger log = LoggerFactory.getLogger(TextService.class);

    private final ArticleRepository articleRepository;
    private final FullTextRepository fullTextRepository;
    private final GridFsTemplate gridFsTemplate;
    private final GeneMapRepository geneMapRepository;
    //private final GeneMIND geneMIND;
    private final SolrClientTool solrClientTool;

    private SentenceModel model;
    private SentenceDetectorME detector;
    private List<Span> names;
    private List<Span> sentences;
    private List<String> genes = Arrays.asList("ABL1,AKT1,ALK,AMER1,APC,AR,ARID1A,ASXL1,ATM,ATRX,AXIN1,BAP1,BCL2,BCOR,BRAF,BRCA1,BRCA2,CARD11,CBL,CDC73,CDH1,CDKN2A,CEBPA,CIC,CREBBP,CTNNB1,DAXX,DNMT3A,EGFR,EP300,ERBB2,EZH2,FBXW7,FGFR2,FGFR3,FLT3,FOXL2,GATA3,GNA11,GNAQ,GNAS,HNF1A,HRAS,IDH1,IDH2,JAK1,JAK2,JAK3,KDM5C,KDM6A,KIT,KMT2D,KRAS,MAP2K1,MAP3K1,MED12,MEN1,MET,MLH1,MPL,MSH2,MSH6,MYD88,NF1,NF2,NFE2L2,NOTCH1,NOTCH2,NPM1,NRAS,PAX5,PBRM1,PDGFRA,PIK3CA,PIK3R1,PPP2R1A,PRDM1,PTCH1,PTEN,PTPN11,RB1,RET,RNF43,SETD2,SF3B1,SMAD2,SMAD4,SMARCA4,SMARCB1,SMO,SOCS1,SPOP,STAG2,STK11,TET2,TNFAIP3,TP53,TSC1,U2AF1,VHL,WT1,AKT2,ARID2,ATR,B2M,BARD1,BCL6,BRD4,BRIP1,BTK,CALR,CASP8,CBFB,CCND1,CCND2,CCND3,CCNE1,CD274,CD79A,CD79B,CDK12,CDK4,CDK6,CDKN1B,CDKN2C,CHEK2,CRLF2,CSF1R,CSF3R,CTCF,CXCR4,DDR2,ERBB3,ERBB4,ERG,ESR1,ETV6,FANCA,FANCC,FGFR1,FGFR4,FLCN,FUBP1,GATA1,GATA2,H3-3A,H3C2,IKZF1,IRF4,JUN,KDM5A,KDR,KEAP1,KMT2A,KMT2C,MAP2K2,MAP2K4,MAPK1,MDM2,MDM4,MITF,MTOR,MUTYH,MYC,MYCL,MYCN,NKX2-1,NSD2,NSD3,NTRK1,NTRK3,PALB2,PDCD1LG2,PDGFRB,PHF6,PIM1,PRKAR1A,RAD21,RAF1,RARA,ROS1,RUNX1,SDHA,SDHB,SDHC,SDHD,SOX2,SPEN,SRC,SRSF2,STAT3,SUFU,SYK,TENT5C,TGFBR2,TMPRSS2,TNFRSF14,TSC2,TSHR,XPO1,AKT3,ARAF,ARID1B,AURKA,AURKB,AXL,BCL10,BCORL1,BCR,BIRC3,BLM,BTG1,CDK8,CDKN2B,CHEK1,CRKL,CYLD,DOT1L,EED,EIF4A2,EPHA3,EPHB1,ERCC4,ETV1,FAS,FGF19,FGF3,FGF4,FH,FLT1,FLT4,FOXO1,FOXP1,GRIN2A,GSK3B,HGF,ID3,IGF1R,IKBKE,IL7R,INPP4B,IRS2,KLF4,LMO1,MALT1,MAP3K13,MCL1,MEF2B,MRE11,MSH3,MSI2,NBN,NCOR1,NFKBIA,NSD1,NT5C2,NTRK2,P2RY8,PDCD1,PIK3CB,PMS2,POLD1,POLE,POT1,PPARG,RAC1,RAD51,RAD51B,RBM10,REL,RHOA,RICTOR,RPTOR,SETBP1,SOX9,STAT5B,SUZ12,TBX3,TCF3,TERT,TET1,TOP1,TP63,TRAF7,ZRSR2,ACVR1,ALOX12B,AXIN2,BCL11B,BCL2L1,BMPR1A,CDKN1A,CIITA,CUL3,CUX1,DDX3X,DICER1,DIS3,DNAJB1,DNMT1,DROSHA,EPAS1,EPHA5,EPHA7,ERCC2,ERCC3,ERCC5,ERRFI1,ETV4,ETV5,EWSR1,FANCD2,FAT1,FBXO11,FOXA1,GLI1,GNA13,H1-2,H3-3B,HDAC1,HLA-A,INHBA,LATS1,LATS2,LYN,MAF,MAP3K14,MAX,MLLT1,MST1R,MYOD1,NCOR2,NOTCH3,NUP93,PARP1,PHOX2B,PIK3C2G,PIK3CG,PIK3R2,PLCG2,PPM1D,PPP6C,PREX2,PRKCI,PRKN,PTPRT,RAD50,RAD51C,RAD51D,RAD52,RAD54L,RECQL4,RUNX1T1,SDHAF2,SGK1,SH2B3,SMAD3,SMARCD1,STAT5A,STAT6,TBL1XR1,TCF7L2,TEK,TMEM127,TRAF2,VEGFA,WWTR1,XRCC2,ZFHX3,ABL2,ABRAXAS1,AGO2,ANKRD11,ARID5B,ASXL2,ATF1,BABAM1,BBC3,BCL2L11,BCL9,CARM1,CCNQ,CD276,CD58,CDC42,CENPA,COP1,CSDE1,CTLA4,CYSLTR2,DCUN1D1,DDIT3,DEK,DNMT3B,DTX1,DUSP22,DUSP4,E2F3,EGFL7,EIF1AX,EIF4E,ELF3,ELOC,EPCAM,ERF,ETNK1,EZH1,FANCG,FANCL,FEV,FLI1,FYN,GNA12,GNB1,GPS2,GREM1,H1-3,H1-4,H2AC11,H2AC16,H2AC17,H2AC6,H2BC11,H2BC12,H2BC17,H2BC4,H2BC5,H3-4,H3-5,H3C1,H3C10,H3C11,H3C12,H3C13,H3C14,H3C3,H3C4,H3C6,H3C7,H3C8,HDAC4,HDAC7,HIF1A,HLA-B,HOXB13,ICOSLG,IFNGR1,IGF1,IGF2,IKZF3,IL10,INHA,INPP4A,INPPL1,INSR,IRF1,IRF8,IRS1,JARID2,KAT6A,KMT2B,KMT5A,KNSTRN,LCK,LMO2,LZTR1,MAFB,MAPK3,MAPKAP1,MDC1,MECOM,MGA,MLLT10,MSI1,MST1,MTAP,MYB,NCOA3,NCSTN,NEGR1,NKX3-1,NOTCH4,NR4A3,NTHL1,NUF2,NUP98,NUTM1,PAK1,PAK5,PCBP1,PDGFB,PDPK1,PGR,PIK3C3,PIK3CD,PIK3R3,PLCG1,PLK2,PMAIP1,PMS1,PNRC1,PPP4R2,PRDM14,PRKD1,PTP4A1,PTPN2,PTPRD,PTPRS,RAB35,RAC2,RASA1,RBM15,RECQL,RHEB,RIT1,RPS6KA4,RPS6KB2,RRAGC,RRAS,RRAS2,RTEL1,RXRA,RYBP,SESN1,SESN2,SESN3,SETDB1,SH2D1A,SHOC2,SHQ1,SLX4,SMARCE1,SMC1A,SMC3,SMYD3,SOS1,SOX17,SPRED1,SS18,STK19,STK40,TAL1,TAP1,TAP2,TCL1A,TFE3,TGFBR1,TLX1,TLX3,TP53BP1,TRAF3,TRAF5,TYK2,U2AF2,UBR5,UPF1,USP8,VTCN1,XBP1,XIAP,YAP1,YES1,ABI1,ACTG1,ACVR1B,AFDN,AFF1,AFF4,AGO1,ALB,APLNR,ARFRP1,ARHGAP26,ARHGAP35,ARHGEF12,ARHGEF28,ARID3A,ARID3B,ARID3C,ARID4A,ARID4B,ARID5A,ARNT,ATIC,ATP6AP1,ATP6V1B2,ATXN2,ATXN7,BACH2,BCL11A,BCL2L2,BCL3,BCL7A,BTG2,CAMTA1,CARS1,CBFA2T3,CD22,CD28,CD70,CD74,CDX2,CEP43,CLTC,CLTCL1,CMTR2,CNTRL,COL1A1,CRBN,CREB1,CREB3L1,CREB3L2,CTNNA1,CTR9,CYP19A1,DDX10,DDX6,DNM2,EBF1,ECT2L,EGR1,ELF4,ELL,EML4,EMSY,EP400,EPOR,EPS15,ESCO2,ETAA1,EZHIP,EZR,FANCE,FANCF,FCGR2B,FCRL4,FGF10,FGF14,FGF23,FGF6,FHIT,FOXF1,FOXO3,FOXO4,FSTL3,FURIN,FUS,GAB1,GAB2,GAS7,GID4,GPHN,GTF2I,H1-5,H2BC8,H4C9,HERPUD1,HEY1,HIP1,HLA-C,HLF,HMGA1,HMGA2,HOXA11,HOXA13,HOXA9,HOXC11,HOXC13,HOXD11,HOXD13,HSP90AA1,HSP90AB1,IGH,IGK,IGL,IL21R,IL3,ITK,KBTBD4,KDSR,KIF5B,KLF5,KLHL6,KSR2,LASP1,LEF1,LPP,LRP1B,LTB,LYL1,MAD2L2,MGAM,MLF1,MLLT3,MLLT6,MN1,MOB3B,MPEG1,MRTFA,MSN,MUC1,MYH11,MYH9,NADK,NCOA2,NDRG1,NFE2,NFKB2,NIN,NRG1,NUMA1,NUP214,NUTM2A,PAFAH1B2,PAX3,PAX7,PAX8,PBX1,PCM1,PDE4DIP,PDK1,PDS5B,PER1,PGBD5,PICALM,PIGA,PLAG1,PML,POU2AF1,PPP2R2A,PRDM16,PRKACA,PRRX1,PSIP1,PTPN1,PTPRO,QKI,RABEP1,RAP1GDS1,RELN,REST,RHOH,RNF213,ROBO1,RPL22,RPN1,RSPO2,SAMHD1,SCG5,SDC4,SERPINB3,SERPINB4,SET,SETD1A,SETD1B,SETD3,SETD4,SETD5,SETD6,SETD7,SETDB2,SH3GL1,SLC34A2,SLFN11,SMARCA2,SMG1,SOCS3,SP140,SPRTN,SRSF3,SSX1,SSX2,SSX4,STAG1,TAF15,TAL2,TET3,TFG,TNFRSF17,TPM3,TPM4,TRA,TRB,TRD,TRG,TRIM24,TRIP11,TRIP13,USP6,VAV1,VAV2,WIF1,ZBTB16,ZMYM2,ZNF217,ZNF384,ZNF521,ZNF703,ZNRF3,ACKR3,ACSL3,ACSL6,ACTB,ACVR2A,ADGRA2,AFF3,AJUBA,APH1A,APOBEC3B,ASMTL,ASPSCR1,ATG5,ATP1A1,ATP2B3,BAX,BCL9L,BRD3,BRSK1,BTLA,BUB1B,CACNA1D,CAD,CANT1,CBLB,CBLC,CCDC6,CCN6,CCNB1IP1,CCNB3,CCT6B,CD36,CDH11,CHCHD7,CHD2,CHD4,CHIC2,CHN1,CILK1,CKS1B,CLIP1,CLP1,CNBP,CNOT3,COL2A1,CPS1,CRTC1,CRTC3,CSF1,CUL4A,CYP17A1,DAZAP1,DCTN1,DDB2,DDR1,DDX4,DDX41,DDX5,DKK1,DKK2,DKK3,DKK4,DUSP2,DUSP9,EIF3E,ELK4,ELN,ELP2,EPHB4,ERC1,ETS1,EXOSC6,EXT1,EXT2,FAF1,FAT4,FBXO31,FES,FGF12,FIP1L1,FLYWCH1,FNBP1,FRS2,GABRA6,GADD45B,GATA4,GATA6,GMPS,GOLGA5,GOPC,GPC3,GRM3,GTSE1,H3C15,H3P6,HIRA,HNRNPA2B1,HOOK3,HOXA3,HSD3B1,IKBKB,IKZF2,IL2,IL6ST,INPP5D,IRF2,IRS4,JAZF1,KAT6B,KCNJ5,KDM2B,KDM4C,KEL,KLF2,KLF3,KLF6,KLK2,KNL1,KTN1,LARP4B,LCP1,LIFR,LMNA,LRIG3,LRP5,LRP6,LRRK2,LTK,MAGED1,MAML2,MAP3K6,MAP3K7,MBD6,MDS2,MEF2C,MEF2D,MERTK,MIB1,MIDEAS,MKI67,MKNK1,MLLT11,MNX1,MTCP1,MYO18A,MYO5A,NAB2,NACA,NBEAP1,NCOA1,NCOA4,NFATC2,NFIB,NFKBIE,NOD1,NONO,NUTM2B,OLIG2,OMD,PAG1,PAK3,PARP2,PARP3,PASK,PATZ1,PC,PCLO,PCSK7,PDCD11,PHF1,PIK3C2B,POLQ,POU5F1,PPFIBP1,PPP1CB,PRCC,PRF1,PRKDC,PRSS1,PRSS8,PTK6,PTK7,PTPN13,PTPN6,PTPRB,PTPRC,PTPRK,RALGDS,RANBP2,RASGEF1A,RMI2,RNF217-AS1,RPL10,RPL5,RSPO3,RUNX2,S1PR2,SALL4,SBDS,SEC31A,SEPTIN5,SEPTIN6,SEPTIN9,SERP2,SFPQ,SFRP1,SFRP2,SFRP4,SIX1,SLC1A2,SLC45A3,SMARCA1,SNCAIP,SND1,SNX29,SOCS2,SOX10,SS18L1,STAT1,STAT2,STAT4,STIL,STRN,TAF1,TCEA1,TCF12,TCL1B,TEC,TERC,TFEB,TFPT,TFRC,TIPARP,TLE1,TLE2,TLE3,TLE4,TLL2,TMEM30A,TMSB4XP8,TNFRSF11A,TPR,TRIM27,TRIM33,TRRAP,TTL,TUSC3,TYRO3,WAS,WDCP,WDR90,WRN,XPA,XPC,YPEL5,YWHAE,YY1,YY1AP1,ZBTB20,ZBTB7A,ZFP36L1,ZMYM3,ZNF24,ZNF331,ZNF750".split(","));


    @Autowired
    public TextServiceImpl(ArticleRepository articleRepository, FullTextRepository fullTextRepository, GridFsTemplate gridFsTemplate, GeneMapRepository geneMapRepository, SolrClientTool solrClientTool/*, GeneMIND geneMIND*/) {
        this.articleRepository = articleRepository;
        this.fullTextRepository = fullTextRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.geneMapRepository = geneMapRepository;
        //this.geneMIND = geneMIND;
        this.solrClientTool = solrClientTool;
        //this.symbols = geneMIND.getSymbols();
        try (InputStream modelIn = new FileInputStream("en-sent.bin")) {
            model = new SentenceModel(modelIn);
            detector = new SentenceDetectorME(model);
        } catch (IOException e) {
            e.printStackTrace();
            model = null;
            detector = null;
        }
    }

    @Override
    public List<String> nameFinder(String text) {
        try {
            InputStream inputStreamNameFinder = new FileInputStream("en-ner-location.bin");
            TokenNameFinderModel model = new TokenNameFinderModel(inputStreamNameFinder);
            NameFinderME nameFinder = new NameFinderME(model);
            Span[] nameSpans = nameFinder.find(text.split("\\."));
            List<String> result = new ArrayList<>();
            for (Span span : nameSpans) {
                System.out.println(text.substring(span.getStart(), span.getEnd()));
                result.add(text.substring(span.getStart(), span.getEnd()));
            }
            names = Arrays.asList(nameSpans);
            return result;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Integer> getSentences(String PMID, String term) {
        if (term == null) return null;
        FullText article = null;
        Optional<FullText> oft = fullTextRepository.findById(PMID);
        if (oft.isPresent()) article = oft.get();
        else {
            log.info("User request for full-text PMID {} does not exist", PMID);
            return null;
        }
        if (article.getTextEntry() == null) {
            log.info("User request for full text of {} does exist", article.getPmId());
            return null;
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        String fullText = article.getTextEntry();

        int refPosition = getReferencesPosition(fullText);
        log.info("Fulltext (plain text version) has References at position {} of total {}", refPosition, fullText.length());

        Span[] spans = detector.sentPosDetect(fullText);
        if (spans.length == 0) return null;
        this.sentences = Arrays.asList(spans);
        String[] sentences;

        sentences = new String[spans.length];
        for (int i = 0; i < spans.length; i++) {
            sentences[i] = spans[i].getCoveredText(fullText).toString();
        }
        //detector.sentDetect(fullText);
        //Set<String> stringSet = new LinkedHashSet<>();
        int island = 0;
        int lastItem = -1;
        for (int i = 0; i < sentences.length; i++) {
            if (spans[i].getStart() > refPosition) break;
            if (i == 0 && sentences[i].contains(term)) {
                result.put(sentences[i], island);
                result.put(sentences[i + 1], island);
                lastItem = i + 1;
            } else if (i == sentences.length - 1 && sentences[i].contains(term)) {
                if (lastItem != -1 && lastItem != i - 1 && lastItem != i - 2) island++;
                result.put(sentences[i - 1], island);
                result.put(sentences[i], island);
                lastItem = i;
            } else if (sentences[i].contains(term)) {
                if (lastItem != -1 && lastItem != i - 1 && lastItem != i - 2) island++;
                result.put(sentences[i - 1], island);
                result.put(sentences[i], island);
                result.put(sentences[i + 1], island);
                lastItem = i;
            }

            //if (result.keySet().size() > setLength) {
            //    for (int j = Math.max(setLength-1, 0); j < result.keySet().size(); j++) log.info("Probability {}", probs[j]);
            //}
        }
        return result;
    }

    private List<Integer> pageNumbers;

    @Override
    public List<Integer> getPageNumbers() {
        return pageNumbers;
    }

    @Override
    public Map<Integer, Integer> getPageCounts() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Integer i : pageNumbers) {
            result.merge(i, 1, Integer::sum);
        }
        return result;
    }

    @Override
    public void setupPageNumbers(String PMID, String mutation) {
        Optional<FullText> oft = fullTextRepository.findById(PMID);
        if (oft.isPresent()) {
            FullText ft = oft.get();
            if (ft.getHTMLEntry() == null) {
                log.info("setupPageNumbers: No HTML Entry - re-running Tika on the document");
                String[] resourceIds = ft.getResourceIds();
                if (resourceIds.length == 0) {
                    log.error("No resourceIds present for document with PMID {}", PMID);
                    return;
                }
                GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[0])));
                if (file != null) {
                    String fileName = file.getFilename();
                    if (fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc")) {
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
                    } else return;
                } else return;
            }
            String s = ft.getHTMLEntry();
            int referencesPos = getReferencesPosition(s, true);
            log.info("setupPageNumbers: references [HTML] start at {} of length {} of PMID {}", referencesPos, s.length(), PMID);
            pageNumbers = new ArrayList<>();
            if (s.length() >= mutation.length()) {
                Item item = new Item(0, s);
                while ((item = nextOccurrence(item, mutation)).getResult() != -1) {
                    if (referencesPos != -1 && item.getResult() > referencesPos) break;
                    int midIndex = item.getResult() + mutation.length() / 2;
                    pageNumbers.add(calculatePageNumber(s, midIndex));
                }
            }
        }
    }

    /**
     * OMG - I am getting caught in a long stupid loop sometimes and these ```<p>``` tags are so useless, we need to get rid of this entirely and come up
     * with a different approach to the problem.
     * @param PMID
     * @param mutation
     * @return
     */
    @Override
    public List<String> getParagraphs(String PMID, String mutation) {
        Optional<FullText> oft = fullTextRepository.findById(PMID);
        if (oft.isEmpty()) {
            log.info("The specified full text does not exist in system: PMID {}, using abstract as the only paragraph", PMID);
            Article a = articleRepository.findByPmId(PMID);
            String paragraph = a.getPubAbstract();
            if (paragraph.contains(mutation)) {
                return Collections.singletonList(paragraph);
            }
            return null;
        } else {
            FullText ft = oft.get();
            if (ft.getHTMLEntry() == null) {
                String[] resourceIds = ft.getResourceIds();
                if (resourceIds.length == 0) {
                    log.info("No resourceIds present for document with PMID {}", PMID);
                    return null;
                }
                GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(resourceIds[0])));
                if (file != null) {
                    String fileName = file.getFilename();
                    if (fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc")) {
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
                            return null;
                        }
                    } else return null;
                } else return null;
            }
            Set<String> result = new LinkedHashSet<>();
            String s = ft.getHTMLEntry();
            int referencesPos = getReferencesPosition(s, true);
            log.info("getParagraphs: references [HTML] start at {} of length {} of PMID {}", referencesPos, s.length(), PMID);
            pageNumbers = new ArrayList<>();
            int n = 1;
            int pos;
            if (s.length() >= mutation.length()) {
                while ((pos = nthOccurrence(s, mutation, n)) != -1) {
                    if (referencesPos != -1 && pos > referencesPos) break;
                    int midIndex = pos + mutation.length() / 2;
                    pageNumbers.add(calculatePageNumber(s, midIndex));
                    String half1 = s.substring(0, midIndex);
                    String half2 = s.substring(midIndex);
                    int opening = half1.lastIndexOf("<p>");
                    int closing = half2.indexOf("</p>");
                    if (closing == -1) closing = half2.length() - 1;
                    if (opening != -1 || half1.length() >= 3) {
                        result.add(half1.substring(opening + 3) + half2.substring(0, closing));
                    }
                    n++;
                }
                return new ArrayList<>(result);
            } else return null;
        }
    }

    @Override
    public int getReferencesPosition(String text, boolean html) {
        if (text == null) {
            log.error("No text sent!");
            return -1;
        }
        List<Integer> positions = new ArrayList<>();
        String[] search = {"r( ){0,1}e( ){0,1}f( ){0,1}e( ){0,1}r( ){0,1}e( ){0,1}n( ){0,1}c( ){0,1}e( ){0,1}s\n", "literature cited\n", "citations\n", "\n( ){0,1}1. ", "acknowledgements\n", "pubmed:"};//, "\nCITATIONS\n", "Citation", "ACKNOWLEDGMENTS", "Acknowledgments", "ACKNOWLEDGEMENTS", "Acknowledgements"};
        if (html)
            search = new String[]{"<p>r( ){0,1}e( ){0,1}f( ){0,1}e( ){0,1}r( ){0,1}e( ){0,1}n( ){0,1}c( ){0,1}e( ){0,1}s", "r( ){0,1}e( ){0,1}f( ){0,1}e( ){0,1}r( ){0,1}e( ){0,1}n( ){0,1}c( ){0,1}e( ){0,1}s\n", "<br/>references", "literature cited", "citations", "acknowledgements", "pubmed:"};
        for (int i = 0; i < search.length - 1; i++) {
            String s = search[i];
            Pattern p = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            int position = -1;
            while (m.find()) {
                if (i < 2 && m.start() > text.length() / 2) return m.start();
                if (position == -1 || m.start() > position) position = m.start();
            }
            if (position != -1) {
                if (!text.substring(position).contains("!{") && position >= text.length() / 2)
                    positions.add(position);
                /*if (position > text.length() - 200) {  // too close to the end to make sense, maybe an extra format item.
                    int biggest = text.split(s).length - 1;
                    if (biggest > 1) return nthOccurrence(text, s, biggest-1);
                } else*/
            }
        }
        if (positions.size() == 1) return positions.get(0);
        else if (positions.size() > 1) {
            int max = 0;
            for (int position : positions) if (position > max) max = position;
            return max;
        }
        int position = -1; // last element is desperation move, usually inside citations
        Pattern p = Pattern.compile(search[search.length - 1], Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        while (m.find()) position = m.start();
        return position;
    }

    @Override
    public int getReferencesPosition(String text) {
        return getReferencesPosition(text, false);
    }

    private static int calculatePageNumber(String s, int position) {
        return s.substring(0, position).split("class=\"page\"").length - 1;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    static class Item {
        private int result;
        private String remainder;
    }

    public static Item nextOccurrence(Item input, String term) {
        String remainder = input.getRemainder();
        int index = input.getResult();
        int tempIndex = -1;
        tempIndex = remainder.indexOf(term);
        if (tempIndex == -1) {
            return new Item(-1, "");
        }
        index += tempIndex;
        return new Item(index, remainder.substring(tempIndex+1));
    }

    public static int nthOccurrence(String input, String term, int n) {
        String tempStr = input;
        //int lastPosition = input.length()-1;
        int tempIndex = -1;
        int finalIndex = 0;
        for (int occurrence = 0; occurrence < n; ++occurrence) {
            tempIndex = tempStr.indexOf(term);
            if (tempIndex == -1) {
                finalIndex = 0;
                break;
            }
            tempStr = tempStr.substring(++tempIndex);
            finalIndex += tempIndex;
        }
        return --finalIndex;
    }

    @Override
    public boolean validateText(String text, String gene, List<String> geneSynonyms, String mutation, int count) {
        //mutation = SolrClientTool.escape(mutation);
        int total = 0;
        Pattern p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,5}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return true;
        String mutationSynonym = AminoAcids.mutationSynonym(mutation);
        List<String> mutationSynonyms = AminoAcids.hotspotSubstitution(mutation);
        if (mutationSynonyms != null) {
            for (String mut : mutationSynonyms) {
                p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,5}?" + mut + "\\b", Pattern.CASE_INSENSITIVE);
                m = p.matcher(text);
                if (m.find()) return true;
            }
        }
        if (!mutationSynonym.equalsIgnoreCase(mutation)) {
            p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,5}?" + mutationSynonym + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.find()) return true;
        }
        if (text.contains(gene) && text.substring(text.indexOf(gene)).contains("{!keywords}")) return true;
        if (geneSynonyms != null) for (String synonym : geneSynonyms) {
            if (text.contains(synonym) && text.substring(text.indexOf(synonym)).contains("{!keywords}")) return true;
        }
        // ^^^ if the gene is found only a few words to the left, or if the gene is in the title of article, it's probably right!
        p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,20}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
        m = p.matcher(text);
        while (m.find()) {
            int start = m.start();
            if (text.substring(start).contains("{!keywords}")) return true;   // if it's in the title!
            total++;
            if (count == 1) return true;
            if (count > 1 && total > 1) return true;
        }
        if (geneSynonyms != null) for (String gs : geneSynonyms) {
            p = Pattern.compile("\\b" + gs + "\\W+(?:\\w+\\W+){0,20}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            while (m.find()) {
                int start = m.start();
                if (text.substring(start).contains("{!keywords}")) return true;   // if it's in the title!
                total++;
                if (count == 1) return true;
                if (count > 1 && total > 1) return true;
            }
        }
        if (!mutationSynonym.equalsIgnoreCase(mutation)) {
            p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,20}?" + mutationSynonym + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            while (m.find()) {
                total++;
                if (count == 1) return true;
                if (count > 1 && total > 1) return true;
            }
            if (geneSynonyms != null) for (String gs : geneSynonyms) {
                p = Pattern.compile("\\b" + gs + "\\W+(?:\\w+\\W+){0,20}?" + mutationSynonym + "\\b", Pattern.CASE_INSENSITIVE);
                m = p.matcher(text);
                while (m.find()) {
                    total++;
                    if (count == 1) return true;
                    if (count > 1 && total > 1) return true;
                }
            }

        }
        if (mutationSynonyms != null) for (String mut: mutationSynonyms) {
            p = Pattern.compile("\\b" + gene + "\\W+(?:\\w+\\W+){0,20}?" + mut + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            while (m.find()) {
                total++;
                if (count == 1) return true;
                if (count > 1 && total > 1) return true;
            }
        }
        if (geneSynonyms != null) for (String synonym : geneSynonyms) {
            p = Pattern.compile("\\b" + synonym + "\\W+(?:\\w+\\W+){0,5}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.find()) return true;
            if (text.contains(synonym) && text.substring(text.indexOf(synonym)).contains("{!keywords}")) return true;
            // ^^^ if the gene is in the title of article, it's probably right!
            p = Pattern.compile("\\b" + synonym + "\\W+(?:\\w+\\W+){0,20}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            while (m.find()) {
                int start = m.start();
                if (text.substring(start).contains("{!keywords}")) return true;   // if it's in the title!
                total++;
                if (count == 1) return true;
                if (count > 1 && total > 1) return true;
            }
        }
        for (String otherGene : genes) {
            if (otherGene.equals(gene)) continue;
            if (!otherGene.equals("WAS")) p = Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,5}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
            else Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,5}?" + mutation + "\\b");
            m = p.matcher(text);
            if (m.find()) {
                log.info("The real gene for {} and {} in this article was {}", gene, mutation, otherGene);
                return false;
            }
            if (!mutationSynonym.equalsIgnoreCase(mutation)) {
                if (!otherGene.equals("WAS")) p = Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,5}?" + mutationSynonym + "\\b", Pattern.CASE_INSENSITIVE);
                else p = Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,5}?" + mutationSynonym + "\\b");
                m = p.matcher(text);
                if (m.find()) {
                    log.info("The real gene for {} and {} in this article was {}", gene, mutation, otherGene);
                    return false;
                }
            }
            if (!otherGene.equals("WAS")) p = Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,50}?" + mutation + "\\b", Pattern.CASE_INSENSITIVE);
            else p = Pattern.compile("\\b" + otherGene + "\\W+(?:\\w+\\W+){0,50}?" + mutation + "\\b");
            m = p.matcher(text);
            int total2 = 0;
            while (m.find()) {
                int start = m.start();
                if (text.substring(start).contains("{!keywords}")) {
                    log.info("The real gene for {} and {} in this article was {}", gene, mutation, otherGene);
                    return false;   // if it's in the title!
                }
                total2++;
                if (count == 1) {
                    log.info("The real gene for {} and {} in this article was {}", gene, mutation, otherGene);
                    return false;
                }
                if (count > 1 && total2 > 1) {
                    log.info("The real gene for {} and {} in this article was {}", gene, mutation, otherGene);
                    return false;
                }
            }
        }
        return true;
        /*
        p = Pattern.compile("\\b" + mutation + "\\W+(?:\\w+\\W+){0,10}?" + gene + "\\b");
        m = p.matcher(text);
        while (m.find()) {
            int start = m.start();
            if (text.substring(start).contains("{!keywords}")) return true;   // if it's in the title!
            total++;
            if (count > 1 && total > 1) return true;
            if (count == 1 && total == count) return true;
        }

        return total > 1;  // ultimately, if we have 2 matches, we should lean towards accepting it.
         */
    }

    @Override
    public Map<String, Integer> findKeywordSentences(String mutation, List<ProjectList> keywords, String pmId, boolean includeSupporting) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (mutation.endsWith(" Fusion")) mutation = mutation.substring(0, mutation.indexOf(" Fusion"));
        FullText article = null;
        Optional<FullText> oft = fullTextRepository.findById(pmId);
        if (oft.isPresent()) article = oft.get();
        for (ProjectList pl : keywords) {
            for (String keyword : pl.getSynonyms()) {
                keyword = keyword.replace("\"", "").trim();
                int wordCount = (mutation + " " + keyword).split(" ").length;
                //String query = "{!complexphrase inOrder=false df=text}" + "\"" + SolrClientTool.escape(mutation) + " " + keyword + "\"" + "~10";
                String query = "\"" + SolrClientTool.escape(mutation) + " " + keyword + "\"" + "~15";
                String fq = "pmid:" + pmId;
                if (pmId.contains("S")) {
                    fq = "pmid_supporting:" + pmId;
                } else if (!includeSupporting) fq += " AND -pmid_supporting:*";
                SolrClientTool.ResultMap resultMap;
                log.info("Query = {}", query);
                if (mutation.contains("*") || SolrClientTool.escape(mutation).contains("\\")) solrClientTool.setDefaultField("text_ws");
                else solrClientTool.setDefaultField("text");
                try {
                    resultMap = solrClientTool.queryHighlight("knowledge", query, fq, 50);
                } catch (IOException | SolrServerException e) {
                    log.error("SolrClientTool queryHighlight method: {}", e.getMessage());
                    continue;
                }
                if (resultMap != null && resultMap.getDocs().size() > 0) {
                    List<SolrItem> solrItems = SolrClientTool.documentsToItems(resultMap.getDocs());
                    Map<String, Map<String, List<String>>> highlightingMap = resultMap.getHighlightingMap();
                    for (SolrItem solrItem : solrItems) {
                        for (String key : highlightingMap.get(solrItem.getId()).keySet()) {
                            if (highlightingMap.get(solrItem.getId()).get(key).size() > 0) {
                                boolean fulltext = false;
                                String text = highlightingMap.get(solrItem.getId()).get(key).get(0);
                                while (text.contains("<mark>")) {
                                    int textlength = text.length();
                                    String half1 = text.substring(0, text.indexOf("<mark>"));
                                    int pos = half1.lastIndexOf(". ");
                                    int pos2 = half1.lastIndexOf(".\n");
                                    if (pos2 != -1 && pos2 > pos) pos = pos2;
                                    String start = half1.substring(pos + 1).stripLeading();
                                    if (half1.contains("{!") && half1.lastIndexOf("{!") > pos) {
                                        start = half1.substring(half1.lastIndexOf("{!"));
                                    }
                                    if (half1.contains("{!fulltext}")) fulltext = true;
                                    //String item = text.substring(text.indexOf("<mark>")+6, text.indexOf("</mark>"));
                                    String half2 = text.substring(text.indexOf("<mark>"));
                                    String end = half2.substring(0, half2.indexOf(".") + 1);
                                    if (half2.contains("!{") && half2.indexOf("{!") < half2.indexOf(".")) {
                                        end = half2.substring(0, half2.indexOf("{!"));
                                    } else if (half2.indexOf(".") == half2.indexOf(" al.")+3 || !Character.isWhitespace(half2.charAt(half2.indexOf(".")+1))) {
                                        int n = 2;
                                        while (nthOccurrence(half2, ".", n) != -1 && (nthOccurrence(half2, ".", n) == nthOccurrence(half2, " al.", n)+3 || !Character.isWhitespace(half2.charAt(nthOccurrence(half2, ".", n)+1)))) {
                                            n++;
                                        }
                                        if (nthOccurrence(half2, ".", n) == -1) n--;
                                        end = half2.substring(0, nthOccurrence(half2, ".", n)+1);
                                    }
                                    String sentence = start + end;
                                    sentence = sentence.replace("-\n", "").replace("\n", " ");

                                    if (sentence.contains("<mark>")) {
                                        int count = 2;
                                        if (count < wordCount) {
                                            String remainder = sentence.substring(sentence.indexOf("<mark>"));
                                            remainder = remainder.substring(remainder.indexOf("<mark>")+6);
                                            if (remainder.contains("<mark>")) count++;
                                        }
                                        if (fulltext && article != null && count >= wordCount && sentence.contains("<mark>") && sentence.contains(mutation)) {
                                            //log.info("CALCULATING PAGE NUMBER");
                                            if (result.get(sentence) != null) sentence += " [repeat]";
                                            result.put(sentence, findPageNumber(article.getHTMLEntry(), sentence));
                                        }
                                        else if (sentence.contains("<mark>")) {
                                            result.put(sentence, -1);
                                        }
                                    }
                                    text = half2.substring(half2.indexOf(".") + 1);
                                    if (text.length() == textlength) text = text.substring(6);  // skip <mark>
                                }
                            }
                        }
                    }
                }
            }
        }
        //for (String line : result.keySet()) {
        //    System.out.println("***" + line + ":" + result.get(line));
        //}
        return result;
    }

    @Override
    public Map<String, Integer> findKeywordOnlySentences(String mutation, List<ProjectList> keywords, String pmId, boolean includeSupporting) {
        Map<String, Integer> result = new LinkedHashMap<>();
        FullText article = null;
        Optional<FullText> oft = fullTextRepository.findById(pmId);
        if (oft.isPresent()) article = oft.get();
        for (ProjectList pl : keywords) {
            for (String keyword : pl.getSynonyms()) {
                String query = "\"" + keyword + "\"";
                String fq = "pmid:" + pmId;
                if (pmId.contains("S")) {
                    fq = "pmid_supporting:" + pmId;
                } else if (!includeSupporting) fq += " AND -pmid_supporting:*";
                SolrClientTool.ResultMap resultMap;
                log.info("Query = {}", query);
                if (mutation.contains("*") || SolrClientTool.escape(mutation).contains("\\")) solrClientTool.setDefaultField("text_ws");
                else solrClientTool.setDefaultField("text");
                try {
                    resultMap = solrClientTool.queryHighlight("knowledge", query, fq, 50);
                } catch (IOException | SolrServerException e) {
                    log.error("SolrClientTool queryHighlight method: {}", e.getMessage());
                    continue;
                }
                if (resultMap != null && resultMap.getDocs().size() > 0) {
                    List<SolrItem> solrItems = SolrClientTool.documentsToItems(resultMap.getDocs());
                    Map<String, Map<String, List<String>>> highlightingMap = resultMap.getHighlightingMap();
                    for (SolrItem solrItem : solrItems) {
                        for (String key : highlightingMap.get(solrItem.getId()).keySet()) {
                            if (highlightingMap.get(solrItem.getId()).get(key).size() > 0) {
                                boolean fulltext = false;
                                String text = highlightingMap.get(solrItem.getId()).get(key).get(0);
                                while (text.contains("<mark>")) {
                                    //log.info("LOOP {}", text.length());
                                    int textlength = text.length();
                                    String half1 = text.substring(0, text.indexOf("<mark>"));
                                    int pos = half1.lastIndexOf(". ");
                                    int pos2 = half1.lastIndexOf(".\n");
                                    if (pos2 != -1 && pos2 > pos) pos = pos2;
                                    String start = half1.substring(pos + 1).stripLeading();
                                    if (half1.contains("{!") && half1.lastIndexOf("{!") > pos) {
                                        start = half1.substring(half1.lastIndexOf("{!"));
                                    }
                                    if (half1.contains("{!fulltext}")) fulltext = true;
                                    //String item = text.substring(text.indexOf("<mark>") + 6, text.indexOf("</mark>"));
                                    String half2 = text.substring(text.indexOf("<mark>"));
                                    String end = half2.substring(0, half2.indexOf(".") + 1);
                                    if (half2.contains("{!") && half2.indexOf("{!") < half2.indexOf(".")) {
                                        end = half2.substring(0, half2.indexOf("{!"));
                                    } else if (half2.indexOf(".") == half2.indexOf(" al.")+3 || !Character.isWhitespace(half2.charAt(half2.indexOf(".")+1))) {
                                        int n = 2;
                                        //Item val = nextOccurrence(new Item(0, half2), ".");
                                        while (nthOccurrence(half2, ".", n) != -1 && (nthOccurrence(half2, ".", n) == nthOccurrence(half2, " al.", n)+3 || !Character.isWhitespace(half2.charAt(nthOccurrence(half2, ".", n)+1)))) {
                                            n++;
                                        }
                                        /*if (nthOccurrence(half2, ".", n) == -1)*/
                                        //n--;
                                        end = half2.substring(0, nthOccurrence(half2, ".", --n)+1);
                                    }
                                    String sentence = start + end;
                                    sentence = sentence.replace("-\n", "").replace("\n", " ");
                                    if (fulltext && article != null && sentence.contains("<mark>")) {
                                        //log.info("CALCULATING PAGE NUMBER");
                                        if (result.get(sentence) != null) sentence += " [repeat]";
                                        result.put(sentence, findPageNumber(article.getHTMLEntry(), sentence));
                                    }
                                    else if (sentence.contains("<mark>")) result.put(sentence, -1);
                                    text = half2.substring(half2.indexOf(".") + 1);
                                    if (text.length() == textlength) text = text.substring(6);  // skip <mark>, no period?
                                }
                            }
                            //System.out.println("key = " + key);
                            //System.out.println(highlightingMap.get(solrItem.getId()).get(key));
                            //result.addAll(highlightingMap.get(solrItem.getId()).get(key));
                        }
                    }
                }
            }
        }
        //for (String line : result.keySet()) {
        //    System.out.println("***" + line + ":" + result.get(line));
        //}
        return result;
    }

    @Override
    public int findPageNumber(String HTMLEntry, String sentence) {
        if (sentence.startsWith("{!")) sentence = sentence.substring(sentence.indexOf(" ")+1);
        String html = HTMLEntry.replace("<p>", "").replace("</p>", "")
                .replace("-\n", "").replace("\n", " ");
        String[] words = sentence.split(" ");
        if (words.length > 4) for (int i = 0; i < words.length-3; i++) {
            String search = words[i] + " " + words[i+1] + " " + words[i+2] + " " + words[i+3];
            if (html.contains(search)) {
                return html.substring(0, html.indexOf(search)).split("class=\"page\"").length - 1;
            }
        } else if (html.contains(sentence)) {
            return html.substring(0, html.indexOf(sentence)).split("class=\"page\"").length - 1;
        }
        return -1;
    }

    /**
     * Strips unnecessary characters from input String and either brings together word and sub/superscript (e.g. "G^^(67)" becomes "G67") or
     * separates word and sub/superscript into two words (e.g. "BRAF^(V600E)" becomes "BRAF V600E ")
     * @param input The input string to transform
     * @param mergeSubSuperScripts If true, brings together word and its sub/superscript, if false, separates word and its sub/superscript into two words
     * @return The replaced input string, preserving the original length by adding the same number of spaces
     */
    public static String textTransform(String input, boolean mergeSubSuperScripts) {
        while (input.contains("^^(")) {
            int position = input.indexOf("^^(");
            int endPosition = position + input.substring(position).indexOf(")");
            if (mergeSubSuperScripts) {
                int lastWS = input.substring(0, position).lastIndexOf(" ");
                if (input.substring(0, position).lastIndexOf("\t") > lastWS) lastWS = input.substring(0, position).lastIndexOf("\t");
                input = input.substring(0, lastWS+1) + "    " + input.substring(lastWS+1, position) + input.substring(position+3, endPosition) + input.substring(endPosition+1);
            }
            else input = input.substring(0, position) + "  " + input.substring(position+3, endPosition) + "  " + input.substring(endPosition+1);
        }
        while (input.contains("__(")) {
            int position = input.indexOf("__(");
            int endPosition = position + input.substring(position).indexOf(")");
            if (mergeSubSuperScripts) {
                int lastWS = input.substring(0, position).lastIndexOf(" ");
                if (input.substring(0, position).lastIndexOf("\t") > lastWS) lastWS = input.substring(0, position).lastIndexOf("\t");
                input = input.substring(0, lastWS+1) + "    " + input.substring(lastWS+1, position) + input.substring(position+3, endPosition) + input.substring(endPosition+1);
            }
            else input = input.substring(0, position) + "  " + input.substring(position+3, endPosition) + "  " + input.substring(endPosition+1);
        }
        // next four substitutions also preserve the input String's length
        input = input.replaceAll("[?.!,%&^$#@\"()\\[\\]\\\\]", " ");
        input = input.replaceAll("( [-/])|([-/] )", "  ");
        input = input.replaceAll("(\t[-/])", "\t ");
        input = input.replaceAll("([-/]\t)", " \t");
        // split two missense mutations like "G30V-A102T" to "G30V A102T"
        Matcher m = Pattern.compile("\\b[ACDEFGHIKLMNPQRSTVWY][1-9]\\d{0,4}[ACDEFGHIKLMNPQRSTVWY]").matcher(input);
        String result = "";
        while (m.find()) {
            if (!Character.isWhitespace(input.charAt(m.start())))
                result = input.substring(0, m.start()) + " " + input.substring(m.start()+1);
        }
        if (result.length() > 0) input = result;
        return input;
    }


}
