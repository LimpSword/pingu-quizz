<template>
  <div class="p-8 bg-gray-100 min-h-screen">
    <h1 class="text-3xl font-bold text-gray-800 mb-8">Gestion des Salles</h1>

    <!-- Room Management Section -->
    <div class="mb-12 bg-white p-6 rounded-lg shadow">
      <div class="mb-4">
        <h3 class="text-xl font-medium text-gray-700">Créer une nouvelle salle</h3>
        <input
          v-model="newRoomName"
          type="text"
          placeholder="Nom de la salle"
          class="border border-gray-300 rounded px-3 py-2 mr-2"
        />
        <button
          @click="createRoom"
          class="mt-2 bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-all duration-200"
        >
          Créer une salle
        </button>
      </div>

      <h3 class="text-xl font-medium text-gray-700 mt-6">Salles actives</h3>
      <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Code
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Gérer</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
          <tr v-for="room in activeRooms" :key="room.id">
            <td class="px-6 py-4 text-sm text-gray-900">{{ room.name }}</td>
            <td class="px-6 py-4 text-sm text-gray-700">{{ room.code }}</td>
            <td class="px-6 py-4 text-sm text-gray-700">
              <span
                :class="{
                  'bg-green-100 text-green-800': room.started && !room.paused,
                  'bg-red-100 text-red-800': !room.started,
                  'bg-yellow-100 text-yellow-800': room.started && room.paused,
                }"
                class="px-2 py-1 rounded-full text-xs font-medium"
              >
                {{ room.started && !room.paused ? "En cours" : room.started ? "En pause" : "Non démarré" }}
              </span>
            </td>
            <td class="px-6 py-4 text-sm text-gray-700">
              <button
                class="text-blue-600 hover:text-blue-900 mr-4"
              >
                Gérer
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <h1 class="text-3xl font-bold text-gray-800 mb-8">Gestion des Quizz</h1>

    <!-- Filter for Active Quizzes -->
    <div class="mb-6">
      <label class="flex items-center space-x-2">
        <input
          type="checkbox"
          v-model="showActiveOnly"
          class="form-checkbox h-5 w-5 text-blue-600"
        />
        <span class="text-gray-700">Afficher uniquement les quizz actifs</span>
      </label>
    </div>

    <!-- Quizzes Table -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full">
        <thead class="bg-gray-50">
        <tr>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
        </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
        <tr v-for="quiz in filteredQuizzes" :key="quiz.id">
          <td class="px-6 py-4 text-sm text-gray-900">{{ quiz.name }}</td>
          <td class="px-6 py-4 text-sm text-gray-700">{{ quiz.description }}</td>
          <td class="px-6 py-4 text-sm text-gray-700">
              <span
                :class="{
                  'bg-green-100 text-green-800': quiz.isActive,
                  'bg-red-100 text-red-800': !quiz.isActive,
                }"
                class="px-2 py-1 rounded-full text-xs font-medium"
              >
                {{ quiz.isActive ? "Actif" : "Inactif" }}
              </span>
          </td>
          <td class="px-6 py-4 text-sm text-gray-700">
            <button
              @click="editQuiz(quiz.id)"
              class="text-blue-600 hover:text-blue-900 mr-4"
            >
              Modifier
            </button>
            <button
              @click="deleteQuiz(quiz.id)"
              class="text-red-600 hover:text-red-900"
            >
              Supprimer
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Quiz Button -->
    <div class="mt-8">
      <router-link
        to="/admin/quizz/create"
        class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-all duration-200"
      >
        Créer un nouveau quizz
      </router-link>
    </div>
  </div>
</template>

<script>
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {fetcher} from "@/api/api.js";

export default {
  setup() {
    const router = useRouter();
    const quizzes = ref([]);
    const rooms = ref([]);
    const newRoomName = ref("");
    const showActiveOnly = ref(false);

    // Fetch quizzes
    const fetchQuizzes = async () => {
      try {
        quizzes.value = await fetcher("/quizz/all");
      } catch (error) {
        console.error("Failed to fetch quizzes:", error);
      }
    };

    // Fetch active rooms
    const fetchRooms = async () => {
      try {
        rooms.value = await fetcher("/room/all");
        console.log(rooms.value);
      } catch (error) {
        console.error("Failed to fetch rooms:", error);
      }
    };

    // Create a new room
    const createRoom = async () => {
      if (!newRoomName.value.trim()) return;
      try {
        await fetcher("/room/create", {
          method: "POST",
          body: JSON.stringify({name: newRoomName.value}),
          headers: {"Content-Type": "application/json"},
        });
        newRoomName.value = "";
        fetchRooms(); // Refresh room list
      } catch (error) {
        console.error("Failed to create room:", error);
      }
    };

    // Filter quizzes
    const filteredQuizzes = computed(() => {
      return showActiveOnly.value ? quizzes.value.filter(q => q.isActive) : quizzes.value;
    });

    // Active rooms
    const activeRooms = computed(() => rooms.value);

    onMounted(() => {
      fetchQuizzes();
      fetchRooms();
    });

    return {
      quizzes,
      showActiveOnly,
      filteredQuizzes,
      rooms,
      newRoomName,
      activeRooms,
      editQuiz: (id) => router.push(`/admin/quizzes/edit/${id}`),
      deleteQuiz: async (id) => {
        if (confirm("Êtes-vous sûr de vouloir supprimer ce quizz ?")) {
          try {
            await fetch(`/api/quizzes/${id}`, {method: "DELETE"});
            await fetchQuizzes();
          } catch (error) {
            console.error("Failed to delete quiz:", error);
          }
        }
      },
      createRoom,
    };
  },
};
</script>

<style scoped>
/* Add custom styles if needed */
</style>
