<template>
  <div
    id="home"
    class="min-h-screen flex flex-col items-center justify-center bg-cover bg-center bg-no-repeat"
    :style="{ backgroundImage: 'url(/background.jpg)' }"
  >
    <!-- Main content container -->
    <div
      class="relative bg-white p-8 rounded-lg shadow-lg w-full max-w-md transform transition-all duration-300 ease-in-out hover:scale-105 animate-fade-in"
    >
      <h1 class="text-3xl font-bold text-center text-gray-800 mb-6">
        Bienvenue sur le Pingu Quizz !
      </h1>

      <!-- Quiz Code Input -->
      <div class="mb-6">
        <label for="quizCode" class="block text-sm font-medium text-gray-700">
          Code de la salle
        </label>
        <input
          type="text"
          id="quizCode"
          v-model="quizCode"
          placeholder="ABC123"
          class="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200"
        />
      </div>

      <!-- Username Input (if not connected) -->
      <div v-if="!isConnected" class="mb-6">
        <label for="username" class="block text-sm font-medium text-gray-700">
          Nom d'utilisateur
        </label>
        <input
          type="text"
          id="username"
          v-model="username"
          placeholder="Matho"
          class="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200"
        />
      </div>

      <!-- Error Message -->
      <div
        v-if="errorMessage"
        class="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-md animate-fade-in"
      >
        {{ errorMessage }}
      </div>

      <!-- Join Quiz Button -->
      <button
        @click="joinQuiz"
        :disabled="isLoading"
        class="w-full bg-gradient-to-r from-blue-600 to-blue-500 text-white py-2 px-4 rounded-md hover:from-blue-700 hover:to-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all duration-200 flex items-center justify-center"
      >
        <span v-if="!isLoading">Rejoindre le Quizz</span>
        <svg
          v-if="isLoading"
          class="animate-spin h-5 w-5 text-white"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          ></circle>
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          ></path>
        </svg>
      </button>

      <!-- Admin Panel Button -->
      <button
        v-if="isAdmin"
        @click="admin"
        class="mt-4 w-full bg-gradient-to-r from-red-600 to-red-500 text-white py-2 px-4 rounded-md hover:from-red-700 hover:to-red-600 focus:outline-none focus:ring-2 focus:ring-red-500 transition-all duration-200"
      >
        Panel administrateur
      </button>

      <!-- Login Button -->
      <button
        v-if="!isConnected"
        @click="login"
        class="mt-4 w-full bg-gradient-to-r from-green-600 to-green-500 text-white py-2 px-4 rounded-md hover:from-green-700 hover:to-green-600 focus:outline-none focus:ring-2 focus:ring-green-500 transition-all duration-200"
      >
        Se connecter
      </button>
    </div>
  </div>
</template>

<script>
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";
import { ref } from "vue";

export default {
  setup() {
    const authStore = useAuthStore();
    const router = useRouter();

    const isAdmin = authStore.isAdmin;
    const isConnected = authStore.isConnected;
    const quizCode = ref("");
    const username = ref("");
    const errorMessage = ref("");
    const isLoading = ref(false);

    const admin = () => {
      router.push("/admin");
    };

    const login = () => {
      router.push("/login");
    };

    const joinQuiz = () => {
      if (!quizCode.value) {
        showError("Veuillez entrer un code de quiz.");
        return;
      }
      if (!isConnected && !username.value) {
        showError("Veuillez entrer un nom d'utilisateur.");
        return;
      }

      isLoading.value = true;
      // Simulate API call or quiz joining logic
      setTimeout(() => {
        isLoading.value = false;
        console.log("Joining quiz with code:", quizCode.value);
        if (!isConnected) {
          console.log("Username:", username.value);
        }
        // Redirect to the quiz page or perform other actions
      }, 2000);
    };

    const showError = (message) => {
      errorMessage.value = message;
      setTimeout(() => {
        errorMessage.value = "";
      }, 3000);
    };

    return {
      admin,
      isAdmin,
      isConnected,
      quizCode,
      username,
      errorMessage,
      isLoading,
      joinQuiz,
      login,
    };
  },
};
</script>

<style scoped>
/* Custom animations */
.animate-fade-in {
  animation: fadeIn 0.5s ease-in-out;
}

#home {
  background-image: url("/background.jpg");
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
