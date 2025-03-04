package fr.alexandredch.pinguquizz.services;

import fr.alexandredch.pinguquizz.models.Answer;
import fr.alexandredch.pinguquizz.models.Question;
import fr.alexandredch.pinguquizz.models.room.QuizzRoom;
import fr.alexandredch.pinguquizz.models.room.RoomAnswer;
import fr.alexandredch.pinguquizz.models.room.RoomPlayer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminRoomService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public AdminRoomService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void update(QuizzRoom quizzRoom) {
        float percentageResponded = quizzRoom.getPercentageResponded();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("type", "UPDATE");
        responseData.put("roomCode", quizzRoom.getCode());
        responseData.put("players", quizzRoom.getPlayers());
        responseData.put("percentageResponded", percentageResponded);
        responseData.put("currentQuestionIndex", quizzRoom.getCurrentQuestion());

        // Add analytics for current question if quiz is active
        if (quizzRoom.isStarted() && quizzRoom.getQuizz() != null &&
            quizzRoom.getCurrentQuestion() < quizzRoom.getQuizz().getQuestions().size()) {
            enrichWithQuizData(responseData, quizzRoom);
        }

        simpMessagingTemplate.convertAndSend("/user/queue/admin/room", responseData);
    }

    public void sendQuizStartUpdate(QuizzRoom quizzRoom) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "QUIZ_START");
        data.put("roomCode", quizzRoom.getCode());
        data.put("quizName", quizzRoom.getQuizz().getName());
        data.put("questionCount", quizzRoom.getQuizz().getQuestions().size());
        data.put("players", quizzRoom.getPlayers());

        simpMessagingTemplate.convertAndSend("/user/queue/admin/room", data);
    }

    public void sendQuestionStartUpdate(QuizzRoom quizzRoom) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "QUESTION_START");
        data.put("roomCode", quizzRoom.getCode());
        data.put("currentQuestionIndex", quizzRoom.getCurrentQuestion());
        data.put("totalQuestions", quizzRoom.getQuizz().getQuestions().size());

        // Add current question details (without answers)
        if (quizzRoom.getCurrentQuestion() < quizzRoom.getQuizz().getQuestions().size()) {
            Question currentQuestion = quizzRoom.getQuizz().getQuestions().get(quizzRoom.getCurrentQuestion());
            data.put("questionText", currentQuestion.getQuestion());
            data.put("questionTime", currentQuestion.getTime());
        }

        simpMessagingTemplate.convertAndSend("/user/queue/admin/room", data);

        // Also send a regular update
        update(quizzRoom);
    }

    public void sendQuestionEndUpdate(QuizzRoom quizzRoom) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "QUESTION_END");
        data.put("roomCode", quizzRoom.getCode());
        data.put("currentQuestionIndex", quizzRoom.getCurrentQuestion());

        // Calculate and add question stats
        enrichWithQuizData(data, quizzRoom);

        simpMessagingTemplate.convertAndSend("/user/queue/admin/room", data);
    }

    public void sendQuizCompleteUpdate(QuizzRoom quizzRoom) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "QUIZ_COMPLETE");
        data.put("roomCode", quizzRoom.getCode());
        data.put("quizName", quizzRoom.getQuizz().getName());

        // Set the quiz as explicitly completed
        data.put("quizComplete", true);

        // Add comprehensive final stats
        data.put("finalResults", getFinalResults(quizzRoom));

        System.out.println("Sending QUIZ_COMPLETE message to admin");
        simpMessagingTemplate.convertAndSend("/user/queue/admin/room", data);
    }

    private List<Float> getCorrectRateByQuestion(QuizzRoom quizzRoom) {
        List<Float> rates = new ArrayList<>();
        int questionCount = quizzRoom.getQuizz().getQuestions().size();
        int currentQuestionIndex = Math.min(quizzRoom.getCurrentQuestion(), questionCount - 1);

        // Only include questions that have been shown
        for (int i = 0; i <= currentQuestionIndex; i++) {
            int finalI = i;
            long answeredCount = quizzRoom.getPlayers().stream()
                    .filter(player -> player.getAnswers().size() > finalI &&
                                      player.getAnswers().get(finalI).isAnswered())
                    .count();

            if (answeredCount == 0) {
                rates.add(0.0f);
                continue;
            }

            long correctCount = quizzRoom.getPlayers().stream()
                    .filter(player -> player.getAnswers().size() > finalI &&
                                      player.getAnswers().get(finalI).isAnswered() &&
                                      player.getAnswers().get(finalI).isCorrect())
                    .count();

            rates.add((float) correctCount / answeredCount * 100);
        }

        return rates;
    }

    private void enrichWithQuizData(Map<String, Object> data, QuizzRoom quizzRoom) {
        int currentQuestionIndex = quizzRoom.getCurrentQuestion();

        // Get answer distribution
        Map<String, Integer> answerDistribution = getAnswerDistribution(quizzRoom, currentQuestionIndex);
        data.put("answerDistribution", answerDistribution);

        // Calculate correct answer rate for all questions answered so far
        List<Float> correctRateByQuestion = getCorrectRateByQuestion(quizzRoom);
        data.put("correctRateByQuestion", correctRateByQuestion);

        // Get player performance data
        List<Map<String, Object>> playerPerformance = getPlayerPerformance(quizzRoom);
        data.put("playerPerformance", playerPerformance);
    }

    private Map<String, Integer> getAnswerDistribution(QuizzRoom quizzRoom, int questionIndex) {
        Map<String, Integer> distribution = new HashMap<>();

        quizzRoom.getPlayers().stream()
                .filter(RoomPlayer::isConnected)
                .filter(player -> player.getAnswers().size() > questionIndex)
                .filter(player -> player.getAnswers().get(questionIndex).isAnswered())
                .forEach(player -> {
                    String answer = player.getAnswers().get(questionIndex).getAnswer();
                    distribution.put(answer, distribution.getOrDefault(answer, 0) + 1);
                });

        return distribution;
    }

    private List<Map<String, Object>> getPlayerPerformance(QuizzRoom quizzRoom) {
        return quizzRoom.getPlayers().stream()
                .filter(RoomPlayer::isConnected)
                .map(player -> {
                    Map<String, Object> performance = new HashMap<>();
                    performance.put("name", player.getName());
                    performance.put("id", player.getPlayerId());

                    // Count questions that the player has actually answered
                    long answeredQuestions = player.getAnswers().stream()
                            .filter(RoomAnswer::isAnswered)
                            .count();

                    // Count correct answers
                    long correctAnswers = player.getAnswers().stream()
                            .filter(RoomAnswer::isAnswered)
                            .filter(RoomAnswer::isCorrect)
                            .count();

                    // Make sure we always report at least the current question number as total
                    // even if player hasn't answered yet
                    int totalQuestions = Math.max(
                            (int) answeredQuestions,
                            quizzRoom.getCurrentQuestion() + 1
                    );

                    performance.put("correctAnswers", correctAnswers);
                    performance.put("answeredQuestions", answeredQuestions);
                    performance.put("totalQuestions", totalQuestions);
                    performance.put("score", correctAnswers * 100);

                    return performance;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> getFinalResults(QuizzRoom quizzRoom) {
        Map<String, Object> results = new HashMap<>();

        // Top performers
        List<Map<String, Object>> playerScores = getPlayerPerformance(quizzRoom);
        playerScores.sort((a, b) -> ((Integer) b.get("score")).compareTo((Integer) a.get("score")));
        results.put("rankings", playerScores);

        // Most difficult questions
        List<Float> correctRateByQuestion = getCorrectRateByQuestion(quizzRoom);
        results.put("questionDifficulty", correctRateByQuestion);

        // Average score
        double avgScore = playerScores.stream()
                .mapToLong(p -> (Long) p.get("score"))
                .average()
                .orElse(0.0);
        results.put("averageScore", avgScore);

        // Question details
        List<Map<String, Object>> questionDetails = new ArrayList<>();
        List<Question> questions = quizzRoom.getQuizz().getQuestions();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Map<String, Object> qDetails = new HashMap<>();
            qDetails.put("text", q.getQuestion());
            qDetails.put("type", q.getType());

            // Calculate the most common wrong answer if available
            if (q.getType() == Question.Type.MULTIPLE_CHOICE) {
                Map<String, Integer> answerDistribution = getAnswerDistribution(quizzRoom, i);
                qDetails.put("answerDistribution", answerDistribution);

                // Find correct answers
                List<String> correctAnswers = q.getCorrectAnswers().stream()
                        .map(Answer::getAnswer)
                        .collect(Collectors.toList());
                qDetails.put("correctAnswers", correctAnswers);

                // Find most common wrong answer if any
                String mostCommonWrongAnswer = null;
                int maxCount = 0;

                for (Map.Entry<String, Integer> entry : answerDistribution.entrySet()) {
                    String answer = entry.getKey().replace("[", "").replace("]", "");
                    if (!correctAnswers.contains(answer) && entry.getValue() > maxCount) {
                        mostCommonWrongAnswer = answer;
                        maxCount = entry.getValue();
                    }
                }

                qDetails.put("mostCommonWrongAnswer", mostCommonWrongAnswer);
            }

            // Add question details to the list
            questionDetails.add(qDetails);
        }

        results.put("questionDetails", questionDetails);

        // Time statistics - not implemented yet but could show average time taken per question

        return results;
    }
}