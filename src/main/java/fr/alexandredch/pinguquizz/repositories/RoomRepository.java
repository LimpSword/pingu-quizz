package fr.alexandredch.pinguquizz.repositories;

import fr.alexandredch.pinguquizz.models.QuizzRoom;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoomRepository {

    private final EntityManager entityManager;

    public RoomRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<QuizzRoom> findByCode(String code) {
        return entityManager.createQuery("SELECT qr FROM QuizzRoom qr WHERE qr.code = :code", QuizzRoom.class)
                .setParameter("code", code).getResultStream().findFirst();
    }

    public void save(QuizzRoom room) {
        entityManager.getTransaction().begin();
        entityManager.persist(room);
        entityManager.getTransaction().commit();
    }
}
