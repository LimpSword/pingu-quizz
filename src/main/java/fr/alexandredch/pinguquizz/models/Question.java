package fr.alexandredch.pinguquizz.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Type type;

    private String image;
    private String originalImageName;
    private String question;
    private Difficulty difficulty;
    private int points;
    private int time;

    // For open questions and true/false questions
    private String answer;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Answer> answers;

    public static Question minimal(Question question) {
        Question minimal = new Question();
        minimal.setType(question.getType());
        minimal.setImage(question.getImage());
        minimal.setQuestion(question.getQuestion());
        minimal.setDifficulty(question.getDifficulty());
        minimal.setPoints(question.getPoints());
        minimal.setTime(question.getTime());
        minimal.setAnswers(question.getAnswers().stream().map(Answer::minimal).toList());
        return minimal;
    }

    public List<Answer> getCorrectAnswers() {
        if (type == Type.OPEN || type == Type.TRUE_FALSE) {
            return List.of(new Answer(-1L, true, answer, null, null));
        }
        return answers.stream().filter(Answer::isCorrect).toList();
    }

    public enum Type {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        OPEN
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }
}
