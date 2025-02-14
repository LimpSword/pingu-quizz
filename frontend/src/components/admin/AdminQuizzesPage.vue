<template>
  <div class="bg-gray-100 min-h-screen">
    <div class="p-8 container mx-auto">
      <h1 class="text-3xl font-bold text-gray-800 mb-8">Gestion des Salles</h1>

      <!-- Room Management Section -->
      <div class="mb-12 bg-white p-6 rounded-lg shadow">
        <div class="mb-4">
          <h3 class="text-xl font-medium text-gray-700">Créer une nouvelle salle</h3>
          <input v-model="newRoomName" type="text" placeholder="Nom de la salle"
                 class="border border-gray-300 rounded px-3 py-2 mr-2"/>
          <button @click="createRoom"
                  class="mt-2 bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-all duration-200">
            Créer une salle
          </button>
        </div>

        <h3 class="text-xl font-medium text-gray-700 mt-6">Salles actives</h3>
        <div class="bg-white rounded-lg shadow overflow-hidden">
          <div class="hidden md:block">
            <table class="min-w-full">
              <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Code
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Gérer
                </th>
              </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
              <tr v-for="room in activeRooms" :key="room.id">
                <td class="px-6 py-4 text-sm text-gray-900">{{ room.name }}</td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  {{ room.code }}
                  <button @click="copyRoomCode(room.code)"
                          class="copy-btn transition-all duration-200 hover:bg-gray-200 active:bg-gray-300 p-1 rounded">
                    <img src="/copy-svgrepo-com.svg" alt="Copy Icon" class="w-4 h-4 inline-block"/>
                  </button>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                    <span
                      :class="{ 'bg-green-100 text-green-800': room.started && !room.paused, 'bg-red-100 text-red-800': !room.started, 'bg-yellow-100 text-yellow-800': room.started && room.paused }"
                      class="px-2 py-1 rounded-full text-xs font-medium">
                      {{
                        room.started && !room.paused ? "En cours" : room.started ? "En pause" : "Non démarré"
                      }}
                    </span>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <router-link :to="'/admin/room/' + room.code">
                    <button class="text-blue-600 hover:text-blue-900">Gérer</button>
                  </router-link>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="md:hidden space-y-4">
            <div v-for="room in activeRooms" :key="room.id"
                 class="p-4 border rounded-lg bg-gray-50">
              <div class="flex">
                <div>
                  <p class="text-sm font-medium text-gray-900">Nom : {{ room.name }}</p>
                  <p class="text-sm text-gray-700">Code : {{ room.code }}</p>
                </div>
                <span
                  :class="{ 'bg-green-100 text-green-800': room.started && !room.paused, 'bg-red-100 text-red-800': !room.started, 'bg-yellow-100 text-yellow-800': room.started && room.paused }"
                  class="px-2 py-1 rounded-full text-xs font-medium max-h-fit ml-auto">
                {{
                    room.started && !room.paused ? "En cours" : room.started ? "En pause" : "Non démarré"
                  }}
              </span>
              </div>


              <router-link :to="'/admin/room/' + room.code"
                           class="block mt-2 text-blue-600 hover:text-blue-900">Gérer
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <h1 class="text-3xl font-bold text-gray-800 mb-8">Gestion des Quizz</h1>

      <!-- Quizzes Table -->
      <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nom</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Description
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
          <tr v-for="quiz in quizzes" :key="quiz.id">
            <td class="px-6 py-4 text-sm text-gray-900">{{ quiz.name }}</td>
            <td class="px-6 py-4 text-sm text-gray-700">{{ quiz.description }}</td>
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

    const copyRoomCode = (code) => {
      navigator.clipboard.writeText(code);
    };

    // Fetch quizzes
    const fetchQuizzes = async () => {
      try {
        quizzes.value = await (await fetcher("/quizz/all")).json();
      } catch (error) {
        console.error("Failed to fetch quizzes:", error);
      }
    };

    // Fetch active rooms
    const fetchRooms = async () => {
      try {
        rooms.value = await (await fetcher("/room/all")).json();
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

    // Active rooms
    const activeRooms = computed(() => rooms.value);

    onMounted(() => {
      fetchQuizzes();
      fetchRooms();
    });

    return {
      quizzes,
      showActiveOnly,
      rooms,
      newRoomName,
      activeRooms,
      copyRoomCode,
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
