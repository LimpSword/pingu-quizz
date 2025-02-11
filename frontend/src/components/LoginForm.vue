<template>
  <div class="login-container">
    <h2>Login</h2>
    <form @submit.prevent="handleLogin">
      <input v-model="username" type="text" placeholder="Username" required/>
      <input v-model="password" type="password" placeholder="Password" required/>
      <button type="submit">Login</button>
    </form>
    <p v-if="errorMessage">{{ errorMessage }}</p>
  </div>
</template>

<script>
import {ref} from "vue";
import {useAuthStore} from "@/stores/auth";
import {useRouter} from "vue-router";

export default {
  setup() {
    const username = ref("");
    const password = ref("");
    const errorMessage = ref("");
    const authStore = useAuthStore();
    const router = useRouter();

    const handleLogin = async () => {
      authStore.login(username.value, password.value).then(result => {
        if (result.token) {
          console.log("Login successful");
          router.push({path: "/admin"});
        } else {
          errorMessage.value = "Invalid credentials";
        }
      })
    };

    return {username, password, handleLogin, errorMessage};
  },
};
</script>

<style scoped>
.login-container {
  max-width: 300px;
  margin: auto;
  text-align: center;
}

input, button {
  display: block;
  width: 100%;
  margin: 10px 0;
}
</style>
