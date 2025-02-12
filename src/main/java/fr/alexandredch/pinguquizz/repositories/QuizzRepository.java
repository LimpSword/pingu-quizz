package fr.alexandredch.pinguquizz.repositories;

import fr.alexandredch.pinguquizz.models.Quizz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizzRepository extends JpaRepository<Quizz, Long> {
}
