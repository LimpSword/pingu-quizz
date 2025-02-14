<template>
  <div
    class="min-h-screen bg-gradient-to-r from-blue-500 to-purple-600 flex flex-col items-center justify-center p-4">
    <div v-if="currentQuestion && started"
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

    <div v-if="!started" class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">
        Le Quiz va bientôt démarrer<span class="min-w-[1.5em] inline-block text-left">{{ dots}}</span>
      </h2>
      <div class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center opacity-85">
        <img src="https://placehold.co/150" alt="Quiz Image" class="mx-auto mb-4 rounded-lg shadow-md"/>
        <h3 class="text-2xl font-bold text-white mb-2">{{ quiz?.name }}</h3>
        <p class="text-lg text-white">{{ quiz?.description }}</p>
      </div>
    </div>

    <div v-if="!currentQuestion && started" class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">Quiz Terminé!</h2>
      <p class="text-xl text-white mb-6">Votre score: {{ score }}</p>
    </div>
  </div>
</template>

<script>
import { onMounted, onUnmounted, ref } from "vue";
import { Client } from "@stomp/stompjs";
import {useRoute} from "vue-router";

export default {
  props: {
    code: String,
  },
  setup(props) {
    const route = useRoute();

    const username = ref(route.query.username);
    const currentQuestion = ref(null);
    const roomCode = ref(props.code);
    const playerId = ref(null);
    const timer = ref(10);
    const score = ref(0);
    const started = ref(false);
    const paused = ref(false);
    const openAnswer = ref("");
    const dots = ref("");
    const quiz = ref(null);
    let stompClient = null;
    let interval;

    if (username.value === "") {
      username.value = "Anonyme";
    }

    const setupWebSocket = () => {
      const socket = new SockJS("http://localhost:8080/api/quiz");
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          console.log("Connected to WebSocket");

          // Subscribe to private messages for the player
          stompClient.subscribe("/user/queue/quiz", (message) => {
            const data = JSON.parse(message.body);
            console.log("Received:", data);

            if (data.type === "INFO") {
              quiz.value = data.quizz;
              playerId.value = data.playerId;
            } else if (data.type === "WAITING") {
              playerId.value = data.playerId;
            } else if (data.type === "QUESTION") {
              currentQuestion.value = data.question;
              timer.value = 10;
              startTimer();
            } else if (data.type === "RESULT") {
              if (data.correct) score.value++;
            }
          });

          // TODO: if we already have a playerId, we should send a join message
          stompClient.publish({
            destination: "/app/join",
            body: JSON.stringify({ "roomCode": roomCode.value, "playerName": username.value}),
          });
        },
      });

      stompClient.activate();
    };

    const startDotAnimation = () => {
      setInterval(() => {
        if (dots.value.length < 3) {
          dots.value += ".";
        } else {
          dots.value = "";
        }
      }, 500);
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
      if (stompClient && stompClient.connected) {
        stompClient.publish({
          destination: "/app/answer",
          body: JSON.stringify({ roomCode, answers: [answer] }),
        });
      }
    };

    onMounted(startDotAnimation);
    onMounted(setupWebSocket);
    onUnmounted(() => {
      clearInterval(interval);
      if (stompClient) stompClient.deactivate();
    });

    return { currentQuestion, timer, started, paused, score, dots, quiz, openAnswer, submitAnswer };
  },
};
</script>
