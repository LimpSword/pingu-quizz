import { defineStore } from "pinia";
import {apiUrls} from "@/api/api.js";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || null,
    roles: localStorage.getItem("roles") || null,
  }),
  actions: {
    async login(username, password) {
      try {
        const response = await fetch(apiUrls.auth, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, password }),
        });

        const data = await response.json();
        if (data.token) {
          this.token = data.token;
          this.roles = data.roles;
          localStorage.setItem("token", data.token);
          localStorage.setItem("roles", data.roles);
        }
        return data;
      } catch (error) {
        console.error("Login error:", error);
        return { error: "Login failed" };
      }
    },
    logout() {
      this.token = null;
      this.role = null;
      localStorage.removeItem("token");
      localStorage.removeItem("roles");
    },
    removeTokenIfExpired() {
      if (this.token) {
        const token = JSON.parse(atob(this.token.split(".")[1]));
        if (token.exp < Date.now() / 1000) {
          this.logout();
        }
      }
    }
  },
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.roles && state.roles.includes("ADMIN"),
    isConnected: (state) => state.token !== null,
  },
});
