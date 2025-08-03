package nv.nadav.smart_home.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;

    public String getSecret() {
        if (secret == null) {
            secret = System.getenv("JWT_SECRET_KEY");
        }
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
