package org.mskcc.knowledge.MIND;

import org.mskcc.knowledge.model.Target;
import org.mskcc.knowledge.model.Variant;
import org.mskcc.knowledge.repository.TargetRepository;
import org.mskcc.knowledge.repository.VariantRepository;
import org.mskcc.knowledge.service.SolrService;
import org.mskcc.knowledge.service.TextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private final List<Target> targetsSorted = new ArrayList<>();  // sorted by symbol
    private Set<String> symbols = null;
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
