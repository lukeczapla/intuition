package org.magicat.service;

import org.magicat.MIND.GeneMIND;
import org.magicat.model.SequenceItem;
import org.magicat.model.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SequencingServiceImpl implements SequencingService {

    private final GeneMIND geneMIND;

    @Autowired
    public SequencingServiceImpl(GeneMIND geneMIND) {
        this.geneMIND = geneMIND;
    }

    public Target findGene(String seq) {
        List<SequenceItem> items = geneMIND.findSequence(seq, true);
        if (items == null || items.size() == 0) return null;
        SequenceItem item = items.get(0);
        List<Target> targets = geneMIND.getTargets().parallelStream().filter(x -> x.getChromosome().equals(item.getChromosome().get(0)) && x.getStartPosition() <= item.getPosition().get(0) && x.getEndPosition() >= (item.getPosition().get(0) + seq.length())).collect(Collectors.toList());
        if (targets.size() == 0) return null;
        return targets.get(0);
    }

}
