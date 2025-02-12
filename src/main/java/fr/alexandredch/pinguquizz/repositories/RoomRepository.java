package fr.alexandredch.pinguquizz.repositories;

import fr.alexandredch.pinguquizz.models.QuizzRoom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_TRIES = 10;

    private final EntityManager entityManager;

    public RoomRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public Optional<String> generateUniqueCode() {
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

    public Optional<QuizzRoom> findByCode(String code) {
        return entityManager.createQuery("SELECT qr FROM QuizzRoom qr WHERE qr.code = :code", QuizzRoom.class)
                .setParameter("code", code).getResultList().stream().findFirst();
    }

    public void save(QuizzRoom room) {
        entityManager.getTransaction().begin();
        entityManager.persist(room);
        entityManager.getTransaction().commit();
    }

    public List<QuizzRoom> findAll() {
        return entityManager.createQuery("SELECT qr FROM QuizzRoom qr", QuizzRoom.class).getResultList();
    }
}
