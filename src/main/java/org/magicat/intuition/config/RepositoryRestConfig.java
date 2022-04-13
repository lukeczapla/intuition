package org.mskcc.knowledge.config;

import org.mskcc.knowledge.model.CancerMap;
import org.mskcc.knowledge.model.DrugMap;
import org.mskcc.knowledge.model.GeneMap;
import org.mskcc.knowledge.model.MutationMap;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class RepositoryRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(GeneMap.class, MutationMap.class, DrugMap.class, CancerMap.class);
    }

}