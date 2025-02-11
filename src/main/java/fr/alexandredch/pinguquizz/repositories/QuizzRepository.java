package fr.alexandredch.pinguquizz.repositories;

import fr.alexandredch.pinguquizz.models.Quizz;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuizzRepository {

    private final EntityManager entityManager;

    public QuizzRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public List<Quizz> findAll() {
        return entityManager.createQuery("SELECT q FROM Quizz q", Quizz.class).getResultList();
    }

    public Optional<Quizz> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Quizz.class, id));
    }

    public void save(Quizz quizz) {
        entityManager.getTransaction().begin();
        entityManager.persist(quizz);
        entityManager.getTransaction().commit();
    }
}
