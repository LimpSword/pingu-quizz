package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminRoomService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RoomRepository roomRepository;

    public AdminRoomService(SimpMessagingTemplate simpMessagingTemplate, RoomRepository roomRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.roomRepository = roomRepository;
    }

    public void updatePlayerList(String roomCode) {
        simpMessagingTemplate.convertAndSend("/topic/admin/room/" + roomCode + "/players",
                Map.of("type", "UPDATE",
                        "roomCode", roomCode,
                        "players", roomRepository.findByCode(roomCode).orElseThrow().getPlayers()));
    }
}
