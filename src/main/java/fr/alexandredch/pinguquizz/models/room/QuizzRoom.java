package fr.alexandredch.pinguquizz.models.room;

import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.User;
import jakarta.persistence.CascadeType;
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

import java.util.ArrayList;
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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

    public void setQuizz(Quizz quizz) {
        this.quizz = quizz;

        for (RoomPlayer player : players) {
            List<RoomAnswer> answers = new ArrayList<>();
            for (int i = 0; i < quizz.getQuestions().size(); i++) {
                answers.add(RoomAnswer.byDefault());
            }
            player.setAnswers(answers);
            System.out.println(player);
        }
    }

    public boolean hasPlayer(String playerId) {
        return players.stream().anyMatch(p -> p.getPlayerId() != null && p.getPlayerId().equals(playerId));
    }

    public RoomPlayer addPlayer(String playerName, String playerId, String sessionId) {
        RoomPlayer player = new RoomPlayer();
        player.setPlayerId(playerId);
        player.setSessionId(sessionId);
        player.setName(playerName);
        if (quizz != null) {
            List<RoomAnswer> answers = new ArrayList<>();
            for (int i = 0; i < quizz.getQuestions().size(); i++) {
                answers.add(RoomAnswer.byDefault());
            }
            player.setAnswers(answers);
        }

        players.add(player);
        return player;
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getPlayerId().equals(playerId));
    }

    public boolean answer(String playerId, List<String> answers) {
        Question currentQuestion = quizz.getQuestions().get(this.currentQuestion);

        // Has the player already answered the question?
        if (players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow().getAnswers().get(this.currentQuestion).isAnswered()) {
            return false;
        }

        RoomPlayer player = players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow();
        RoomAnswer roomAnswer = player.getAnswers().get(this.currentQuestion);
        roomAnswer.setAnswer(answers.toString());

        // Check if all correct answers are in the answers list
        if (currentQuestion.getCorrectAnswers().stream().allMatch(answer -> answers.contains(answer.getAnswer()))) {
            System.out.println(playerId);
            System.out.println(players);

            roomAnswer.setAnswered(true);
            roomAnswer.setCorrect(true);
            return true;
        }

        roomAnswer.setAnswered(true);
        roomAnswer.setCorrect(false);

        return true;
    }
}
