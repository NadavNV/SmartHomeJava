package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.exception.UserAlreadyExistsException;
import nv.nadav.smart_home.model.User;
import nv.nadav.smart_home.repository.UserRepository;
import nv.nadav.smart_home.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repo;
    private final SCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository repo, SCryptPasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void createUser(String username, String password) {
        if (!repo.existsByUsername(username)) {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPasswordHash(passwordEncoder.encode(password));
            newUser.setRole("user");
            repo.insert(newUser);
        } else {
            throw new UserAlreadyExistsException(String.format("Username %s is taken", username));
        }
    }

    @Override
    public boolean verifyCredentials(String username, String password) {
        User user = repo.findByUsername(username).orElse(null);
        if (user != null) {
            return passwordEncoder.matches(password, user.getPasswordHash());
        }
        return false;
    }

    @Override
    public boolean existsByUsername(String username) {
        return repo.existsByUsername(username);
    }

    @Override
    public String getRole(String username) {
        User user = repo.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException(String.format("Username %s not found", username)));
        return user.getRole();
    }
}
