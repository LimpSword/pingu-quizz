package fr.alexandredch.pinguquizz.repositories;

import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<QuizzRoom, Long> {

    String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    int MAX_TRIES = 10;

    default Optional<String> generateUniqueCode() {
        // Code is a combination of 6 uppercase letters or digits
        StringBuilder code = new StringBuilder();
        int tries = 0;
        do {
            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARACTERS.charAt((int) (Math.random() * CODE_CHARACTERS.length())));
            }
        } while (findByCode(code.toString()).isPresent() && tries++ < MAX_TRIES);
        return Optional.of(code.toString());
    }

    Optional<QuizzRoom> findByCode(String code);
}
