package nv.nadav.smart_home.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import nv.nadav.smart_home.config.JwtConfig;
import nv.nadav.smart_home.service.JwtService;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtServiceImpl implements JwtService {
    private final JwtConfig jwtConfig;
    private Key signingKey;

    public JwtServiceImpl(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    @Override
    public String generateToken(String username, String role) {
        return generateToken(username, Map.of("role", role));
    }

    @Override
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public String generateToken(String username, Map<String, Object> additionalClaims) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 43200000)) // 12 hours
                .addClaims(additionalClaims)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
