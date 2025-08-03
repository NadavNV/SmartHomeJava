package nv.nadav.smart_home.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public SCryptPasswordEncoder passwordEncoder() {
        return new SCryptPasswordEncoder(
                32768, // CPU cost (N)
                8,     // Memory cost (r)
                1,     // Parallelization (p)
                32,    // Key length
                64     // Salt length
        );
    }
}
