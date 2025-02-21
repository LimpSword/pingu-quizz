package fr.alexandredch.pinguquizz.controllers.websocket;

import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import fr.alexandredch.pinguquizz.services.AdminRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class AdminRoomWebSocketController {

    private final RoomRepository roomRepository;
    private final AdminRoomService adminRoomService;
    private final Map<String, String> sessionAdminMap = new ConcurrentHashMap<>();

    public AdminRoomWebSocketController(RoomRepository roomRepository, AdminRoomService adminRoomService) {
        this.roomRepository = roomRepository;
        this.adminRoomService = adminRoomService;
    }

    @MessageMapping("/admin/join")
    @SendTo("/user/queue/admin/room")
    public ResponseEntity<?> joinRoom(@Payload Map<String, String> payload, @Header("simpSessionId") String sessionId) {
        String roomCode = payload.get("roomCode");
        Optional<QuizzRoom> room = roomRepository.findByCode(roomCode);
        if (room.isEmpty()) {
            return ResponseEntity.badRequest().body("Room not found");
        }
        UUID playerId = UUID.randomUUID();
        sessionAdminMap.put(sessionId, playerId.toString());

        adminRoomService.update(room.get());

        var response = Map.of("type", "INFO",
                "roomCode", roomCode,
                "adminId", playerId.toString()
        );
        return ResponseEntity.ok(response);
    }
}
