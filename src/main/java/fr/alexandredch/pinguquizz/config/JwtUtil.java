package fr.alexandredch.pinguquizz.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "your_secret_key_which_should_be_long";

    private final Logger logger = Logger.getLogger(JwtUtil.class.getName());

    public String generateToken(String username, Set<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean hasRole(AbstractAuthenticationToken authentication, String... roles) {
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        logger.info("Authentication: " + authentication);
        for (String role : roles) {
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String token, String... roles) {
        Claims claims = extractAllClaims(token);
        Set<?> claimsRoles = claims.get("roles", Set.class);
        for (String role : roles) {
            if (claimsRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String token, String role) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", Set.class).contains(role);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
}
