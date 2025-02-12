package fr.alexandredch.pinguquizz.controllers.admin;

import fr.alexandredch.pinguquizz.controllers.admin.action.EditRoomAction;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.models.User;
import fr.alexandredch.pinguquizz.repositories.QuizzRepository;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/room")
public class ManageRoomController {

    private final RoomRepository roomRepository;
    private final QuizzRepository quizzRepository;

    public ManageRoomController(RoomRepository roomRepository, QuizzRepository quizzRepository) {
        this.roomRepository = roomRepository;
        this.quizzRepository = quizzRepository;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QuizzRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoom(@RequestBody String json) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        String name = json.split("\"name\":\"")[1].split("\"")[0];

        QuizzRoom room = new QuizzRoom();
        room.setName(name);
        room.setOwner(user);
        Optional<String> code = roomRepository.generateUniqueCode();
        if (code.isEmpty()) {
            return ResponseEntity.badRequest().body("Failed to generate a unique code");
        }
        room.setCode(code.get());

        roomRepository.save(room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editRoom(@RequestBody EditRoomAction editRoomAction) {
        Optional<QuizzRoom> room = roomRepository.findByCode(editRoomAction.getCode());
        if (room.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        switch (editRoomAction.getAction()) {
            case PAUSE -> room.get().setPaused(true);
            case RESUME -> room.get().setPaused(false);
            case START -> room.get().setStarted(true);
            case CHANGE_QUIZZ -> {
                Quizz quizz = quizzRepository.findById(Long.valueOf(editRoomAction.getBody())).orElse(null);
                if (quizz == null) {
                    return ResponseEntity.badRequest().body("Quizz not found");
                }
                room.get().setQuizz(quizz);
            }
        }
        roomRepository.save(room.get());
        return ResponseEntity.ok(room.get());
    }
}
