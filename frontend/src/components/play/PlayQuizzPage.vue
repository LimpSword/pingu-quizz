<template>
  <div
    class="min-h-screen bg-gradient-to-r from-blue-500 to-purple-600 flex flex-col items-center justify-center p-4">
    <div v-if="currentQuestion && started"
         class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center">
      <div class="text-2xl font-bold text-gray-800 mb-4">
        Temps restant: {{ timer }}s
      </div>

      <h2 class="text-3xl font-bold text-gray-800 mb-6">{{ currentQuestion.question }}</h2>

      <img v-if="currentQuestion.image" :src="`${imageUrl}/${currentQuestion.image}`" alt="Question Image"
           class="mx-auto mb-6 rounded-lg shadow-md" style="max-height: 200px;"/>

      <div v-if="currentQuestion.type === 'MULTIPLE_CHOICE'" class="space-y-4">
        <button v-for="(answer, index) in currentQuestion.answers" :key="index"
                @click="submitAnswer(answer.answer)"
                :disabled="responded"
                :class="{
                  'w-full bg-blue-100 text-gray-800 py-2 px-4 rounded-md hover:bg-blue-200': !responded,
                  'w-full bg-blue-300 text-gray-800 py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer !== answer.answer,
                  'w-full bg-green-300 text-gray-800 py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer === answer.answer
                }">
          {{ answer.answer }}
          <img v-if="answer.image" :src="`${imageUrl}/${answer.image}`" class="mx-auto mt-2 rounded-lg shadow-md"
               style="max-height: 100px;"/>
        </button>
      </div>

      <div v-if="currentQuestion.type === 'TRUE_FALSE'" class="space-y-4">
        <button @click="submitAnswer('Vrai')"
                :disabled="responded"
                :class="{
                  'w-full bg-green-100 text-gray-800 py-2 px-4 rounded-md hover:bg-green-200': !responded,
                  'w-full bg-green-300 text-gray-800 py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer !== 'Vrai',
                  'w-full bg-green-500 text-white py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer === 'Vrai'
                }">
          Vrai
        </button>
        <button @click="submitAnswer('Faux')"
                :disabled="responded"
                :class="{
                  'w-full bg-red-100 text-gray-800 py-2 px-4 rounded-md hover:bg-red-200': !responded,
                  'w-full bg-red-300 text-gray-800 py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer !== 'Faux',
                  'w-full bg-red-500 text-white py-2 px-4 rounded-md cursor-not-allowed': responded && submittedAnswer === 'Faux'
                }">
          Faux
        </button>
      </div>

      <div v-if="currentQuestion.type === 'OPEN'" class="space-y-4">
        <input v-model="openAnswer" type="text" placeholder="Votre réponse"
               :disabled="responded"
               class="w-full px-4 py-2 border border-gray-300 rounded-md"/>
        <button @click="submitAnswer(openAnswer)"
                :disabled="responded"
                class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700">
          Soumettre
        </button>
      </div>

      <div v-if="showResult" class="mt-6">
        <p class="text-2xl font-bold" :class="resultClass">{{ resultMessage }}</p>
      </div>
    </div>

    <div v-if="!started" class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">
        Le Quiz va bientôt démarrer<span
        class="min-w-[1.5em] inline-block text-left">{{ dots }}</span>
      </h2>
      <div class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center opacity-85">
        <img :src="`${imageUrl}/${quiz?.image}`" alt="Quiz Image"
             class="mx-auto mb-4 rounded-lg shadow-md"/>
        <h3 class="text-2xl font-bold mb-2">{{ quiz?.name }}</h3>
        <p class="text-lg">{{ quiz?.description }}</p>
      </div>
    </div>

    <div v-if="!currentQuestion && started" class="text-center">
      <h2 class="text-4xl font-bold text-white mb-4">Quiz Terminé!</h2>
      <p class="text-xl text-white mb-6">Votre score: {{ score }}</p>
    </div>

    <div class="fixed top-4 right-4 bg-white p-4 rounded-lg shadow-lg">
      <p class="text-xl font-bold">Score: {{ score }}</p>
    </div>
  </div>
</template>

<script>
import {onMounted, onUnmounted, ref} from "vue";
import {Client} from "@stomp/stompjs";
import {useRoute, useRouter} from "vue-router";
import JSConfetti from 'js-confetti';
import {apiUrls} from "@/api/api.js";

export default {
  props: {
    code: String,
  },
  setup(props) {
    const route = useRoute();
    const router = useRouter();
    const jsConfetti = new JSConfetti();

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
    const showResult = ref(false);
    const resultMessage = ref("");
    const resultClass = ref("");
    const isCorrect = ref(false);
    const responded = ref(false);
    const submittedAnswer = ref("");
    const imageUrl = apiUrls.download
    let stompClient = null;
    let interval;

    if (username.value === "") {
      username.value = "Anonyme";
    }

    const loadStoredPlayerId = () => {
      const storedData = localStorage.getItem(`quiz_player_${roomCode.value}`);
      if (storedData) {
        const data = JSON.parse(storedData);
        playerId.value = data.playerId;
        username.value = data.username;
        return true;
      }
      return false;
    };

    const savePlayerData = (pid) => {
      localStorage.setItem(`quiz_player_${roomCode.value}`, JSON.stringify({
        playerId: pid,
        username: username.value
      }));
    };

    const setupWebSocket = () => {
      const socket = new SockJS(apiUrls.websocket);
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          console.log("Connected to WebSocket");
          const urlarray = socket._transport.url.split("/");
          const index = urlarray.length - 2;
          console.log("session id", urlarray[index]);

          stompClient.subscribe("/queue/quiz-user" + urlarray[index], (message) => {
            const data = JSON.parse(message.body);
            if (data.type === "RESULT") {
              if (data.correct) {
                score.value++;
                resultMessage.value = "Correct!";
                resultClass.value = "text-green-500";
                isCorrect.value = true;
                jsConfetti.addConfetti();
              } else {
                resultMessage.value = "Incorrect!";
                resultClass.value = "text-red-500";
                isCorrect.value = false;
              }
              showResult.value = true;
              setTimeout(() => {
                showResult.value = false;
              }, 3000);
            }
          });

          stompClient.subscribe("/user/queue/quiz", (message) => {
            const data = JSON.parse(message.body);
            console.log("Received:", data);

            if (data.type === "ERROR") {
              console.error("Error:", data.error);
              router.push("/");
              return;
            }

            if (data.type === "INFO") {
              quiz.value = data.quizz;
              if (data.playerId) {
                playerId.value = data.playerId;
                savePlayerData(data.playerId);
              }
            } else if (data.type === "WAITING") {
              if (data.playerId) {
                playerId.value = data.playerId;
                savePlayerData(data.playerId);
              }
            } else if (data.type === "START") {
              started.value = true;
            } else if (data.type === "QUESTION") {
              currentQuestion.value = data.question;
              responded.value = false;
              submittedAnswer.value = "";
              timer.value = data.question.time;
              startTimer();
            } else if (data.type === "END") {
              currentQuestion.value = null;
              timer.value = 0;
              clearInterval(interval);
            }
          });

          stompClient.publish({
            destination: "/app/join",
            body: JSON.stringify({
              "roomCode": roomCode.value,
              "playerName": username.value,
              "playerId": playerId.value
            }),
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
        responded.value = true;
        submittedAnswer.value = answer;
        stompClient.publish({
          destination: "/app/answer",
          body: JSON.stringify({"roomCode": roomCode.value, "answers": [answer]}),
        });
      }
    };

    onMounted(() => {
      loadStoredPlayerId();
      startDotAnimation();
      setupWebSocket();
    })
    onUnmounted(() => {
      clearInterval(interval);
      if (stompClient) stompClient.deactivate();
    });

    return {currentQuestion, timer, started, paused, score, dots, quiz, openAnswer, submitAnswer, showResult, resultMessage, resultClass, isCorrect, responded, submittedAnswer, imageUrl};
  },
};
</script>
