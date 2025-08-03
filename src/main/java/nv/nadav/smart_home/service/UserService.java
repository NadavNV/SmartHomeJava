package nv.nadav.smart_home.service;

public interface UserService {
    void createUser(String username, String password);

    boolean verifyCredentials(String username, String password);

    boolean existsByUsername(String username);

    String getRole(String username);
}
