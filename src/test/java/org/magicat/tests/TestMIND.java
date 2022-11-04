package org.magicat.tests;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.magicat.MIND.GeneMIND;
import org.magicat.MIND.StructureMIND;
import org.magicat.model.SequenceItem;
import org.magicat.model.Target;
import org.magicat.repository.TargetRepository;
import org.magicat.util.SolrClientTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TestMIND {

    @Autowired
    GeneMIND geneMIND;
    @Autowired
    SolrClientTool solrClientTool;
    @Autowired
    TargetRepository targetRepository;

    @Test
    public void testGeneMIND() {
        System.out.println(geneMIND.getSymbols());
    }

    @Test
    void testSeqSearch() {
        // Abl1 synthetic construct sequence
        //List<SequenceItem> sequences = geneMIND.findSequence("AGACACCTCTGCCCTCACCATGAGCCTCTGGCAGCCCCTGGTCCTGGTGCTCCTGGTGCTGGGCTGCTGC", true);//geneMIND.findSequence("tcccccaactacgacaagtg");
        List<SequenceItem> sequences = geneMIND.findSequence("GGGAGACACCTCTGCCCTCA", true);//geneMIND.findSequence("tcccccaactacgacaagtg");

        Map<String, Map<String, List<String>>> highlightingMap = geneMIND.getHighlightingMap();
        if (sequences != null && sequences.size() == 2 && Math.abs(sequences.get(0).getPosition().get(0)-sequences.get(1).getPosition().get(0)) == 100) {
            if ((sequences.get(0).getPosition().get(0)-1) % 200 != 0) sequences.remove(0);
            else sequences.remove(1);
        }
        if (sequences != null && sequences.size() > 0) for (SequenceItem s: sequences) {
            System.out.println(s.getChromosome() + " : " + s.getSeq());
            System.out.println(s);
            if (geneMIND.isForward()) System.out.println("Forward strand at position " + s.getPosition());
            else System.out.println("Reverse strand starting at position " + s.getPosition());
            //System.out.println(highlightingMap);
            if (highlightingMap.get(s.getId()) != null) for (String str: highlightingMap.get(s.getId()).keySet()) {
                System.out.println(highlightingMap.get(s.getId()).get(str));
            }
        }
        /*
        [Chr 9] : [tctgt gggct gaagg ctgtt ccctg tttcc ttcag ctcta cgtct cctcc gagag ccgct tcaac accct ggccg agttg gttca tcatc attca acggt ggccg acggg ctcat cacca cgctc catta tccag cccca aagcg caaca agccc actgt ctatg gtgtg tcccc caact acgac aagtg ggaga tggaa]
SequenceItem(id=3da654e, chromosome=[Chr 9], name=[CP068269.2], position=[143067601], genbank=[CP068269.2], refseq=[NC_060933.1], seq=[tctgt gggct gaagg ctgtt ccctg tttcc ttcag ctcta cgtct cctcc gagag ccgct tcaac accct ggccg agttg gttca tcatc attca acggt ggccg acggg ctcat cacca cgctc catta tccag cccca aagcg caaca agccc actgt ctatg gtgtg tcccc caact acgac aagtg ggaga tggaa])
        [Chr 9] : [ggccg acggg ctcat cacca cgctc catta tccag cccca aagcg caaca agccc actgt ctatg gtgtg tcccc caact acgac aagtg ggaga tggaa cgcac ggaca tcacc atgaa gcaca agctg ggcgg gggcc agtac gggga ggtgt acgag ggcgt gtgga agaaa tacag cctga cggtg gccgt gaaga]
SequenceItem(id=1ee1177c, chromosome=[Chr 9], name=[CP068269.2], position=[143067701], genbank=[CP068269.2], refseq=[NC_060933.1], seq=[ggccg acggg ctcat cacca cgctc catta tccag cccca aagcg caaca agccc actgt ctatg gtgtg tcccc caact acgac aagtg ggaga tggaa cgcac ggaca tcacc atgaa gcaca agctg ggcgg gggcc agtac gggga ggtgt acgag ggcgt gtgga agaaa tacag cctga cggtg gccgt gaaga])
         */
    }


    @Test
    public void testPage() {
        try {
            SolrDocumentList results = solrClientTool.deepPage("text:BRCA1", 2000);
            System.out.println(results.size() + " articles retrieved");
            //for (SolrDocument d : results) {
                //System.out.println(d.get("pmid"));
                //System.out.println(d.get("text"));
            //}
        } catch (IOException | SolrServerException e ) {
            e.printStackTrace();
        }
    }

    @Test
    public void testKinases() {
        String[] genes = "ABL1,ABRAXAS1,ACAT1,ACO2,ACSF2,ACVR1,AGK,AGO2,AKAP10,AKT1,AKT2,AKT3,ALB,ALDH2,ALDOA,ALG9,ALK,ALOX12B,AMER1,AMPH,ANK3,ANKRD11,APC,APLNR,APOOL,AR,ARAF,ARHGAP35,ARHGEF11,ARHGEF17,ARID1A,ARID1B,ARID2,ARID5B,ARNTL2,ASPSCR1,ASXL1,ASXL2,ATF1,ATF6B,ATM,ATR,ATRX,ATXN7,AURKA,AURKB,AXIN1,AXIN2,AXL,B2M,BABAM1,BAD,BAI3,BAIAP2,BAIAP2L1,BAP1,BARD1,BBC3,BCL10,BCL2,BCL2L1,BCL2L11,BCL2L14,BCL6,BCOR,BIRC3,BLM,BMP2K,BMPR1A,BRAF,BRCA1,BRCA2,BRD4,BRIP1,BTBD11,BTK,CA10,CALR,CAMK2A,CARD11,CARM1,CASP8,CBFB,CBL,CCDC171,CCDC6,CCND1,CCND2,CCND3,CCNE1,CCNQ,CCNT1,CD274,CD276,CD58,CD74,CD79A,CD79B,CDC42,CDC73,CDH1,CDK12,CDK4,CDK5RAP2,CDK6,CDK8,CDKN1A,CDKN1B,CDKN2A,CDKN2B,CDKN2C,CEACAM7,CEBPA,CECR2,CENPA,CEP89,CFDP1,CHAF1A,CHEK1,CHEK2,CIC,CIT,CLEC1A,CLIP2,CLMN,CLSPN,COL15A1,COP1,CREBBP,CRKL,CRLF2,CSDE1,CSF1R,CSF2RA,CSF3R,CTCF,CTLA4,CTNNB1,CTR9,CTTNBP2,CUL1,CUL3,CXCR4,CYB5R3,CYLD,CYP19A1,CYSLTR2,DAXX,DCTN1,DCUN1D1,DDR2,DEPDC7,DERA,DGKH,DHX35,DIAPH1,DICER1,DIS3,DLG1,DNAJB1,DNAJC8,DNM2,DNMT1,DNMT3A,DNMT3B,DOCK1,DOT1L,DROSHA,DTNB,DUSP4,E2F3,E4F1,EED,EGFL7,EGFR,EIF1AX,EIF2C1,EIF4A2,EIF4E,ELF3,ELOC,EMILIN1,EML4,EP300,EPAS1,EPB41,EPCAM,EPHA3,EPHA5,EPHA7,EPHB1,ERBB2,ERBB3,ERBB4,ERCC2,ERCC3,ERCC4,ERCC5,ERF,ERG,ERRFI1,ESR1,ETV1,ETV6,EWSR1,EXOC4,EZH1,EZH2,EZHIP,EZR,FAM175A,FAM211A,FAM46C,FAM58A,FANCA,FANCC,FAT1,FBXL7,FBXW7,FER1L6,FES,FGF19,FGF3,FGF4,FGFR1,FGFR2,FGFR3,FGFR4,FH,FLCN,FLT1,FLT3,FLT4,FMN1,FOXA1,FOXF1,FOXL2,FOXO1,FOXP1,FUBP1,FYN,GAB1,GAB2,GAS6,GATA1,GATA2,GATA3,GBAS,GLI1,GNA11,GNAI2,GNAQ,GNAS,GNB1,GNL2,GPS2,GREB1L,GREM1,GRIN2A,GRIPAP1,GRM8,GSDMA,GSK3B,H1-2,H3-3B,H3-5,H3C1,H3C10,H3C13,H3C14,H3C2,H3C3,H3C4,H3C6,H3C7,H3C8,H3F3A,H3F3B,H3F3C,HGF,HIST1H1C,HIST1H2BD,HIST1H3A,HIST1H3B,HIST1H3C,HIST1H3D,HIST1H3E,HIST1H3F,HIST1H3G,HIST1H3H,HIST1H3I,HIST1H3J,HIST2H3C,HIST2H3D,HIST3H3,HLA-A,HLA-B,HLA-C,HMBOX1,HMGA2,HNF1A,HOXB13,HRAS,ICOSLG,ID3,IDH1,IDH2,IFNGR1,IFT140,IGF1,IGF1R,IGF2,IGHMBP2,IKBKE,IKZF1,IKZF3,IL10,IL7R,INHA,INHBA,INPP4A,INPP4B,INPPL1,INSR,IRF4,IRS1,IRS2,JAK1,JAK2,JAK3,JUN,KBTBD4,KCNK13,KCNMA1,KCNMB3,KDM5A,KDM5C,KDM6A,KDR,KEAP1,KIAA1244,KIAA1549,KIAA1731,KIF13A,KIF5B,KIT,KLF4,KLF5,KLHL12,KMT2A,KMT2B,KMT2C,KMT2D,KMT5A,KNSTRN,KRAS,KREMEN1,LAMA4,LATS1,LATS2,LETM2,LGR5,LMNA,LMO1,LRRC16A,LYN,LZTR1,LZTS1,MAD2L2,MALT1,MAP2K1,MAP2K2,MAP2K4,MAP3K1,MAP3K13,MAP3K14,MAPK1,MAPK3,MAPKAP1,MAX,MCL1,MDC1,MDM2,MDM4,MED12,MED20,MEF2B,MEN1,MET,MFSD11,MGA,MICU1,MITF,MKLN1,MKRN1,MLH1,MLL,MLL2,MLL3,MLL4,MLLT1,MPL,MRE11,MRE11A,MSH2,MSH3,MSH6,MSI1,MSI2,MST1,MST1R,MTAP,MTOR,MUTYH,MYC,MYCL,MYCL1,MYCN,MYD88,MYL6B,MYOD1,NAB2,NADK,NAV1,NBN,NCOA3,NCOA4,NCOR1,NEFH,NEGR1,NF1,NF2,NFE2L2,NFKBIA,NKX2,NKX2-1,NKX3,NKX3-1,NLGN3,NLRX1,NOS1AP,NOTCH1,NOTCH2,NOTCH3,NOTCH4,NPAS1,NPM1,NR4A3,NRAS,NRG1,NRG3,NSD1,NSD3,NT5C1B,NTHL1,NTN1,NTRK1,NTRK2,NTRK3,NUDC,NUF2,NUP210L,NUP93,OSGEP,OXSR1,PAK1,PAK7,PALB2,PAPSS2,PARK2,PARP1,PAWR,PAX5,PAX8,PBRM1,PDCD1,PDCD1LG2,PDGFRA,PDGFRB,PDPK1,PEMT,PGBD5,PGR,PHLDB3,PHOX2B,PIEZO1,PIK3C2G,PIK3C3,PIK3CA,PIK3CB,PIK3CD,PIK3CG,PIK3R1,PIK3R2,PIK3R3,PIM1,PLCD3,PLCG2,PLEKHA7,PLK2,PMAIP1,PMS1,PMS2,PNRC1,POLD1,POLE,POT1,PPARG,PPM1D,PPM1G,PPP2R1A,PPP4R2,PPP6C,PRCC,PRDM1,PRDM14,PREX2,PRKACA,PRKAR1A,PRKCE,PRKCI,PRKD1,PRKN,PTCH1,PTEN,PTP4A1,PTPN11,PTPRD,PTPRS,PTPRT,PXN,QARS,RAB11FIP3,RAB11FIP4,RAB35,RAB5C,RABGAP1,RAC1,RAC2,RAD21,RAD50,RAD51,RAD51B,RAD51C,RAD51D,RAD52,RAD54L,RAF1,RARA,RASA1,RB1,RBM10,RBPMS,RCBTB2,RECQL,RECQL4,REL,RELA,RERE,REST,RET,RFWD2,RHEB,RHOA,RICTOR,RIN2,RIT1,RNF43,ROS1,RPGRIP1L,RPS6KA4,RPS6KB2,RPTOR,RRAGC,RRAS,RRAS2,RRBP1,RTEL1,RUFY1,RUFY2,RUNX1,RXRA,RYBP,SBF2,SCG5,SDC4,SDHA,SDHAF2,SDHB,SDHC,SDHD,SEC16A,SEC23B,SERPINB3,SERPINB4,SESN1,SESN2,SESN3,SETD2,SETD8,SETDB1,SF3B1,SFXN2,SH2B3,SH2D1A,SHMT1,SHOC2,SHQ1,SIPA1L3,SIRT2,SLC24A3,SLC47A1,SLC5A5,SLFN11,SLIT2,SLX4,SMAD2,SMAD3,SMAD4,SMARCA2,SMARCA4,SMARCB1,SMARCD1,SMARCE1,SMO,SMYD3,SOCS1,SOS1,SOX17,SOX2,SOX9,SPECC1L,SPEN,SPOP,SPRED1,SPRTN,SQSTM1,SRC,SRSF2,ST7,STAG2,STAT3,STAT5A,STAT5B,STK11,STK19,STK40,STMN3,STRN,SUFU,SUGP1,SUZ12,SYK,SYMPK,SYNE1,TAP1,TAP2,TBX3,TCEB1,TCF3,TCF7L2,TEF,TEK,TENM2,TENT5C,TERT,TET1,TET2,TFE3,TFG,TGFBR1,TGFBR2,THSD4,TIMM8B,TLK2,TMBIM4,TMEM127,TMEM145,TMPRSS2,TNFAIP3,TNFRSF14,TNFRSF21,TOP1,TP53,TP53BP1,TP63,TPM3,TPR,TRAF2,TRAF7,TRAP1,TRAPPC9,TRIP13,TRMT1,TSC1,TSC2,TSEN2,TSHR,U2AF1,UPF1,USP6,USP8,VEGFA,VHL,VPS9D1,VTCN1,WHSC1,WHSC1L1,WIBG,WT1,WWOX,WWTR1,XIAP,XPO1,XRCC2,YAP1,YBX2,YES1,ZFHX3,ZFPM2,ZNF429,ZNF532,ZRSR2".split(",");
        Set<String> value = new TreeSet<>(Arrays.asList(genes));
        Set<String> kinases = geneMIND.getKinases();
        System.out.println(kinases.size());
        kinases.retainAll(value); // find the ones that are in our set
        kinases.forEach(System.out::println);
        System.out.println(kinases.size());
        Set<String> receptors = targetRepository.findByNameRegex("receptor").parallelStream().map(Target::getSymbol).collect(Collectors.toSet());
        kinases.removeAll(receptors);
        kinases.forEach(System.out::println);
        System.out.println(kinases.size());
    }

    @Autowired
    StructureMIND structureMIND;

    @Test
    public void testAblStructures() {
        structureMIND.analyzeStructures(structureMIND.getStructures());
    }

}
