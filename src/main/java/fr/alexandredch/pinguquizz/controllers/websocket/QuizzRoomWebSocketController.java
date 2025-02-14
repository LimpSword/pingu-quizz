package fr.alexandredch.pinguquizz.controllers.websocket;

import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import fr.alexandredch.pinguquizz.services.AdminRoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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
        System.out.println("payload: " + payload);
        String roomCode = payload.get("roomCode");
        Optional<QuizzRoom> room = roomRepository.findByCode(roomCode);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room not found");
        }
        String playerName = payload.get("playerName");
        System.out.println(room.get());
        String playerId = payload.getOrDefault("playerId", UUID.randomUUID().toString());
        if (!room.get().hasPlayer(playerId)) {
            System.out.println("add player with name " + playerName + " and id " + playerId);
            room.get().addPlayer(playerName, playerId);
        }
        System.out.println(room.get());
        sessionPlayerMap.put(sessionId, playerId);

        roomRepository.save(room.get());

        adminRoomService.updatePlayerList(room.get());
        if (room.get().getQuizz() == null) {
            return Map.of("type", "WAITING", "playerId", playerId);
        }
        return Map.of("type", "INFO", "quizz", Quizz.minimal(room.get().getQuizz()), "playerId", playerId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String playerId = sessionPlayerMap.remove(sessionId);

        if (playerId != null) {
            roomRepository.findAll().stream()
                    .filter(room -> room.hasPlayer(playerId))
                    .forEach(room -> {
                        room.removePlayer(playerId);
                        adminRoomService.updatePlayerList(room);
                    });
        }
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
