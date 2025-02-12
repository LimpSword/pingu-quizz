package fr.alexandredch.pinguquizz.controllers;

import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class HomeController {

    private final RoomRepository roomRepository;

    public HomeController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody String code) {
        Optional<QuizzRoom> room = roomRepository.findByCode(code);
        if (room.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room.get());
    }
}
