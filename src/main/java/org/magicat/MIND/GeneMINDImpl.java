package org.magicat.MIND;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocumentList;
import org.magicat.model.SequenceItem;
import org.magicat.model.Target;
import org.magicat.model.Variant;
import org.magicat.repository.TargetRepository;
import org.magicat.repository.VariantRepository;
import org.magicat.service.SolrService;
import org.magicat.service.TextService;
import org.magicat.util.SolrClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GeneMINDImpl implements GeneMIND, Serializable {

    @Serial
    private static final long serialVersionUID = 123234567890L;
    private static final Logger log = LoggerFactory.getLogger(GeneMIND.class);

    private final String regex_missense_ws = "/[\\(\\[]{0,1}[ACDEFGHIKLMNPQRSTVWY][1-9][0-9]{1,4}[ACDEFGHIKLMNPQRSTVWY][\\)\\]\\,]{0,1}/";
    private final String regex_missense = "/[ACDEFGHIKLMNPQRSTVWY][1-9][0-9]{1,4}[ACDEFGHIKLMNPQRSTVWY]/";

    transient private final TargetRepository targetRepository;
    transient private final VariantRepository variantRepository;
    transient private final SolrService solrService;
    transient private final TextService textService;

    private boolean forward = true, reportEnd = false;
    private Long position = 0L;

    private final List<Target> targetsSorted = new ArrayList<>();  // sorted by symbol
    private Set<String> symbols = null;
    private Map<String, Map<String, List<String>>> highlightingMap;
    private final Map<String, List<String>> synonyms = new HashMap<>();

    @Autowired
    public GeneMINDImpl(TargetRepository targetRepository, VariantRepository variantRepository, SolrService solrService, TextService textService) {
        this.targetRepository = targetRepository;
        this.variantRepository = variantRepository;
        this.solrService = solrService;
        this.textService = textService;
    }

    public GeneMIND load() {
        if (targetsSorted.size() > 0) return this;  // already loaded!
        List<Target> targets = targetRepository.findAll();
        symbols = targets.stream().map(Target::getSymbol).collect(Collectors.toCollection(TreeSet::new));
        symbols.remove("");
        symbols.forEach(s -> targets.parallelStream().filter(t -> t.getSymbol().equals(s)).findAny().ifPresent(targetsSorted::add));
        log.info("There are {} unique gene symbols", symbols.size());
        targetsSorted.forEach(t -> synonyms.merge(t.getSymbol(), Arrays.asList(t.getSynonyms().split(";")), (listOne, listTwo) -> Stream.concat(listOne.stream(), listTwo.stream())
                .collect(Collectors.toList())));
        return this;
    }

    public Set<String> getKinases() {
        return targetRepository.findAllByFamily("Kinase").parallelStream().map(Target::getSymbol).collect(Collectors.toCollection(TreeSet::new));
    }

    private String wildcard(String seq, int n, boolean fuzzy) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < seq.length()+n; i++) {
            if (i != 0 && i % 5 == 0) result.append(fuzzy ? "~1 " : " ");
            if (i < n) result.append("?");
            else result.append(seq.charAt(i-n));
        }
        if ((seq.length()+n) % 5 != 0) result.append(fuzzy ? "*~1" : "*");
        return result.toString();
    }

    private String complement(String seq) {
        StringBuilder result = new StringBuilder();
        for (int i = seq.length()-1; i >= 0; i--) {
            if (Character.toLowerCase(seq.charAt(i)) == 'a') result.append("t");
            else if (Character.toLowerCase(seq.charAt(i)) == 't') result.append("a");
            else if (Character.toLowerCase(seq.charAt(i)) == 'g') result.append("c");
            else if (Character.toLowerCase(seq.charAt(i)) == 'c') result.append("g");
            else result.append("?");
        }
        return result.toString();
    }

    @Override
    public org.magicat.MIND.GeneMIND.GeneResult findGene(String startSeq, String endSeq) {
        org.magicat.MIND.GeneMIND.GeneResult gr = new GeneMIND.GeneResult();
        startSeq = startSeq.toLowerCase().replace(" ", "");
        endSeq = endSeq.toLowerCase().replace(" ", "");
        SolrClientTool solrClientTool = solrService.getSolrClientTool();
        solrClientTool.setCollection("t2t");
        solrClientTool.setParser("lucene");
        solrClientTool.setDefaultField("seq");
        StringBuilder query = new StringBuilder();
        query.append("{!complexphrase inOrder=true}");

        for (int i = 0; i < 5; i++) {
            query.append("seq:\"").append(wildcard(startSeq, i, false)).append(i == 4 ? "\"": "\" OR ");
        }
        try {
            SolrDocumentList sdl = solrClientTool.find("t2t", query.toString());
            if (sdl.size() == 0) {
                query = new StringBuilder();
                String cseq = complement(startSeq);
                query.append("{!complexphrase inOrder=true}");
                for (int i = 0; i < 5; i++) {
                    query.append("seq:\"").append(wildcard(cseq,i, false)).append(i == 4 ? "\"" : "\" OR ");
                }
                sdl = solrClientTool.find("t2t", query.toString());
                if (sdl.size() == 0) {
                    log.error("Neither forward nor reverse starting sequence found");
                    return null;
                }
                DocumentObjectBinder binder = new DocumentObjectBinder();
                List<SequenceItem> result = binder.getBeans(SequenceItem.class, sdl);
                SequenceItem item = result.get(0);
                String sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(cseq)) {
                    gr.setStartPosition(item.getPosition().get(0).intValue() + sequence.indexOf(cseq) + cseq.length() - 1);
                }
                query = new StringBuilder();
                query.append("{!complexphrase inOrder=true}");

                cseq = complement(endSeq);
                for (int i = 0; i < 5; i++) {
                    query.append("seq:\"").append(wildcard(cseq,i, false)).append(i == 4 ? "\"" : "\" OR ");
                }
                sdl = solrClientTool.find("t2t", query.toString());
                if (sdl.size() == 0) {
                    log.error("No reverse ending sequence found");
                    return null;
                }
                binder = new DocumentObjectBinder();
                result = binder.getBeans(SequenceItem.class, sdl);
                item = result.get(0);
                sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(cseq)) {
                    gr.setEndPosition(item.getPosition().get(0).intValue() + sequence.indexOf(cseq));
                }
                gr.setChromosome(item.getChromosome().get(0));
                gr.setForward(false);
            } else {
                DocumentObjectBinder binder = new DocumentObjectBinder();
                List<SequenceItem> result = binder.getBeans(SequenceItem.class, sdl);
                SequenceItem item = result.get(0);
                String sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(startSeq)) {
                    gr.setStartPosition(item.getPosition().get(0).intValue() + sequence.indexOf(startSeq));
                }
                query = new StringBuilder();
                query.append("{!complexphrase inOrder=true}");
                for (int i = 0; i < 5; i++) {
                    query.append("seq:\"").append(wildcard(endSeq, i, false)).append(i == 4 ? "\"" : "\" OR ");
                }
                sdl = solrClientTool.find("t2t", query.toString());
                if (sdl.size() == 0) {
                    log.error("Could not find ending strand for gene");
                    return null;
                }
                binder = new DocumentObjectBinder();
                result = binder.getBeans(SequenceItem.class, sdl);
                item = result.get(0);
                sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(endSeq)) {
                    gr.setEndPosition(item.getPosition().get(0).intValue() + sequence.indexOf(endSeq) + endSeq.length() - 1);
                }
                gr.setChromosome(item.getChromosome().get(0));
                gr.setForward(true);
            }
        } catch (SolrServerException|IOException e) {
            log.error("ERROR IN FINDGENE: {}", e.getMessage());
        }
        return gr;
    }

    @Override
    public List<SequenceItem> findSequence(String seq, boolean fuzzy) {
        if (fuzzy) log.debug("Running fuzzy search to find issues with missing sequences");
        seq = seq.toLowerCase().replace(" ", "");   // remove spaces
        SolrClientTool solrClientTool = solrService.getSolrClientTool();
        solrClientTool.setCollection("t2t");
        solrClientTool.setParser("lucene");
        solrClientTool.setDefaultField("seq");
        StringBuilder query = new StringBuilder();
        query.append("{!complexphrase inOrder=true}");
        for (int i = 0; i < 5; i++) {
            query.append("seq:\"").append(wildcard(seq, i, fuzzy)).append("\" OR ");
        }
        String cseq = complement(seq);
        for (int i = 0; i < 5; i++) {
            query.append("seq:\"").append(wildcard(cseq,i, fuzzy)).append(i == 4 ? "\"" : "\" OR ");
        }
        List<SequenceItem> result = null;
        //log.info(query.toString());
        try {
            SolrClientTool.ResultMap results = solrClientTool.find("t2t", query.toString(), true);
            SolrDocumentList sdl = results.getDocs();
            highlightingMap = results.getHighlightingMap();
            DocumentObjectBinder binder = new DocumentObjectBinder();
            result = binder.getBeans(SequenceItem.class, sdl);
            // if there is only a single result with possible redundant location
            if (sdl.size() == 0) {
                if (fuzzy) log.debug("NO RESULT WITH FUZZY SEARCH ALSO");
                position = -1L;
            } else if (result != null && result.size() > 0 /*&& result.size() <= 2*/) {
                if (result.size() > 2) log.debug("TOO MANY MATCHES, {}", result.size());
                if (fuzzy) log.debug("Restored issue with missing sequence!");
                SequenceItem item = result.get(0);
                String sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(seq)) {
                    forward = true;
                    if (!reportEnd) position = item.getPosition().get(0) + sequence.indexOf(seq);
                    else position = item.getPosition().get(0) + sequence.indexOf(seq) + seq.length();
                    item.setPosition(List.of(position));
                }
                if (sequence.contains(cseq)) {
                    forward = false;
                    if (!reportEnd) position = item.getPosition().get(0) + sequence.indexOf(cseq) + cseq.length();
                    else position = item.getPosition().get(0) + sequence.indexOf(cseq);
                    item.setPosition(List.of(position));
                }
            } else if (result != null && result.size() > 2 && fuzzy) {
                log.debug("FUZZY PROBLEM - TOO MANY SEQUENCES FOUND, {}", result.size());
            }
        } catch (SolrServerException|IOException e) {
            log.error("Error searching genome: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public Set<String> getSymbols() {
        if (symbols == null) load();
        return symbols;
    }

    @Override
    public List<Target> getTargets() {
        if (targetsSorted.size() == 0) load();
        return targetsSorted;
    }

    @Override
    public Map<String, Map<String, List<String>>> getHighlightingMap() {
        return highlightingMap;
    }

    public void setHighlightingMap(Map<String, Map<String, List<String>>> highlightingMap) {
        this.highlightingMap = highlightingMap;
    }

    public boolean isForward() {
        return forward;
    }

    public Long getPosition() {
        return position;
    }


    public boolean isReportEnd() {
        return reportEnd;
    }

    public void setReportEnd(boolean reportEnd) {
        this.reportEnd = reportEnd;
    }

    @Override
    public void findNewAlterations() {
        List<Variant> variants = variantRepository.findAll();
        Collections.shuffle(variants);
        Map<String, List<String>> alterationGenes = new ConcurrentHashMap<>(), geneAlterations = new ConcurrentHashMap<>();
        variants.parallelStream().forEach(v -> {
           alterationGenes.putIfAbsent(v.getMutation(), new ArrayList<>());
           geneAlterations.putIfAbsent(v.getGene(), new ArrayList<>());
           alterationGenes.get(v.getMutation()).add(v.getGene());
           geneAlterations.get(v.getGene()).add(v.getMutation());
        });
    }

}
