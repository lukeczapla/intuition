package org.magicat;

import org.joda.time.DateTime;
import org.magicat.config.SwaggerConfig;
import org.magicat.config.WebSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "org.magicat")
//@EnableAutoConfiguration
@EnableMongoRepositories(basePackages = "org.magicat.repository")
@EnableMongoAuditing
@EnableScheduling
@Import({WebSecurityConfig.class, SwaggerConfig.class})
public class Startup {

    public static DateTime startTime;

    public static void main(String[] args) {
        startTime = DateTime.now();
        SpringApplication.run(Startup.class, args);
    }

}