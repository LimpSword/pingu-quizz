package fr.alexandredch.pinguquizz.models.room;

import fr.alexandredch.pinguquizz.WebApplication;
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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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

    private transient TimerTask currentTask;

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

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            currentTask.cancel();
        } else {
            new Timer().schedule(currentTask = new java.util.TimerTask() {
                @Override
                public void run() {
                    sendQuestion();
                }
            }, 5000);
        }
    }

    public void answer(WebSocketSession session, List<String> answers) {
        Question currentQuestion = quizz.getQuestions().get(this.currentQuestion);

        // Check if all correct answers are in the answers list
        if (currentQuestion.getCorrectAnswers().stream().allMatch(answer -> answers.contains(answer.getAnswer()))) {
            RoomPlayer player = players.stream().filter(p -> p.getSession().equals(session)).findFirst().orElseThrow();
            player.getAnswers().set(this.currentQuestion, true);
        }
    }

    public void sendQuestion() {
        // Check if the quizz is finished
        if (currentQuestion >= quizz.getQuestions().size()) {
            players.forEach(player -> {
                try {
                    player.getSession().sendMessage(new TextMessage(WebApplication.OBJECT_MAPPER.writeValueAsString(Map.of("type", "END"))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        Question question = Question.minimal(quizz.getQuestions().get(currentQuestion));

        players.forEach(player -> {
            try {
                player.getSession().sendMessage(new TextMessage(WebApplication.OBJECT_MAPPER.writeValueAsString(Map.of("type", "QUESTION", "question", question))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendAnswer() {
        players.forEach(player -> {
            try {
                player.getSession().sendMessage(new TextMessage(WebApplication.OBJECT_MAPPER.writeValueAsString(Map.of("type", "RESULT", "correct", player.getAnswers().get(currentQuestion)))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void endQuestion() {
        sendAnswer();

        currentQuestion++;

        new Timer().schedule(currentTask = new java.util.TimerTask() {
            @Override
            public void run() {
                sendQuestion();
            }
        }, 5000);
    }

    public List<WebSocketSession> getSessions() {
        return players.stream().map(RoomPlayer::getSession).toList();
    }
}
