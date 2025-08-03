package nv.nadav.smart_home.service;

public interface JwtService {
    String generateToken(String username, String role);
}
