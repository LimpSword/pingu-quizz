package fr.alexandredch.pinguquizz.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.*;

public class QuizWebSocketHandler extends TextWebSocketHandler {

    private final RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<WebSocketSession, QuizzRoom> sessionQuizzRoom = new HashMap<>();

    public QuizWebSocketHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomCode = session.getUri().getQuery().split("=")[1];
        QuizzRoom room = roomRepository.findByCode(roomCode).orElseThrow();
        sessionQuizzRoom.put(session, room);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, String> received = objectMapper.readValue(message.getPayload(), Map.class);
        String type = received.get("type");

        if ("ANSWER".equals(type)) {
            QuizzRoom room = sessionQuizzRoom.get(session);
            List<String> sentAnswers = objectMapper.readValue(received.get("answers"), List.class);

            room.answer(session, sentAnswers);
        }
    }
}
