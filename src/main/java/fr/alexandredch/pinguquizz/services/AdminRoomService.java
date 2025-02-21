package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminRoomService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public AdminRoomService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void update(QuizzRoom quizzRoom) {
        float percentageResponded = quizzRoom.getPercentageResponded();
        simpMessagingTemplate.convertAndSend("/user/queue/admin/room",
                Map.of("type", "UPDATE",
                        "roomCode", quizzRoom.getCode(),
                        "players", quizzRoom.getPlayers(),
                        "percentageResponded", percentageResponded));
    }
}
