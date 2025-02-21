<template>
  <div class="p-8 bg-gray-100 min-h-screen">
    <div class="flex mb-4">
      <button @click="router().push('/admin')" class="text-blue-600 hover:text-blue-900">
        < Retour
      </button>
      <h1 class="ml-5 text-3xl font-bold text-gray-800 ">Gestion de la salle de Quizz</h1>
    </div>

    <!-- Current Quiz Section -->
    <div class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Quizz actuel</h2>
      <div v-if="currentQuiz" class="flex items-center gap-4">
        <p class="text-gray-700">{{ currentQuiz.name }}</p>
      </div>
      <p v-else class="text-gray-500">Aucun quizz sélectionné</p>
      <button @click="openQuizSelector" class="btn-primary">Changer de quizz</button>
    </div>

    <!-- Room Code Section -->
    <div class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Code de la salle</h2>
      <div class="flex items-center gap-4">
        <span class="text-gray-700">{{ roomCode }}</span>
        <button
          class="copy-btn transition-all duration-200 hover:bg-gray-200 active:bg-gray-300 p-1 rounded"
          @click="copyRoomCode()"
        >
          <img src="/copy-svgrepo-com.svg" alt="Copy Icon" class="w-4 h-4 inline-block"/>
        </button>
      </div>
    </div>

    <!-- Players Section -->
    <div class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Joueurs connectés</h2>
      <div v-if="players.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="player in players" :key="player.id" >
          <div v-if="player.connected" class="bg-gray-50 p-4 rounded-lg">
            <span class="text-gray-700">{{ player.name }}</span> <span class="text-sm">{{player.playerId}}</span>
          </div>
        </div>
      </div>
      <p v-else class="text-gray-500">Aucun joueur connecté</p>
    </div>

    <!-- Quiz Controls Section -->
    <div class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Contrôles du quizz</h2>
      <div class="flex gap-4">
        <button @click="togglePause" class="btn-primary">
          {{ isPaused ? 'Reprendre' : 'Mettre en pause' }}
        </button>
        <button @click="startQuiz" v-if="!isStarted" class="btn-primary">Commencer le quizz</button>
      </div>
    </div>

    <!-- Quiz Statistics Section -->
    <div v-if="isStarted" class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Statistiques du quizz</h2>

      <!-- Current Question Response Percentage -->
      <div class="mb-6">
        <h3 class="text-lg font-medium text-gray-700 mb-2">Réponses à la question actuelle</h3>
        <div class="w-full bg-gray-200 rounded-full h-4">
          <div class="bg-blue-500 h-4 rounded-full"
               :style="{ width: responsePercentage + '%' }"></div>
        </div>
        <p class="text-gray-700 mt-2">{{ responsePercentage }}% des joueurs ont répondu</p>
      </div>

      <!-- Graph of Correct Answers -->
      <div class="mb-6">
        <h3 class="text-lg font-medium text-gray-700 mb-2">Pourcentage de bonnes réponses par
          question</h3>
        <div class="w-full h-64">
          <canvas ref="chartCanvas"></canvas>
        </div>
      </div>
    </div>

    <!-- Quiz Selector Modal -->
    <div v-if="showQuizSelector"
         class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
      <div class="bg-white p-6 rounded-lg shadow w-1/3">
        <h2 class="text-xl font-semibold text-gray-800 mb-4">Sélectionner un quizz</h2>
        <select v-model="selectedQuizId" class="input-field mb-4">
          <option v-for="quiz in availableQuizzes" :key="quiz.id" :value="quiz.id">{{
              quiz.name
            }}
          </option>
        </select>
        <div class="flex justify-end gap-4">
          <button @click="closeQuizSelector" class="btn-secondary">Annuler</button>
          <button @click="changeQuiz" class="btn-primary">Changer</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {ref, onMounted, watch, onUnmounted} from 'vue';
import {Chart, registerables} from 'chart.js';
import {fetcher} from "@/api/api.js";
import router from "@/router/index.js";
import {Client} from "@stomp/stompjs";

Chart.register(...registerables);

export default {
  methods: {
    router() {
      return router
    }
  },
  props: {
    code: String,
  },
  setup(props) {
    const currentQuiz = ref(null);
    const roomCode = ref(props.code);
    const players = ref([]);
    const isPaused = ref(false);
    const isStarted = ref(false);
    const responsePercentage = ref(0);
    const chartCanvas = ref(null);
    const chartInstance = ref(null);
    const showQuizSelector = ref(false);
    const availableQuizzes = ref([]);
    const selectedQuizId = ref(null);
    let stompClient = null;

    const setupWebSocket = () => {
        const socket = new SockJS("http://localhost:8080/api/quiz");
        stompClient = new Client({
          webSocketFactory: () => socket,
          reconnectDelay: 5000,
          onConnect: () => {
            console.log("Connected to WebSocket");

            // Subscribe to private messages for the player
            stompClient.subscribe("/user/queue/admin/room", (message) => {
              const data = JSON.parse(message.body);
              console.log("Received:", data);

              if (data.type === "UPDATE") {
                // Update current player list
                players.value = data.players;
                console.log(players.value)
                responsePercentage.value = data.percentageResponded * 100;
              }
            });

            // TODO: if we already have a playerId, we should send a join message
            stompClient.publish({
              destination: "/app/admin/join",
              body: JSON.stringify({ "roomCode": roomCode.value}),
            });
          },
        });
      stompClient.activate();
    };

    // Fetch initial data
    const fetchRoomData = async () => {
      const response = await fetcher('/room/info/' + roomCode.value);
      if (response.ok) {
        const data = await response.json();
        currentQuiz.value = data.quizz;
        players.value = data.players;
        isPaused.value = data.paused;
        isStarted.value = data.started;
      } else {
        // Go back to admin page if room is not found
        await router.push('/admin');
      }
    };

    // Copy room code to clipboard
    const copyRoomCode = () => {
      navigator.clipboard.writeText(roomCode.value);
    };

    // Toggle pause state
    const togglePause = async () => {
      const response = await fetcher('/room/edit', {
        method: 'POST',
        body: JSON.stringify({code: roomCode.value, action: isPaused.value ? "RESUME" : "PAUSE"}),
        headers: {"Content-Type": "application/json"},
      });
      if (response.ok) {
        isPaused.value = !isPaused.value;
      }
    };

    // Start the quiz
    const startQuiz = async () => {
      const response = await fetcher('/room/edit', {
        method: 'POST',
        body: JSON.stringify({code: roomCode.value, action: "START"}),
        headers: {"Content-Type": "application/json"},
      });
      if (response.ok) {
        isStarted.value = true;
      }
    };

    // Open quiz selector modal
    const openQuizSelector = async () => {
      const response = await fetcher('/quizz/all');
      if (response.ok) {
        availableQuizzes.value = await response.json();
        showQuizSelector.value = true;
      }
    };

    // Close quiz selector modal
    const closeQuizSelector = () => {
      showQuizSelector.value = false;
    };

    // Change the current quiz
    const changeQuiz = async () => {
      const response = await fetcher('/room/edit', {
        method: 'POST',
        body: JSON.stringify({
          code: roomCode.value,
          body: selectedQuizId.value,
          action: "CHANGE_QUIZZ"
        }),
        headers: {'Content-Type': 'application/json'},
      });

      console.log(response)

      if (response.ok) {
        currentQuiz.value = availableQuizzes.value.find(q => q.id === selectedQuizId.value);
        closeQuizSelector();
      }
    };

    // Initialize chart
    const initChart = () => {
      if (chartCanvas.value) {
        chartInstance.value = new Chart(chartCanvas.value, {
          type: 'bar',
          data: {
            labels: ['Q1', 'Q2', 'Q3', 'Q4', 'Q5'],
            datasets: [{
              label: '% de bonnes réponses',
              data: [75, 60, 80, 45, 90], // Replace with actual data
              backgroundColor: 'rgba(59, 130, 246, 0.5)',
            }],
          },
          options: {
            scales: {
              y: {
                beginAtZero: true,
                max: 100,
              },
            },
          },
        });
      }
    };

    // Watch for changes in quiz state
    watch(isStarted, (newVal) => {
      if (newVal) {
        initChart();
      }
    });

    // Fetch data on mount
    onMounted(() => {
      fetchRoomData();
    });

    onMounted(setupWebSocket);
    onUnmounted(() => {
      if (stompClient) stompClient.deactivate();
    });

    return {
      currentQuiz,
      roomCode,
      players,
      isPaused,
      isStarted,
      responsePercentage,
      chartCanvas,
      showQuizSelector,
      availableQuizzes,
      selectedQuizId,
      copyRoomCode,
      togglePause,
      startQuiz,
      openQuizSelector,
      closeQuizSelector,
      changeQuiz,
    };
  },
};
</script>

<style scoped>
.input-field {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 4px;
}

.btn-primary {
  background-color: #1d4ed8;
  color: white;
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary {
  background-color: #6b7280;
  color: white;
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
