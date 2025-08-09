package nv.nadav.smart_home.controller;

import nv.nadav.smart_home.config.SecurityConfig;
import nv.nadav.smart_home.service.JwtService;
import nv.nadav.smart_home.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Import(SecurityConfig.class)
@RestController
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> json) {
        String username = json.get("username");
        String password = json.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }

        if (userService.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "User already exists"));
        }

        userService.createUser(username, password);
        String token = jwtService.generateToken(username, "user");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("access_token", token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> json) {
        String username = json.get("username");
        String password = json.get("password");

        if (!userService.verifyCredentials(username, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        String role = userService.getRole(username);
        String token = jwtService.generateToken(username, role);
        return ResponseEntity.ok(Map.of("access_token", token));
    }
}
