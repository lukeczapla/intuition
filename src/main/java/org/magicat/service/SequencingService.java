package org.magicat.service;

import org.magicat.model.Target;

public interface SequencingService {

    Target findGene(String seq);

}
