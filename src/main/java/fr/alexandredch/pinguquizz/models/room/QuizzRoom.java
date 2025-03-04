package fr.alexandredch.pinguquizz.models.room;

import fr.alexandredch.pinguquizz.models.Answer;
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

    public void updatePlayerSession(String playerId, String newSessionId) {
        players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(player -> player.setSessionId(newSessionId));
    }

    public void removePlayer(String playerId) {
        players.stream().filter(p -> p.getPlayerId().equals(playerId)).forEach(roomPlayer -> {
            roomPlayer.setSessionId(null);
        });
    }

    public RoomAnswer answer(String playerId, List<String> answers) {
        Question currentQuestion = quizz.getQuestions().get(this.currentQuestion);

        // Has the player already answered the question?
        if (players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow()
                .getAnswers().get(this.currentQuestion).isAnswered()) {
            return null;
        }

        RoomPlayer player = players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow();
        RoomAnswer roomAnswer = player.getAnswers().get(this.currentQuestion);
        roomAnswer.setAnswer(answers.toString());
        roomAnswer.setAnswered(true);

        // Debug output to see what's happening
        System.out.println("Question type: " + currentQuestion.getType());
        System.out.println("Player answer: " + answers);
        System.out.println("Question answers: " + currentQuestion.getAnswers());
        System.out.println("Expected answers: " + currentQuestion.getCorrectAnswers().stream()
                .map(Answer::getAnswer).toList());

        // Special handling for TRUE_FALSE questions
        if (currentQuestion.getType() == Question.Type.TRUE_FALSE) {
            // Convert player's "Vrai"/"Faux" to "True"/"False"
            String normalizedAnswer = answers.getFirst();
            if ("Vrai".equalsIgnoreCase(normalizedAnswer)) {
                normalizedAnswer = "True";
            } else if ("Faux".equalsIgnoreCase(normalizedAnswer)) {
                normalizedAnswer = "False";
            }

            // Check if normalized answer matches any of the correct answers
            String finalNormalizedAnswer = normalizedAnswer;
            boolean isCorrect = currentQuestion.getCorrectAnswers().stream()
                    .anyMatch(a -> a.getAnswer().equalsIgnoreCase(finalNormalizedAnswer));

            System.out.println("Normalized answer: " + normalizedAnswer + ", isCorrect: " + isCorrect);
            roomAnswer.setCorrect(isCorrect);

            player.getAnswers().set(this.currentQuestion, roomAnswer);
            return roomAnswer;
        }

        // For other question types, use the existing logic
        if (currentQuestion.getCorrectAnswers().stream().allMatch(answer -> answers.contains(answer.getAnswer()))) {
            roomAnswer.setCorrect(true);

            player.getAnswers().set(this.currentQuestion, roomAnswer);
            return roomAnswer;
        }

        roomAnswer.setCorrect(false);
        player.getAnswers().set(this.currentQuestion, roomAnswer);
        return roomAnswer;
    }

    public float getPercentageResponded() {
        if (quizz == null || players.isEmpty()) {
            return 0;
        }

        // If we're at the end of the quiz, return 100% (all players have responded)
        if (this.currentQuestion >= quizz.getQuestions().size()) {
            return 1.0f;
        }

        // Count how many players have answered the current question
        long playersAnswered = players.stream()
                .filter(p -> p.getAnswers().size() > this.currentQuestion &&
                             p.getAnswers().get(this.currentQuestion).isAnswered())
                .count();

        return (float) playersAnswered / players.size();
    }
}
