<template>
  <div
    class="min-h-screen bg-gradient-to-r from-blue-500 to-purple-600 flex flex-col items-center justify-center p-4">
    <div v-if="currentQuestion"
         class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center">
      <div class="text-2xl font-bold text-gray-800 mb-4">
        Temps restant: {{ timer }}s
      </div>

      <h2 class="text-3xl font-bold text-gray-800 mb-6">{{ currentQuestion.question }}</h2>

      <img v-if="currentQuestion.image" :src="currentQuestion.image" alt="Question Image"
           class="mx-auto mb-6 rounded-lg shadow-md" style="max-height: 200px;"/>

      <div v-if="currentQuestion.type === 'MULTIPLE_CHOICE'" class="space-y-4">
        <button v-for="(answer, index) in currentQuestion.answers" :key="index"
                @click="submitAnswer(answer.answer)"
                class="w-full bg-blue-100 text-gray-800 py-2 px-4 rounded-md hover:bg-blue-200">
          {{ answer.answer }}
          <img v-if="answer.image" :src="answer.image" class="mx-auto mt-2 rounded-lg shadow-md"
               style="max-height: 100px;"/>
        </button>
      </div>

      <div v-if="currentQuestion.type === 'TRUE_FALSE'" class="space-y-4">
        <button @click="submitAnswer('Vrai')"
                class="w-full bg-green-100 text-gray-800 py-2 px-4 rounded-md hover:bg-green-200">
          Vrai
        </button>
        <button @click="submitAnswer('Faux')"
                class="w-full bg-red-100 text-gray-800 py-2 px-4 rounded-md hover:bg-red-200">
          Faux
        </button>
      </div>

      <div v-if="currentQuestion.type === 'OPEN'" class="space-y-4">
        <input v-model="openAnswer" type="text" placeholder="Votre réponse"
               class="w-full px-4 py-2 border border-gray-300 rounded-md"/>
        <button @click="submitAnswer(openAnswer)"
                class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700">
          Soumettre
        </button>
      </div>
    </div>

    <div v-else class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">Quiz Terminé!</h2>
      <p class="text-xl text-white mb-6">Votre score: {{ score }}</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from "vue";

export default {
  setup() {
    const currentQuestion = ref(null);
    const timer = ref(10);
    const score = ref(0);
    const openAnswer = ref("");
    let ws;
    let interval;

    const setupWebSocket = () => {
      ws = new WebSocket("ws://localhost:8080/quiz");

      ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === "QUESTION") {
          currentQuestion.value = data.question;
          timer.value = 10;
          startTimer();
        } else if (data.type === "RESULT") {
          if (data.correct) score.value++;
        }
      };
    };

    const startTimer = () => {
      clearInterval(interval);
      interval = setInterval(() => {
        if (timer.value > 0) {
          timer.value--;
        }
      }, 1000);
    };

    const submitAnswer = (answer) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({type: "ANSWER", answer}));
      }
    };

    onMounted(setupWebSocket);
    onUnmounted(() => {
      clearInterval(interval);
      ws.close();
    });

    return {currentQuestion, timer, score, openAnswer, submitAnswer};
  },
};
</script>
