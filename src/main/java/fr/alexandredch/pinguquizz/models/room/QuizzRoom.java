package fr.alexandredch.pinguquizz.models.room;

import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class QuizzRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Quizz quizz;
    @ManyToOne
    private User owner;

    private String name;
    private String code;

    @OneToMany(fetch = FetchType.EAGER)
    private List<RoomPlayer> players;

    private boolean paused = false;
    private boolean started = false;

    private int currentQuestion = 0;

    // QuizzRoom object with less sensitive data
    public static QuizzRoom minimal(QuizzRoom room) {
        QuizzRoom minimal = new QuizzRoom();
        minimal.setCode(room.getCode());
        minimal.setPaused(room.isPaused());
        minimal.setStarted(room.isStarted());
        minimal.setCurrentQuestion(room.getCurrentQuestion());
        return minimal;
    }

    public void answer(String playerId, List<String> answers) {
        Question currentQuestion = quizz.getQuestions().get(this.currentQuestion);

        // Check if all correct answers are in the answers list
        if (currentQuestion.getCorrectAnswers().stream().allMatch(answer -> answers.contains(answer.getAnswer()))) {
            RoomPlayer player = players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow();
            player.getAnswers().set(this.currentQuestion, true);
        }
    }
}
