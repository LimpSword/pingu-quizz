package fr.alexandredch.pinguquizz.config;

import fr.alexandredch.pinguquizz.handlers.QuizWebSocketHandler;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomRepository roomRepository;

    public WebSocketConfig(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new QuizWebSocketHandler(roomRepository), "/quiz");
    }
}
