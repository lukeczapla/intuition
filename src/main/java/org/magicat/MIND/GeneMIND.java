package org.magicat.MIND;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.magicat.model.SequenceItem;
import org.magicat.model.Target;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface GeneMIND {

    GeneMIND load();

    Set<String> getSymbols();
    Set<String> getKinases();

    List<SequenceItem> findSequence(String seq, boolean fuzzy);

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    class GeneResult {
        private int startPosition, endPosition;
        private boolean forward;
        private String chromosome;
    }

    GeneResult findGene(String startSeq, String endSeq);

    List<Target> getTargets();
    Map<String, Map<String, List<String>>> getHighlightingMap();

    void findNewAlterations();

    boolean isForward();
    Long getPosition();
    void setReportEnd(boolean reportEnd);

}
