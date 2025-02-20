package fr.alexandredch.pinguquizz.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        System.out.println(attributes);

        // Try to extract the SockJS session id from the request URI.
        String path = request.getURI().getPath();
        String[] parts = path.split("/");
        if (parts.length > 2) {
            String candidate = parts[parts.length - 2];
            if (candidate.length() == 8) {
                return () -> candidate;
            }
        }
        // Fallback: use the HTTP session id if no SockJS session id is found.
        if (request instanceof ServletServerHttpRequest) {
            HttpSession session = ((ServletServerHttpRequest) request).getServletRequest().getSession();
            return session::getId;
        }
        return null;
    }
}
