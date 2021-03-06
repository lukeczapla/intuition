package org.magicat.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Autowired
    private MappingMongoConverter mongoConverter;

    private final static String mongoUri = System.getenv("mongoUri");
    private final static String databaseName = "pmg_knowledge";

    private final static boolean overrideConnectionString = ConfigProperties.mongoOverride;
    private final static String mongoUri2 = "mongodb://192.168.1.86:27017/pmg_knowledge";
    private final static String databaseName2 = "pmg_knowledge";

    // pmg_knowledge for Production
    @NotNull
    @Override
    protected String getDatabaseName() {
        return overrideConnectionString ? databaseName2 : databaseName;
    }

    @Bean
    public GridFsTemplate gridFsTemplate() {
        return new GridFsTemplate(mongoDbFactory(), mongoConverter);
    }

    @NotNull
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(overrideConnectionString ? mongoUri2 : mongoUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }


    public MongoClient mongoClientUri(String uri) {
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    // opposite binding - useful for change stream with two MongoDB databases!  see Test1.java examples
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClientUri(overrideConnectionString ? mongoUri : mongoUri2), overrideConnectionString ? getDatabaseName() : databaseName2);
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        builder.applyConnectionString(new ConnectionString(mongoUri));
    }

}
