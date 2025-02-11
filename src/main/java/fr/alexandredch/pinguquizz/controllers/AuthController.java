package fr.alexandredch.pinguquizz.controllers;

import fr.alexandredch.pinguquizz.config.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // Hardcoded user (replace with DB lookup)
        if ("user".equals(username) && "password".equals(password)) {
            Set<String> roles = Set.of("USER", "ADMIN");

            String token = jwtUtil.generateToken(username, roles);
            return Map.of("token", token, "username", username, "roles", roles.toString());
        } else {
            return Map.of("error", "Invalid credentials");
        }
    }
}