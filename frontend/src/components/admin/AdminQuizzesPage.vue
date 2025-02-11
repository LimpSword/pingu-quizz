<template>
  <div class="p-8 bg-gray-100 min-h-screen">
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
    const showActiveOnly = ref(false);

    // Fetch quizzes from the backend
    const fetchQuizzes = async () => {
      try {
        quizzes.value = await fetcher("/quizz/all");
      } catch (error) {
        console.error("Failed to fetch quizzes:", error);
      }
    };

    // Filter quizzes based on active status
    const filteredQuizzes = computed(() => {
      return showActiveOnly.value
        ? quizzes.value.filter((quiz) => quiz.isActive)
        : quizzes.value;
    });

    // Edit a quiz
    const editQuiz = (quizId) => {
      router.push(`/admin/quizzes/edit/${quizId}`);
    };

    // Delete a quiz
    const deleteQuiz = async (quizId) => {
      if (confirm("Êtes-vous sûr de vouloir supprimer ce quizz ?")) {
        try {
          await fetch(`/api/quizzes/${quizId}`, {method: "DELETE"});
          await fetchQuizzes(); // Refresh the list
        } catch (error) {
          console.error("Failed to delete quiz:", error);
        }
      }
    };

    onMounted(() => {
      fetchQuizzes();
    });

    return {
      quizzes,
      showActiveOnly,
      filteredQuizzes,
      editQuiz,
      deleteQuiz,
    };
  },
};
</script>

<style scoped>
/* Add custom styles if needed */
</style>
