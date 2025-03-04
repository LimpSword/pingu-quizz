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
      <button @click="openQuizSelector" class="btn-primary" :disabled="isStarted">Changer de quizz</button>
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
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Joueurs connectés ({{ connectedPlayersCount }})</h2>
      <div v-if="players.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="player in players" :key="player.playerId" >
          <div v-if="player.connected" class="bg-gray-50 p-4 rounded-lg">
            <span class="text-gray-700 font-medium">{{ player.name }}</span>
            <div v-if="isStarted && playerPerformance[player.playerId]" class="mt-1 text-sm">
          <span class="inline-block px-2 py-1 bg-blue-100 text-blue-800 rounded">
            Score: {{ playerPerformance[player.playerId].score }}
          </span>
              <span class="inline-block px-2 py-1 ml-2 bg-green-100 text-green-800 rounded">
            {{ playerPerformance[player.playerId].correctAnswers }}/{{ playerPerformance[player.playerId].totalQuestions || currentQuestion?.index + 1 || 1 }} correctes
          </span>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="text-gray-500">Aucun joueur connecté</p>
    </div>

    <!-- Quiz Controls Section -->
    <div class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Contrôles du quizz</h2>
      <div class="flex gap-4">
        <button @click="togglePause" class="btn-primary" :disabled="!isStarted">
          {{ isPaused ? 'Reprendre' : 'Mettre en pause' }}
        </button>
        <button @click="startQuiz" v-if="!isStarted" class="btn-primary" :disabled="!currentQuiz">Commencer le quizz</button>
      </div>
    </div>

    <div v-if="isStarted" class="bg-white p-6 rounded-lg shadow mb-8">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-semibold text-gray-800">
          Question {{ currentQuestion?.index + 1 || 1 }}/{{ currentQuestion?.totalQuestions || '?' }}
        </h2>
        <span class="px-3 py-1 rounded-full"
              :class="isPaused ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800'">
      {{ isPaused ? 'En pause' : 'En cours' }}
    </span>
      </div>

      <div v-if="currentQuestion?.text" class="mb-4 p-4 bg-blue-50 rounded-lg">
        <p class="text-lg text-gray-800">{{ currentQuestion.text }}</p>
        <p v-if="currentQuestion.timeSeconds" class="text-sm text-gray-600 mt-2">
          Durée: {{ currentQuestion.timeSeconds }} secondes
        </p>
      </div>

      <!-- Response Progress -->
      <div class="mb-6">
        <div class="flex justify-between mb-2">
          <h3 class="text-lg font-medium text-gray-700">Progression des réponses</h3>
          <span class="text-blue-600 font-semibold">{{ Math.round(responsePercentage) }}%</span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-6">
          <div class="bg-blue-500 h-6 rounded-full transition-all duration-500"
               :style="{ width: responsePercentage + '%' }"></div>
        </div>
        <p class="text-gray-700 mt-2">{{ responseCount }} sur {{ players.length }} joueurs ont répondu</p>
      </div>
    </div>

    <!-- Quiz Statistics Section -->
    <div v-if="isStarted && correctRateByQuestion.length > 0" class="bg-white p-6 rounded-lg shadow mb-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Statistiques du quizz</h2>

      <!-- Graph of Correct Answers -->
      <div class="mb-6">
        <h3 class="text-lg font-medium text-gray-700 mb-2">Pourcentage de bonnes réponses par question</h3>
        <div v-if="correctRateByQuestion.length > 0" class="w-full h-64">
          <canvas ref="chartCanvas"></canvas>
        </div>
        <p v-else class="text-gray-500 italic">Pas encore de données disponibles</p>
      </div>

      <!-- Top Performers -->
      <div class="mb-6">
        <h3 class="text-lg font-medium text-gray-700 mb-2">Classement actuel</h3>
        <div v-if="topPerformers.length > 0" class="overflow-x-auto">
          <table class="min-w-full bg-white rounded-lg overflow-hidden">
            <thead class="bg-gray-100">
            <tr>
              <th class="py-2 px-4 text-left">#</th>
              <th class="py-2 px-4 text-left">Joueur</th>
              <th class="py-2 px-4 text-left">Score</th>
              <th class="py-2 px-4 text-left">Réponses correctes</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(player, index) in topPerformers" :key="player.id"
                class="border-t hover:bg-gray-50">
              <td class="py-2 px-4">{{ index + 1 }}</td>
              <td class="py-2 px-4 font-medium">{{ player.name }}</td>
              <td class="py-2 px-4">{{ player.score }}</td>
              <td class="py-2 px-4">{{ player.correctAnswers }}/{{ player.totalQuestions || player.answeredQuestions }}</td>
            </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="text-gray-500 italic">Aucun joueur n'a encore répondu</p>
      </div>
    </div>

    <!-- Quiz Results Modal -->
    <div v-if="showQuizResults" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white p-8 rounded-lg shadow-lg w-3/4 max-h-3/4 overflow-y-auto">
        <h2 class="text-2xl font-bold text-gray-800 mb-6">Résultats du Quizz</h2>

        <!-- Question Details Section -->
        <div class="mb-8">
          <h3 class="text-xl font-semibold text-gray-700 mb-4">Détails des questions</h3>
          <div class="space-y-4">
            <div v-for="(question, index) in finalResults.questionDetails" :key="index"
                 class="bg-white p-4 rounded-lg shadow">
              <h4 class="text-lg font-medium text-gray-800">
                Question {{ index + 1 }}: {{ question.text }}
              </h4>

              <div class="mt-2 text-sm">
                <p class="text-gray-600">
                  <span class="font-medium">Type:</span>
                  {{ question.type === 'MULTIPLE_CHOICE' ? 'Choix multiple' :
                  question.type === 'TRUE_FALSE' ? 'Vrai/Faux' : 'Ouverte' }}
                </p>

                <p class="text-gray-600">
                  <span class="font-medium">Taux de réussite:</span>
                  {{ Math.round(finalResults.questionDifficulty[index]) }}%
                </p>

                <div v-if="question.answerDistribution" class="mt-2">
                  <p class="font-medium text-gray-700">Distribution des réponses:</p>
                  <div class="ml-4 mt-1 space-y-1">
                    <div v-for="(count, answer) in question.answerDistribution" :key="answer"
                         class="flex items-center">
                      <span class="text-gray-800 mr-2">{{ formatAnswer(answer) }}:</span>
                      <span class="text-gray-600">{{ count }} réponses</span>
                      <span v-if="question.correctAnswers &&
                          question.correctAnswers.includes(formatAnswer(answer))"
                            class="ml-2 text-green-600 font-medium">(Correcte)</span>
                    </div>
                  </div>
                </div>

                <p v-if="question.mostCommonWrongAnswer" class="text-gray-600 mt-2">
                  <span class="font-medium">Erreur la plus commune:</span>
                  {{ question.mostCommonWrongAnswer }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Final Rankings -->
        <div class="mb-8">
          <h3 class="text-xl font-semibold text-gray-700 mb-4">Classement final</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- Top 3 Winners -->
            <div v-for="(player, index) in finalResults.rankings.slice(0, 3)" :key="player.id"
                 class="bg-gradient-to-br from-blue-50 to-indigo-50 p-6 rounded-lg text-center shadow">
              <div class="inline-flex items-center justify-center w-12 h-12 mb-4 rounded-full"
                   :class="[
                     index === 0 ? 'bg-yellow-100 text-yellow-800' :
                     index === 1 ? 'bg-gray-200 text-gray-800' :
                     'bg-amber-800 text-amber-100'
                   ]">
                <span class="text-xl font-bold">{{ index + 1 }}</span>
              </div>
              <h4 class="text-lg font-bold">{{ player.name }}</h4>
              <p class="text-2xl font-extrabold text-indigo-600 mt-2">{{ player.score }}</p>
              <p class="text-sm text-gray-600 mt-1">
                {{ player.correctAnswers }}/{{ player.answeredQuestions }} bonnes réponses
              </p>
            </div>
          </div>

          <!-- Rest of Players -->
          <div class="mt-6 overflow-x-auto">
            <table class="min-w-full bg-white rounded-lg overflow-hidden">
              <thead class="bg-gray-100">
              <tr>
                <th class="py-2 px-4 text-left">#</th>
                <th class="py-2 px-4 text-left">Joueur</th>
                <th class="py-2 px-4 text-left">Score</th>
                <th class="py-2 px-4 text-left">Réponses correctes</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(player, index) in finalResults.rankings.slice(3)" :key="player.id"
                  class="border-t hover:bg-gray-50">
                <td class="py-2 px-4">{{ index + 4 }}</td>
                <td class="py-2 px-4 font-medium">{{ player.name }}</td>
                <td class="py-2 px-4">{{ player.score }}</td>
                <td class="py-2 px-4">{{ player.correctAnswers }}/{{ player.answeredQuestions }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Question Difficulty Chart -->
        <div class="mb-8">
          <h3 class="text-xl font-semibold text-gray-700 mb-4">Difficulté des questions</h3>
          <div class="h-64">
            <canvas ref="difficultyChartCanvas"></canvas>
          </div>
        </div>

        <!-- General Stats -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <div class="bg-blue-50 p-4 rounded-lg text-center">
            <h4 class="text-lg font-medium text-gray-700">Score moyen</h4>
            <p class="text-2xl font-bold text-blue-600">{{ Math.round(finalResults.averageScore) }}</p>
          </div>
          <div class="bg-green-50 p-4 rounded-lg text-center">
            <h4 class="text-lg font-medium text-gray-700">Participants</h4>
            <p class="text-2xl font-bold text-green-600">{{ finalResults.rankings.length }}</p>
          </div>
          <div class="bg-purple-50 p-4 rounded-lg text-center">
            <h4 class="text-lg font-medium text-gray-700">Question la plus difficile</h4>
            <p class="text-2xl font-bold text-purple-600">
              Q{{ hardestQuestionIndex + 1 }}
              <span class="text-sm font-normal">
                ({{ Math.round(finalResults.questionDifficulty[hardestQuestionIndex]) }}% de réussite)
              </span>
            </p>
          </div>
        </div>

        <div class="flex justify-center">
          <button @click="closeQuizResults" class="btn-primary px-8 py-3">Fermer</button>
        </div>
      </div>
    </div>

    <!-- Quiz Selector Modal -->
    <div v-if="showQuizSelector"
         class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
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
import {ref, onMounted, watch, onUnmounted, computed} from 'vue';
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
    const difficultyChartCanvas = ref(null);
    let chartInstance = null;
    let difficultyChartInstance = null;
    const showQuizSelector = ref(false);
    const showQuizResults = ref(false);
    const availableQuizzes = ref([]);
    const selectedQuizId = ref(null);
    const answerDistribution = ref({});
    const correctRateByQuestion = ref([]);
    const playerPerformance = ref({});
    const finalResults = ref(null);
    const currentQuestion = ref(null);
    let stompClient = null;

    // Computed properties
    const connectedPlayersCount = computed(() => {
      return players.value.filter(p => p.connected).length;
    });

    const responseCount = computed(() => {
      return Math.round(responsePercentage.value * players.value.length / 100);
    });

    const topPerformers = computed(() => {
      if (!playerPerformance.value) return [];

      const performanceArray = Object.values(playerPerformance.value);
      return [...performanceArray].sort((a, b) => b.score - a.score).slice(0, 10);
    });

    const hardestQuestionIndex = computed(() => {
      if (!finalResults.value || !finalResults.value.questionDifficulty) return 0;

      let minIndex = 0;
      let minValue = 100;

      finalResults.value.questionDifficulty.forEach((rate, index) => {
        if (rate < minValue) {
          minValue = rate;
          minIndex = index;
        }
      });

      return minIndex;
    });

    const setupWebSocket = () => {
      const socket = new SockJS("http://localhost:8080/api/quiz");
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          console.log("Connected to WebSocket");

          // Update inside the setupWebSocket function in AdminRoomPage.vue
          stompClient.subscribe("/user/queue/admin/room", (message) => {
            try {
              const data = JSON.parse(message.body);
              console.log("Received WebSocket message:", data.type, data);

              if (data.type === "UPDATE") {
                // Update current player list
                players.value = data.players;

                if (data.percentageResponded !== undefined) {
                  responsePercentage.value = data.percentageResponded * 100;
                }

                if (data.currentQuestionIndex !== undefined) {
                  // Update current question index if provided
                  if (currentQuestion.value) {
                    currentQuestion.value.index = data.currentQuestionIndex;
                  }
                }

                if (data.answerDistribution) {
                  answerDistribution.value = data.answerDistribution;
                }

                if (data.correctRateByQuestion && data.correctRateByQuestion.length > 0) {
                  correctRateByQuestion.value = data.correctRateByQuestion;
                  console.log("Updating chart with data:", correctRateByQuestion.value);
                  updateChart();
                }

                if (data.playerPerformance) {
                  // Convert array to object keyed by player ID for easier lookup
                  playerPerformance.value = data.playerPerformance.reduce((acc, player) => {
                    acc[player.id] = player;
                    return acc;
                  }, {});
                }
              }
              else if (data.type === "QUIZ_START") {
                console.log("Quiz starting!");
                // Quiz has started
                isStarted.value = true;
                isPaused.value = false;
                currentQuestion.value = {
                  index: 0,
                  totalQuestions: data.questionCount,
                  text: null,
                  timeSeconds: null
                };

                // Reset analytics data
                answerDistribution.value = {};
                correctRateByQuestion.value = [];
                playerPerformance.value = {};
              }
              else if (data.type === "QUESTION_START") {
                console.log("New question starting:", data.currentQuestionIndex + 1);
                // New question has started
                currentQuestion.value = {
                  index: data.currentQuestionIndex,
                  totalQuestions: data.totalQuestions,
                  text: data.questionText,
                  timeSeconds: data.questionTime
                };

                // Reset the response percentage for the new question
                responsePercentage.value = 0;
                answerDistribution.value = {};
              }
              else if (data.type === "QUESTION_END") {
                console.log("Question ended:", data.currentQuestionIndex + 1);
                // Question has ended - show final stats for this question
                if (data.answerDistribution) {
                  answerDistribution.value = data.answerDistribution;
                }

                if (data.correctRateByQuestion) {
                  correctRateByQuestion.value = data.correctRateByQuestion;
                  console.log("Updating chart at question end:", correctRateByQuestion.value);
                  // Redraw chart with updated data
                  updateChart();
                }

                if (data.playerPerformance) {
                  playerPerformance.value = data.playerPerformance.reduce((acc, player) => {
                    acc[player.id] = player;
                    return acc;
                  }, {});
                }
              }
              else if (data.type === "QUIZ_COMPLETE") {
                console.log("Quiz completed!");
                // Mark quiz as no longer running
                isStarted.value = false;

                // Save and display final results
                finalResults.value = data.finalResults;
                showQuizResults.value = true;

                // Initialize the difficulty chart after a small delay to ensure DOM is ready
                setTimeout(() => {
                  initDifficultyChart();
                }, 300);
              }

              // For backward compatibility
              if (data.quizComplete && data.finalResults) {
                console.log("Quiz completed (legacy format)");
                isStarted.value = false;
                finalResults.value = data.finalResults;
                showQuizResults.value = true;
                setTimeout(() => {
                  initDifficultyChart();
                }, 300);
              }
            } catch (error) {
              console.error("Error processing WebSocket message:", error);
            }
          });

          stompClient.publish({
            destination: "/app/admin/join",
            body: JSON.stringify({ "roomCode": roomCode.value}),
          });
        },
      });
      stompClient.activate();
    };

    // Format answer string for display (remove brackets from array representation)
    const formatAnswer = (answer) => {
      return answer.replace(/[\[\]]/g, '').replace(/,/g, ', ');
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

    // Close quiz results modal
    const closeQuizResults = () => {
      showQuizResults.value = false;
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

      if (response.ok) {
        currentQuiz.value = availableQuizzes.value.find(q => q.id === selectedQuizId.value);
        closeQuizSelector();
      }
    };

    // Initialize chart

    const initChart = () => {
      if (!chartCanvas.value) {
        console.warn("Chart canvas not found");
        return;
      }

      // Destroy existing chart if any
      if (chartInstance) {
        try {
          chartInstance.destroy();
        } catch (e) {
          console.error("Error destroying chart:", e);
        }
        chartInstance = null;
      }

      try {
        const ctx = chartCanvas.value.getContext('2d');
        if (!ctx) {
          console.warn("Could not get 2D context for chart canvas");
          return;
        }

        const labels = correctRateByQuestion.value.map((_, index) => `Q${index + 1}`);

        chartInstance = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: '% de bonnes réponses',
              data: correctRateByQuestion.value,
              backgroundColor: 'rgba(59, 130, 246, 0.7)',
              borderColor: 'rgba(59, 130, 246, 1)',
              borderWidth: 1
            }],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'top',
              },
              title: {
                display: true,
                text: 'Taux de réussite par question'
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                max: 100,
                title: {
                  display: true,
                  text: 'Pourcentage de bonnes réponses'
                }
              },
              x: {
                title: {
                  display: true,
                  text: 'Questions'
                }
              }
            }
          },
        });

        console.log("Chart initialized successfully");
      } catch (error) {
        console.error("Error initializing chart:", error);
      }
    };

    // Update chart with new data
    const updateChart = () => {
      // Just re-initialize the chart with new data
      initChart();
    };


    // Initialize the difficulty chart for final results
    // Update the initDifficultyChart function with better error handling
    const initDifficultyChart = () => {
      if (!difficultyChartCanvas.value || !finalResults.value) {
        console.warn("Difficulty chart canvas or final results not available");
        return;
      }

      // Destroy existing chart if any
      if (difficultyChartInstance) {
        try {
          difficultyChartInstance.destroy();
        } catch (e) {
          console.error("Error destroying difficulty chart:", e);
        }
        difficultyChartInstance = null;
      }

      try {
        const ctx = difficultyChartCanvas.value.getContext('2d');
        if (!ctx) {
          console.warn("Could not get 2D context for difficulty chart canvas");
          return;
        }

        const labels = finalResults.value.questionDifficulty.map((_, index) => `Question ${index + 1}`);
        const data = finalResults.value.questionDifficulty;

        difficultyChartInstance = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Taux de réussite (%)',
              data: data,
              backgroundColor: data.map(value =>
                value < 40 ? 'rgba(239, 68, 68, 0.7)' :   // red for difficult questions
                  value < 70 ? 'rgba(245, 158, 11, 0.7)' :  // amber for medium
                    'rgba(16, 185, 129, 0.7)'                 // green for easy
              ),
              borderColor: data.map(value =>
                value < 40 ? 'rgb(220, 38, 38)' :        // darker red border
                  value < 70 ? 'rgb(217, 119, 6)' :        // darker amber border
                    'rgb(5, 150, 105)'                       // darker green border
              ),
              borderWidth: 1
            }],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'top',
              },
              title: {
                display: true,
                text: 'Difficulté des questions (% de réussite)'
              },
              tooltip: {
                callbacks: {
                  label: function(context) {
                    return `Taux de réussite: ${Math.round(context.raw)}%`;
                  }
                }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                max: 100,
                title: {
                  display: true,
                  text: 'Pourcentage de bonnes réponses'
                }
              }
            }
          }
        });

        console.log("Difficulty chart initialized successfully");
      } catch (error) {
        console.error("Error initializing difficulty chart:", error);
      }
    };

    // Watch for changes in quiz state
    watch(isStarted, (newVal) => {
      if (newVal) {
        initChart();
      }
    });

    // Watch for changes in correctRateByQuestion
    watch(correctRateByQuestion, () => {
      if (isStarted.value && correctRateByQuestion.value.length > 0) {
        updateChart();
      }
    });

    // Fetch data on mount
    onMounted(() => {
      fetchRoomData();
      setupWebSocket();
    });

    onUnmounted(() => {
      if (stompClient) stompClient.deactivate();

      if (chartInstance) {
        try {
          chartInstance.destroy();
        } catch (e) {
          console.error("Error destroying chart:", e);
        }
        chartInstance = null;
      }

      if (difficultyChartInstance) {
        try {
          difficultyChartInstance.destroy();
        } catch (e) {
          console.error("Error destroying difficulty chart:", e);
        }
        difficultyChartInstance = null;
      }
    });

    return {
      currentQuiz,
      roomCode,
      players,
      isPaused,
      isStarted,
      responsePercentage,
      chartCanvas,
      difficultyChartCanvas,
      showQuizSelector,
      showQuizResults,
      availableQuizzes,
      selectedQuizId,
      answerDistribution,
      correctRateByQuestion,
      playerPerformance,
      finalResults,
      connectedPlayersCount,
      responseCount,
      topPerformers,
      hardestQuestionIndex,
      currentQuestion,
      copyRoomCode,
      togglePause,
      startQuiz,
      openQuizSelector,
      closeQuizSelector,
      closeQuizResults,
      changeQuiz,
      formatAnswer,
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
  transition: all 0.2s;
}

.btn-primary:hover {
  background-color: #1e40af;
}

.btn-primary:disabled {
  background-color: #93c5fd;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #6b7280;
  color: white;
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background-color: #4b5563;
}
</style>
