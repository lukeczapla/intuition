package org.magicat.MIND;

import org.magicat.model.SequenceItem;
import org.magicat.model.Target;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The MIND folder and GeneMIND interface and implementation class rely on large amounts of memory and serialization to work
 * on HPC and other computers with large memory totals (e.g. 256 GB RAM) and serialization of the results onto disk for reloading
 * the system.  GeneMIND only has 19648 genes and is one of the smallest of the MIND package.
 */
public interface GeneMIND {

    GeneMIND load();

    Set<String> getSymbols();
    Set<String> getKinases();

    List<SequenceItem> findSequence(String seq);

    List<Target> getTargets();
    Map<String, Map<String, List<String>>> getHighlightingMap();

    void findNewAlterations();

    boolean isForward();
    Long getPosition();
    void setReportEnd(boolean reportEnd);

}
