package org.mskcc.knowledge.MIND;

import java.util.Map;

public interface WordMIND {

    WordMIND load();
    Map<Long, String> getArticleTexts();

}
