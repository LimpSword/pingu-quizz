import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from "@/stores/auth.js";
import LoginForm from "@/components/LoginForm.vue";
import HomeView from "@/views/HomeView.vue";
import AdminQuizzesPage from "@/components/admin/AdminQuizzesPage.vue";
import CreateQuizPage from "@/components/admin/CreateQuizPage.vue";
import PlayQuizzPage from "@/components/play/PlayQuizzPage.vue";
import AdminRoomPage from "@/components/admin/AdminRoomPage.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      component: HomeView
    },
    {
      path: "/play/:code",
      name: "play",
      props: true,
      component: PlayQuizzPage
    },
    {
      path: "/login",
      name: "login",
      component: LoginForm
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminQuizzesPage,
      meta: {requiresAdmin: true},
    },
    {
      path: "/admin/quizz/create",
      name: "create-quizz",
      component: CreateQuizPage,
      meta: {requiresAdmin: true},
    },
    {
      path: "/admin/room/:code",
      name: "admin-room",
      component: AdminRoomPage,
      props: true,
      meta: {requiresAdmin: true},
    }
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  auth.removeTokenIfExpired();
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next({path: '/'});
  } else {
    next();
  }
})

export default router
