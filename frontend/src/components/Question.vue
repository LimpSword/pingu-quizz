<template>
  <div class="question-card p-6 bg-white rounded-lg shadow-lg">
    <p class="text-lg font-semibold text-gray-800">{{ question.question }}</p>
    <img v-if="questionImageUrl" :src="questionImageUrl" class="w-full h-auto rounded mt-4"/>

    <!-- Open-ended question -->
    <div v-if="question.type === 'OPEN'" class="mt-4">
      <input type="text" v-model="userAnswer" class="input-field" placeholder="Votre réponse"/>
    </div>

    <!-- True/False question -->
    <div v-else-if="question.type === 'TRUE_FALSE'" class="mt-4 flex space-x-4">
      <button @click="selectAnswer(true)" :class="getButtonClass(true)">Vrai</button>
      <button @click="selectAnswer(false)" :class="getButtonClass(false)">Faux</button>
    </div>

    <!-- Multiple Choice question -->
    <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="mt-4">
      <div v-for="(answer, index) in question.answers" :key="index"
           class="flex items-center space-x-2">
        <input type="checkbox" v-model="selectedAnswers" :value="answer"
               class="form-checkbox h-5 w-5 text-blue-600"/>
        <span>{{ answer.answer }}</span>
        <img v-if="getAnswerImageUrl(answer)" :src="getAnswerImageUrl(answer)"
             class="w-12 h-12 rounded ml-2"/>
      </div>
    </div>
  </div>
</template>

<script>
import {ref, computed, onMounted, onBeforeUnmount, watch} from "vue";
import {apiUrls} from "@/api/api.js";

export default {
  props: {
    question: Object,
  },
  setup(props) {
    const userAnswer = ref("");
    const selectedAnswers = ref([]);
    const selectedTrueFalse = ref(null);
    const objectUrls = ref([]);

    // Compute URL for question image, handling both strings and File objects
    const questionImageUrl = computed(() => {
      if (!props.question.image) return null;

      if (typeof props.question.image === 'string') {
        return apiUrls.download + "/" + props.question.image;
      } else if (props.question.image instanceof File) {
        const url = URL.createObjectURL(props.question.image);
        objectUrls.value.push(url);
        return url;
      }
      return null;
    });

    // Function to get image URL for answers
    const getAnswerImageUrl = (answer) => {
      if (!answer.image) return null;

      if (typeof answer.image === 'string') {
        return apiUrls.download + "/" + answer.image;
      } else if (answer.image instanceof File) {
        const url = URL.createObjectURL(answer.image);
        objectUrls.value.push(url);
        return url;
      }
      return null;
    };

    const selectAnswer = (value) => {
      selectedTrueFalse.value = value;
    };

    const getButtonClass = (value) => {
      return selectedTrueFalse.value === value ? 'bg-blue-500 text-white px-4 py-2 rounded' : 'bg-gray-200 px-4 py-2 rounded';
    };

    // Clean up object URLs to prevent memory leaks
    onBeforeUnmount(() => {
      objectUrls.value.forEach(url => URL.revokeObjectURL(url));
    });

    return {
      userAnswer,
      selectedAnswers,
      selectedTrueFalse,
      selectAnswer,
      getButtonClass,
      questionImageUrl,
      getAnswerImageUrl
    };
  },
};
</script>

<style scoped>
.question-card {
  border: 1px solid #ddd;
}

.input-field {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 4px;
}
</style>
