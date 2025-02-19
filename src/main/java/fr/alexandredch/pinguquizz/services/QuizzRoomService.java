package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.WebApplication;
import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class QuizzRoomService {

    private static final String WS_DESTINATION = "/user/queue/quiz";

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public QuizzRoomService(RoomRepository roomRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.roomRepository = roomRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendQuestion(QuizzRoom quizzRoom) {
        int currentQuestion = quizzRoom.getCurrentQuestion();
        System.out.println(quizzRoom.getCurrentQuestion() + " " + quizzRoom.getQuizz().getQuestions().size());
        if (currentQuestion >= quizzRoom.getQuizz().getQuestions().size()) {
            simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                    WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "END"), Map.class));

            quizzRoom.setStarted(false);
            quizzRoom.setCurrentQuestion(0);
            quizzRoom.getPlayers().clear();

            roomRepository.save(quizzRoom);
        } else {
            Question question = Question.minimal(quizzRoom.getQuizz().getQuestions().get(currentQuestion));
            simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                    WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "QUESTION", "question", question), Map.class));

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    endQuestion(quizzRoom);
                }
            }, 10000);
        }
    }

    public void sendAnswer(QuizzRoom quizzRoom, String playerId, String playerSessionId) {
        boolean correct = quizzRoom.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow()
                .getAnswers()
                .get(quizzRoom.getCurrentQuestion()).isCorrect();

        System.out.println("send answer " + correct + " to " + playerSessionId);

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(playerSessionId);
        headerAccessor.setLeaveMutable(true);

        simpMessagingTemplate.convertAndSendToUser(playerSessionId, "/queue/quiz",
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "RESULT", "correct", correct), Map.class), headerAccessor.getMessageHeaders());
    }

    public void endQuestion(QuizzRoom quizzRoom) {
        for (int i = 0; i < quizzRoom.getPlayers().size(); i++) {
            sendAnswer(quizzRoom, quizzRoom.getPlayers().get(i).getPlayerId(), quizzRoom.getPlayers().get(i).getSessionId());
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

    public void updateQuiz(QuizzRoom quizzRoom) {
        simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "INFO", "quizz", Quizz.minimal(quizzRoom.getQuizz())), Map.class));
    }
}
