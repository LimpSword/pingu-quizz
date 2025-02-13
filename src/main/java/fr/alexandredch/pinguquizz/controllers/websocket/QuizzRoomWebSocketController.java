package fr.alexandredch.pinguquizz.controllers.websocket;

import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import fr.alexandredch.pinguquizz.services.AdminRoomService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class QuizzRoomWebSocketController {

    private final RoomRepository roomRepository;
    private final AdminRoomService adminRoomService;
    private final Map<String, String> sessionPlayerMap = new ConcurrentHashMap<>();

    public QuizzRoomWebSocketController(RoomRepository roomRepository, AdminRoomService adminRoomService) {
        this.roomRepository = roomRepository;
        this.adminRoomService = adminRoomService;
    }

    @MessageMapping("/join")
    @SendTo("/user/queue/quiz")
    public Map<String, Object> joinRoom(@Payload Map<String, String> payload, @Header("simpSessionId") String sessionId) {
        System.out.println(payload);
        String roomCode = payload.get("roomCode");
        Optional<QuizzRoom> room = roomRepository.findByCode(roomCode);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room not found");
        }
        String playerName = payload.get("playerName");
        UUID playerId = UUID.randomUUID();
        sessionPlayerMap.put(sessionId, playerId.toString());

        room.get().addPlayer(playerName, playerId.toString());

        adminRoomService.updatePlayerList(roomCode);
        if (room.get().getQuizz() == null) {
            return Map.of("type", "WAITING", "playerId", playerId.toString());
        }
        return Map.of("type", "INFO", "quizz", Quizz.minimal(room.get().getQuizz()), "playerId", playerId.toString());
    }

    @MessageMapping("/answer")
    public void handleAnswer(@Payload Map<String, Object> received, @Header("simpSessionId") String sessionId) {
        String roomCode = (String) received.get("roomCode");
        List<String> answers = (List<String>) received.get("answers");
        String playerId = sessionPlayerMap.getOrDefault(sessionId, "Unknown Player");
        Optional<QuizzRoom> room = roomRepository.findByCode(roomCode);
        room.ifPresent(quizzRoom -> quizzRoom.answer(playerId, answers));
    }
}
