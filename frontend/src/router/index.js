import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from "@/stores/auth.js";
import LoginForm from "@/components/LoginForm.vue";
import HomeView from "@/views/HomeView.vue";
import AdminQuizzesPage from "@/components/admin/AdminQuizzesPage.vue";
import CreateQuizPage from "@/components/admin/CreateQuizPage.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      component: HomeView
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
    }
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    console.log("??")
    next({path: '/'});
  } else {
    next();
  }
})

export default router
