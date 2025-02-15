package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.WebApplication;
import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.models.room.RoomPlayer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class QuizzRoomService {

    private static final String WS_DESTINATION = "/user/queue/quiz";

    private final SimpMessagingTemplate simpMessagingTemplate;

    public QuizzRoomService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendQuestion(QuizzRoom quizzRoom) {
        int currentQuestion = quizzRoom.getCurrentQuestion();
        if (currentQuestion >= quizzRoom.getQuizz().getQuestions().size()) {
            simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                    WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "END"), Map.class));
        } else {
            Question question = Question.minimal(quizzRoom.getQuizz().getQuestions().get(currentQuestion));
            simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                    WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "QUESTION", "question", question), Map.class));
        }
    }

    public void sendAnswer(QuizzRoom quizzRoom, String playerSessionId) {
        boolean correct = quizzRoom.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerSessionId))
                .findFirst()
                .orElseThrow()
                .getAnswers()
                .get(quizzRoom.getCurrentQuestion());

        simpMessagingTemplate.convertAndSend(playerSessionId, WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "RESULT", "correct", correct), Map.class));
    }

    public void endQuestion(QuizzRoom quizzRoom) {
        for (String playerSessionId : quizzRoom.getPlayers().stream().map(RoomPlayer::getPlayerId).toList()) {
            sendAnswer(quizzRoom, playerSessionId);
        }

        quizzRoom.setCurrentQuestion(quizzRoom.getCurrentQuestion() + 1);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendQuestion(quizzRoom);
            }
        }, 5000);
    }

    public void startQuizz(QuizzRoom quizzRoom) {
        simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "START"), Map.class));
        sendQuestion(quizzRoom);
    }
}
