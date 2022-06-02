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
    private static final long serialVersionUID = 1234567890L;
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

    private String wildcard(String seq, int n) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < seq.length()+n; i++) {
            if (i != 0 && i % 5 == 0) result.append(" ");
            if (i < n) result.append("?");
            else result.append(seq.charAt(i-n));
        }
        if ((seq.length()+n) % 5 != 0) result.append("?".repeat(5 - (seq.length() + n) % 5));
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
    public List<SequenceItem> findSequence(String seq) {
        seq = seq.toLowerCase().replace(" ", "");
        SolrClientTool solrClientTool = solrService.getSolrClientTool();
        solrClientTool.setCollection("t2t");
        solrClientTool.setParser("lucene");
        solrClientTool.setDefaultField("seq");
        StringBuilder query = new StringBuilder();
        query.append("{!complexphrase inOrder=true}");
        for (int i = 0; i < 5; i++) {
            query.append("seq:\"").append(wildcard(seq, i)).append("\" OR ");
        }
        String cseq = complement(seq);
        query.append("seq:\"").append(wildcard(cseq,0)).append("\" OR ");
        query.append("seq:\"").append(wildcard(cseq,1)).append("\" OR ");
        query.append("seq:\"").append(wildcard(cseq,2)).append("\" OR ");
        query.append("seq:\"").append(wildcard(cseq,3)).append("\" OR ");
        query.append("seq:\"").append(wildcard(cseq,4)).append("\"");
        List<SequenceItem> result = null;

        try {
            SolrClientTool.ResultMap results = solrClientTool.find("t2t", query.toString(), true);
            SolrDocumentList sdl = results.getDocs();
            highlightingMap = results.getHighlightingMap();
            DocumentObjectBinder binder = new DocumentObjectBinder();
            result = binder.getBeans(SequenceItem.class, sdl);
            // if there is only a single result with possible redundant location
            if (result != null && result.size() > 0 && result.size() <= 2) {
                SequenceItem item = result.get(0);
                String sequence = item.getSeq().get(0).toLowerCase().replace(" ", "");
                if (sequence.contains(seq)) {
                    forward = true;
                    if (!reportEnd) position = item.getPosition().get(0) + sequence.indexOf(seq);
                    else position = item.getPosition().get(0) + sequence.indexOf(seq) + seq.length();
                }
                if (sequence.contains(cseq)) {
                    forward = false;
                    if (!reportEnd) position = item.getPosition().get(0) + sequence.indexOf(cseq) + cseq.length();
                    else position = item.getPosition().get(0) + sequence.indexOf(cseq);
                }
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
