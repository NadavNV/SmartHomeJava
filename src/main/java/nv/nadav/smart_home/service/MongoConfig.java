package nv.nadav.smart_home.service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import nv.nadav.smart_home.config.MongoProperties;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private final MongoProperties mongoProperties;

    public MongoConfig(MongoProperties mongoProperties) {
        this.mongoProperties = mongoProperties;
    }

    private static String injectCredentialsIntoUri(String uri, String username, String password) throws URISyntaxException {
        URI parsed = new URI(uri);

        // Check if credentials are already present
        String userInfo = parsed.getUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            return uri;
        }

        String newUserInfo = username + ":" + password;

        URI newUri = new URI(
                parsed.getScheme(),
                newUserInfo + "@" + parsed.getHost() + (parsed.getPort() != -1 ? ":" + parsed.getPort() : ""),
                parsed.getPath(),
                parsed.getQuery(),
                parsed.getFragment()
        );

        return newUri.toString();
    }

    @Override
    @NonNull
    protected String getDatabaseName() {
        return mongoProperties.getDb();
    }

    @Override
    @Bean
    @NonNull
    public MongoClient mongoClient() {
        try {
            String uri = MongoConfig.injectCredentialsIntoUri(
                    mongoProperties.getUri(),
                    mongoProperties.getUser(),
                    mongoProperties.getPass()
            );
            return MongoClients.create(MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .build());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct MongoClient URI", e);
        }
    }
}
