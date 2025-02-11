<template>
  <div class="fixed inset-0 pointer-events-none">
    <div
      v-for="(confetti, index) in confettis"
      :key="index"
      :style="confetti.style"
      class="absolute w-2 h-2 bg-yellow-400 rounded-full"
    ></div>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";

export default {
  setup() {
    const confettis = ref([]);

    const createConfetti = () => {
      for (let i = 0; i < 100; i++) {
        confettis.value.push({
          style: {
            left: `${Math.random() * 100}vw`,
            top: `${Math.random() * 100}vh`,
            transform: `rotate(${Math.random() * 360}deg)`,
            animation: `fall ${Math.random() * 3 + 2}s linear`,
          },
        });
      }
      setTimeout(() => (confettis.value = []), 3000); // Clear confetti after 3 seconds
    };

    onMounted(() => {
      createConfetti();
    });

    return {
      confettis,
    };
  },
};
</script>

<style>
@keyframes fall {
  to {
    transform: translateY(100vh) rotate(360deg);
  }
}
</style>
