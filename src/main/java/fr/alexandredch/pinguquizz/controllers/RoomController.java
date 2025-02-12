package fr.alexandredch.pinguquizz.controllers;

import fr.alexandredch.pinguquizz.models.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/check/{code}")
    public ResponseEntity<?> checkRoom(@PathVariable String code) {
        Optional<QuizzRoom> room = roomRepository.findByCode(code);
        if (room.isEmpty()) {
            // No content
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.ok(Map.of("code", room.get().getCode()));
    }
}
