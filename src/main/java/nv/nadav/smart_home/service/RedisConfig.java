package nv.nadav.smart_home.service;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import nv.nadav.smart_home.config.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @Bean
    public RedisClient redisClient(RedisProperties props) {
        String redisUri = String.format("redis://%s:%s@%s:%d",
                redisProperties.getUser(),
                redisProperties.getPass(),
                redisProperties.getHost(),
                redisProperties.getPort()
        );
        return RedisClient.create(redisUri);
    }

    @Bean
    public StatefulRedisConnection<String, String> redisConnection(RedisClient client) {
        return client.connect();
    }
}
