// src/main/java/fr/alexandredch/pinguquizz/services/QuizzRoomService.java
package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.WebApplication;
import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.Quizz;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.models.room.RoomAnswer;
import fr.alexandredch.pinguquizz.models.room.RoomPlayer;
import fr.alexandredch.pinguquizz.repositories.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class QuizzRoomService {

    private static final String WS_DESTINATION = "/user/queue/quiz";

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AdminRoomService adminRoomService;

    public QuizzRoomService(RoomRepository roomRepository, SimpMessagingTemplate simpMessagingTemplate, AdminRoomService adminRoomService) {
        this.roomRepository = roomRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.adminRoomService = adminRoomService;
    }

    public void sendQuestion(QuizzRoom quizzRoom) {
        int currentQuestion = quizzRoom.getCurrentQuestion();
        System.out.println("Current Question: " + currentQuestion + ", Total Questions: " + quizzRoom.getQuizz().getQuestions().size());

        // Check if we've reached the end of questions
        if (currentQuestion >= quizzRoom.getQuizz().getQuestions().size()) {
            System.out.println("End of quiz reached - sending END message");

            // Send END message to players
            simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                    WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "END"), Map.class));

            // IMPORTANT: Send quiz complete update to admin BEFORE modifying room state
            adminRoomService.sendQuizCompleteUpdate(quizzRoom);

            // Reset room state - mark it explicitly as not started
            quizzRoom.setStarted(false);
            quizzRoom.setCurrentQuestion(0);
            quizzRoom.setArchived(true);
            quizzRoom.setCompletedAt(LocalDateTime.now());
            roomRepository.save(quizzRoom);

            System.out.println("Quiz room reset: started=" + quizzRoom.isStarted());
            return; // Important to exit here!
        }

        // Send next question to players
        Question question = Question.minimal(quizzRoom.getQuizz().getQuestions().get(currentQuestion));
        simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "QUESTION", "question", question), Map.class));

        // Update admin that a new question has started
        adminRoomService.sendQuestionStartUpdate(quizzRoom);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                QuizzRoom updatedRoom = roomRepository.findById(quizzRoom.getId()).orElseThrow();
                endQuestion(updatedRoom);
            }
        }, question.getTime() * 1000L);
    }

    public void sendAnswer(QuizzRoom quizzRoom, String playerId, String playerSessionId, int currentQuestion) {
        if (playerSessionId == null) return;

        // Find the player's answer for the current question
        boolean correct = false;
        try {
            RoomPlayer player = quizzRoom.getPlayers().stream()
                    .filter(p -> p.getPlayerId().equals(playerId))
                    .findFirst()
                    .orElseThrow();

            System.out.println(player);

            RoomAnswer roomAnswer = player.getAnswers().get(currentQuestion);
            correct = roomAnswer.isCorrect();

            System.out.println("Player " + playerId + " answer for question " + currentQuestion + " is " +
                               (correct ? "correct" : "incorrect"));
        } catch (Exception e) {
            System.err.println("Error getting player answer: " + e.getMessage());
        }

        // Send result to the player
        var message = WebApplication.OBJECT_MAPPER.convertValue(
                Map.of("type", "RESULT", "correct", correct),
                Map.class
        );
        simpMessagingTemplate.convertAndSendToUser(playerSessionId, "/queue/quiz", message);
    }

    public void endQuestion(QuizzRoom quizzRoom) {
        // Send results to each player
        for (int i = 0; i < quizzRoom.getPlayers().size(); i++) {
            sendAnswer(quizzRoom, quizzRoom.getPlayers().get(i).getPlayerId(),
                    quizzRoom.getPlayers().get(i).getSessionId(), quizzRoom.getCurrentQuestion());
        }

        // Update admin with the question results before moving to next question
        adminRoomService.sendQuestionEndUpdate(quizzRoom);

        // Move to next question
        int nextQuestionIndex = quizzRoom.getCurrentQuestion() + 1;
        quizzRoom.setCurrentQuestion(nextQuestionIndex);

        // Save the updated state
        roomRepository.save(quizzRoom);

        System.out.println("Moving to next question: " + nextQuestionIndex);

        // Check if this was the last question
        if (nextQuestionIndex >= quizzRoom.getQuizz().getQuestions().size()) {
            System.out.println("Last question finished - ending quiz soon");

            // Schedule ending the quiz
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendQuestion(quizzRoom); // This will trigger the quiz ending logic
                }
            }, 5000L);
        } else {
            // If not the last question, schedule the next question as usual
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendQuestion(quizzRoom);
                }
            }, 5000L);
        }
    }

    public void startQuizz(QuizzRoom quizzRoom) {
        // Notify players that quiz is starting
        simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "START"), Map.class));

        // Notify admins that quiz is starting
        adminRoomService.sendQuizStartUpdate(quizzRoom);

        sendQuestion(quizzRoom);
    }

    public void updateQuiz(QuizzRoom quizzRoom) {
        simpMessagingTemplate.convertAndSend(WS_DESTINATION,
                WebApplication.OBJECT_MAPPER.convertValue(Map.of("type", "INFO", "quizz", Quizz.minimal(quizzRoom.getQuizz())), Map.class));

        // Notify admin about quiz update
        adminRoomService.update(quizzRoom);
    }
}