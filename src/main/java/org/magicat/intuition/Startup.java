package org.mskcc.knowledge;

import org.joda.time.DateTime;
import org.mskcc.knowledge.config.SwaggerConfig;
import org.mskcc.knowledge.config.WebSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableAutoConfiguration
@EnableMongoRepositories
@EnableMongoAuditing
@EnableScheduling
//@ComponentScan("org.mskcc.knowledge")
@Import({WebSecurityConfig.class, SwaggerConfig.class})
public class Startup {

    public static DateTime startTime;

    public static void main(String[] args) {
        startTime = DateTime.now();
        SpringApplication.run(Startup.class, args);
    }

}