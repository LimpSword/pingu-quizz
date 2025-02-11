<template>
  <div
    class="min-h-screen bg-gradient-to-r from-blue-500 to-purple-600 flex flex-col items-center justify-center p-4">
    <!-- Quiz Container -->
    <div v-if="currentQuestion"
         class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center">
      <!-- Timer -->
      <div class="text-2xl font-bold text-gray-800 mb-4">
        Temps restant: {{ timer }}s
      </div>

      <!-- Question -->
      <h2 class="text-3xl font-bold text-gray-800 mb-6">{{ currentQuestion.question }}</h2>

      <!-- Question Image -->
      <img
        v-if="currentQuestion.image"
        :src="currentQuestion.image"
        alt="Question Image"
        class="mx-auto mb-6 rounded-lg shadow-md"
        style="max-height: 200px;"
      />

      <!-- Answers -->
      <div v-if="currentQuestion.type === 'MULTIPLE_CHOICE'" class="space-y-4">
        <button
          v-for="(answer, index) in currentQuestion.answers"
          :key="index"
          @click="submitAnswer(answer)"
          class="w-full bg-blue-100 text-gray-800 py-2 px-4 rounded-md hover:bg-blue-200 transition-all duration-200"
        >
          {{ answer.answer }}
          <img
            v-if="answer.image"
            :src="answer.image"
            alt="Answer Image"
            class="mx-auto mt-2 rounded-lg shadow-md"
            style="max-height: 100px;"
          />
        </button>
      </div>

      <!-- True/False Layout -->
      <div v-if="currentQuestion.type === 'TRUE_FALSE'" class="space-y-4">
        <button
          @click="submitAnswer({ answer: 'Vrai', correct: currentQuestion.answerKey === 'Vrai' })"
          class="w-full bg-green-100 text-gray-800 py-2 px-4 rounded-md hover:bg-green-200 transition-all duration-200"
        >
          Vrai
        </button>
        <button
          @click="submitAnswer({ answer: 'Faux', correct: currentQuestion.answerKey === 'Faux' })"
          class="w-full bg-red-100 text-gray-800 py-2 px-4 rounded-md hover:bg-red-200 transition-all duration-200"
        >
          Faux
        </button>
      </div>

      <!-- Open-Ended Layout -->
      <div v-if="currentQuestion.type === 'OPEN'" class="space-y-4">
        <input
          type="text"
          v-model="openAnswer"
          placeholder="Votre réponse"
          class="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          @click="submitAnswer({ answer: openAnswer, correct: openAnswer === currentQuestion.answerKey })"
          class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-all duration-200"
        >
          Soumettre
        </button>
      </div>
    </div>

    <!-- Quiz Completed -->
    <div v-else class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">Quiz Terminé!</h2>
      <p class="text-xl text-white mb-6">Votre score: {{ score }}/{{ quiz.questions.length }}</p>
      <button
        @click="restartQuiz"
        class="bg-white text-blue-600 py-2 px-4 rounded-md hover:bg-blue-100 transition-all duration-200"
      >
        Rejouer
      </button>
    </div>

    <!-- Confetti Animation -->
    <Confetti v-if="showConfetti"/>
  </div>
</template>

<script>
import {ref, onMounted, onUnmounted} from "vue";
import Confetti from "@/components/animations/Confetti.vue";

export default {
  components: {
    Confetti,
  },
  setup() {
    const quiz = ref({
      id: 1,
      name: "Sample Quiz",
      questions: [
        {
          id: 1,
          question: "Quelle est la capitale de la France?",
          type: "MULTIPLE_CHOICE",
          image: "",
          answerKey: "Paris",
          answers: [
            {answer: "Paris", correct: true, image: ""},
            {answer: "Lyon", correct: false, image: ""},
            {answer: "Marseille", correct: false, image: ""},
          ],
        },
        {
          id: 2,
          question: "2 + 2 = 4",
          type: "TRUE_FALSE",
          image: "",
          answerKey: "Vrai",
        },
        {
          id: 3,
          question: "Quel est le plus grand mammifère du monde?",
          type: "OPEN",
          image: "",
          answerKey: "La baleine bleue",
        },
      ],
    });

    const currentQuestionIndex = ref(0);
    const currentQuestion = ref(quiz.value.questions[0]);
    const timer = ref(10); // 10 seconds per question
    const score = ref(0);
    const openAnswer = ref("");
    const showConfetti = ref(false);
    let interval;

    // Start the timer
    const startTimer = () => {
      interval = setInterval(() => {
        if (timer.value > 0) {
          timer.value--;
        } else {
          nextQuestion();
        }
      }, 1000);
    };

    // Move to the next question
    const nextQuestion = () => {
      clearInterval(interval);
      if (currentQuestionIndex.value < quiz.value.questions.length - 1) {
        currentQuestionIndex.value++;
        currentQuestion.value = quiz.value.questions[currentQuestionIndex.value];
        timer.value = 10; // Reset timer
        startTimer();
      } else {
        currentQuestion.value = null; // End of quiz
      }
    };

    // Submit an answer
    const submitAnswer = (answer) => {
      if (answer.correct) {
        score.value++;
        showConfetti.value = true;
        setTimeout(() => (showConfetti.value = false), 2000); // Show confetti for 2 seconds
      }
      nextQuestion();
    };

    // Restart the quiz
    const restartQuiz = () => {
      currentQuestionIndex.value = 0;
      currentQuestion.value = quiz.value.questions[0];
      timer.value = 10;
      score.value = 0;
      startTimer();
    };

    // WebSocket for real-time updates
    const setupWebSocket = () => {
      const ws = new WebSocket("ws://localhost:8080/quiz");
      ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("WebSocket message:", data);
        // Handle real-time updates (e.g., leaderboard, new questions)
      };
    };

    onMounted(() => {
      startTimer();
      setupWebSocket();
    });

    onUnmounted(() => {
      clearInterval(interval);
    });

    return {
      quiz,
      currentQuestion,
      timer,
      score,
      openAnswer,
      showConfetti,
      submitAnswer,
      restartQuiz,
    };
  },
};
</script>

<style scoped>
/* Add custom styles if needed */
</style>
