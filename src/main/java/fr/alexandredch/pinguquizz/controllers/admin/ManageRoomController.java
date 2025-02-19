package fr.alexandredch.pinguquizz.controllers.admin;

import fr.alexandredch.pinguquizz.controllers.admin.action.EditRoomAction;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.models.User;
import fr.alexandredch.pinguquizz.repositories.QuizzRepository;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import fr.alexandredch.pinguquizz.services.QuizzRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/room")
public class ManageRoomController {

    private final QuizzRoomService quizzRoomService;
    private final RoomRepository roomRepository;
    private final QuizzRepository quizzRepository;

    public ManageRoomController(QuizzRoomService quizzRoomService, RoomRepository roomRepository, QuizzRepository quizzRepository) {
        this.quizzRoomService = quizzRoomService;
        this.roomRepository = roomRepository;
        this.quizzRepository = quizzRepository;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QuizzRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/info/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> currentRoom(@PathVariable String code) {
        Optional<QuizzRoom> room = roomRepository.findByCode(code);
        if (room.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room.get());
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
        System.out.println(editRoomAction);
        switch (editRoomAction.getAction()) {
            case PAUSE -> room.get().setPaused(true);
            case RESUME -> room.get().setPaused(false);
            case START -> {
                room.get().setStarted(true);
                quizzRoomService.startQuizz(room.get());
            }
            case CHANGE_QUIZZ -> {
                Quizz quizz = quizzRepository.findById(Long.valueOf(editRoomAction.getBody())).orElse(null);
                if (quizz == null) {
                    return ResponseEntity.badRequest().body("Quizz not found");
                }
                room.get().setQuizz(quizz);
                quizzRoomService.updateQuiz(room.get());
            }
        }
        System.out.println(room.get());
        roomRepository.save(room.get());
        return ResponseEntity.ok(room.get());
    }
}
