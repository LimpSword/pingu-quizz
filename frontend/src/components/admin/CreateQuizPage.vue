<template>
  <div class="p-8 bg-gray-100 min-h-screen flex gap-8">
    <!-- Form Section -->
    <div class="flex-1 m-4">
      <!-- Back Button -->
      <div class="flex mb-4">
        <button @click="router().push('/admin')" class="text-blue-600 hover:text-blue-900">
          < Retour
        </button>
        <h1 class="ml-5 text-3xl font-bold text-gray-800">
          {{ isEditing ? 'Modifier le quizz' : 'Créer un nouveau quizz' }}</h1>
      </div>

      <form @submit.prevent="submitQuiz" class="bg-white p-6 rounded-lg shadow">
        <!-- Quiz Name -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700">Nom du quizz</label>
          <input v-model="quiz.name" required class="input-field"/>
        </div>

        <!-- Quiz Description -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700">Description</label>
          <textarea v-model="quiz.description" required class="input-field"></textarea>
        </div>

        <!-- Quiz Image -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700">Image</label>
          <input type="file" @change="uploadQuizImage" class="input-field"/>
        </div>

        <!-- Questions Section -->
        <div class="mb-6">
          <h2 class="text-xl font-semibold text-gray-800 mb-4">Questions</h2>
          <div v-for="(question, index) in quiz.questions" :key="index"
               class="mb-6 bg-gray-50 p-4 rounded-lg" v-on:click="selectQuestion(index)"
               :class="{ 'border-2 border-blue-500': selectedQuestion === index }">
            <!-- Question Header -->
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-700">Question {{ index + 1 }}</h3>
              <div class="flex gap-2">
                <button type="button" @click="toggleQuestion(index)"
                        class="text-gray-600 hover:text-gray-900">
                  {{ isQuestionCollapsed(index) ? '↓' : '↑' }}
                </button>
                <button type="button" @click="removeQuestion(index)"
                        class="text-red-600 hover:text-red-900">
                  ✕
                </button>
              </div>
            </div>

            <!-- Question Content -->
            <div v-show="!isQuestionCollapsed(index)">
              <div>
                <label class="block text-sm font-medium text-gray-700">Question</label>
              </div>
              <input v-model="question.question" required class="input-field"/>

              <!-- Question Type -->
              <div class="mt-2">
                <label class="block text-sm font-medium text-gray-700">Type</label>
              </div>
              <select v-model="question.type" class="input-field mt-2">
                <option value="OPEN">Ouverte</option>
                <option value="TRUE_FALSE">Vrai / Faux</option>
                <option value="MULTIPLE_CHOICE">Choix multiple</option>
              </select>

              <!-- Question Points -->
              <div class="mt-2">
                <label class="block text-sm font-medium text-gray-700">Points</label>
              </div>
              <input type="number" v-model="question.points" class="input-field mt-2"
                     placeholder="Points"/>

              <!-- Question Difficulty -->
              <div class="mt-2">
                <label class="block text-sm font-medium text-gray-700">Difficulté</label>
              </div>
              <button type="button" @click="question.difficulty = 'EASY'"
                      :class="{ 'border-2 border-black': question.difficulty === 'EASY' }"
                      class="px-4 py-2 rounded mr-2 bg-green-500">Facile
              </button>
              <button type="button" @click="question.difficulty = 'MEDIUM'"
                      :class="{ 'border-2 border-black': question.difficulty === 'MEDIUM' }"
                      class="px-4 py-2 rounded mr-2 bg-yellow-500">Moyen
              </button>
              <button type="button" @click="question.difficulty = 'HARD'"
                      :class="{ 'border-2 border-black': question.difficulty === 'HARD' }"
                      class="px-4 py-2 rounded bg-red-500">Difficile
              </button>

              <!-- Question Time -->
              <div class="mt-2">
                <label class="block text-sm font-medium text-gray-700">Temps (en secondes)</label>
              </div>
              <input type="number" v-model="question.time" class="input-field mt-2"
                     placeholder="Temps (en secondes)"/>

              <!-- Question Image -->
              <input type="file" @change="(e) => uploadQuestionImage(e, index)"
                     class="input-field mt-2"/>

              <!-- Answers Section -->
              <div v-if="question.type === 'MULTIPLE_CHOICE'" class="ml-6 mt-4">
                <h3 class="text-lg font-medium text-gray-700 mb-2">Réponses</h3>
                <div v-for="(answer, aIndex) in question.answers" :key="aIndex" class="mb-4">
                  <input v-model="answer.answer" required class="input-field"
                         placeholder="Réponse"/>
                  <input type="file" @change="(e) => uploadAnswerImage(e, index, aIndex)"
                         class="input-field mt-2"/>
                  <label class="flex items-center mt-2">
                    <input type="checkbox" v-model="answer.correct"
                           class="form-checkbox h-5 w-5 text-blue-600"/>
                    <span class="ml-2 text-sm text-gray-700">Correcte</span>
                  </label>
                </div>
                <button type="button" @click="addAnswer(index)"
                        class="text-blue-600 hover:text-blue-900 text-sm">
                  + Ajouter une réponse
                </button>
              </div>
              <div v-if="question.type === 'TRUE_FALSE'" class="ml-6 mt-4">
                <h3 class="text-lg font-medium text-gray-700 mb-2">Réponse</h3>
                <div class="flex items-center space-x-4">
                  <p>Quelle est la bonne réponse ?</p>
                  <div class="flex items-center space-x-2">
                    <input type="radio" id="true-radio" v-model="question.trueAnswer" :value="true"
                           class="form-radio h-5 w-5 text-blue-600"/>
                    <label for="true-radio">Vrai</label>

                    <input type="radio" id="false-radio" v-model="question.trueAnswer"
                           :value="false"
                           class="form-radio h-5 w-5 text-blue-600 ml-4"/>
                    <label for="false-radio">Faux</label>
                  </div>
                </div>
              </div>
              
              <div v-if="question.type === 'OPEN'" class="ml-6 mt-4">
                <h3 class="text-lg font-medium text-gray-700 mb-2">Réponse correcte</h3>
                <div class="space-y-2">
                  <p class="text-sm text-gray-600">Entrez la réponse attendue pour cette question ouverte :</p>
                  <input v-model="question.correctAnswer" placeholder="Réponse correcte" 
                         class="input-field" />
                </div>
              </div>
            </div>
          </div>
          <button type="button" @click="addQuestion"
                  class="text-blue-600 hover:text-blue-900 text-sm">
            + Ajouter une question
          </button>
        </div>

        <button type="submit" class="btn-primary">
          {{ isEditing ? 'Modifier le quizz' : 'Créer le quizz' }}
        </button>
      </form>
    </div>

    <!-- Quiz Preview - Fixed on right side -->
    <div class="w-1/3 sticky top-8 self-start bg-white p-6 rounded-lg shadow">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">Aperçu d'une question</h2>
      <div v-if="selectedQuestion !== null" class="space-y-4">
        <h3 class="text-lg font-bold">Question {{ selectedQuestion + 1 }}</h3>
        <Question :question="quiz.questions[selectedQuestion]"/>
      </div>
      <div v-else class="text-gray-500 text-center py-8">
        Sélectionnez une question pour voir l'aperçu
      </div>
    </div>
  </div>
</template>

<script>
import {onMounted, ref} from "vue";
import {useRouter, useRoute} from "vue-router";
import Question from "@/components/Question.vue";
import {fetcher, postFormData} from "@/api/api.js";
import router from "@/router/index.js";

export default {
  methods: {
    router() {
      return router
    }
  },
  components: {Question},
  setup() {
    const route = useRoute();
    const router = useRouter();
    const isEditing = ref(false);
    const quizId = ref(null);
    const quiz = ref({
      name: "",
      description: "",
      image: null,
      questions: [],
    });

    const collapsedQuestions = ref(new Set());
    const selectedQuestion = ref(null);

    const isQuestionCollapsed = (index) => collapsedQuestions.value.has(index);

    const toggleQuestion = (index) => {
      if (collapsedQuestions.value.has(index)) {
        collapsedQuestions.value.delete(index);
      } else {
        collapsedQuestions.value.add(index);
      }
      selectedQuestion.value = index;
    };

    const selectQuestion = (index) => {
      selectedQuestion.value = index;
    };

    const removeQuestion = (index) => {
      quiz.value.questions.splice(index, 1);
      collapsedQuestions.value.delete(index);
      if (selectedQuestion.value === index) {
        selectedQuestion.value = null;
      }
    };

    const addQuestion = () => {
      const newIndex = quiz.value.questions.length;
      quiz.value.questions.push({
        question: "",
        type: "MULTIPLE_CHOICE",
        image: null,
        points: 1,
        difficulty: "EASY",
        time: 30,
        trueAnswer: true, // Default value for TRUE_FALSE questions
        correctAnswer: "", // Default value for OPEN questions
        answers: [{answer: "", correct: false, image: null}],
      });
      selectedQuestion.value = newIndex;
    };

    const addAnswer = (questionIndex) => {
      quiz.value.questions[questionIndex].answers.push({
        answer: "",
        correct: false,
        image: null,
      });
    };

    const uploadQuizImage = (event) => {
      const file = event.target.files[0];
      if (file) quiz.value.image = file;
    };

    const uploadQuestionImage = (event, index) => {
      const file = event.target.files[0];
      if (file) quiz.value.questions[index].image = file;
    };

    const uploadAnswerImage = (event, questionIndex, answerIndex) => {
      const file = event.target.files[0];
      if (file) quiz.value.questions[questionIndex].answers[answerIndex].image = file;
    };

    const loadQuiz = async (id) => {
      try {
        const response = await fetcher(`/quizz/get/${id}`);
        const quizData = await response.json();

        // Process questions to set trueAnswer for TRUE_FALSE type and correctAnswer for OPEN type
        const processedQuestions = quizData.questions.map(q => {
          if (q.type === 'TRUE_FALSE') {
            // Find the correct answer (should be either "True" or "False")
            const correctAnswer = q.answers.find(a => a.correct);
            q.trueAnswer = correctAnswer &&
              (correctAnswer.answer === "True" ||
                correctAnswer.answer === "true" ||
                correctAnswer.answer === "TRUE");
          } else if (q.type === 'OPEN') {
            // For open questions, extract the correctAnswer from the answers
            const correctAnswer = q.answers.find(a => a.correct);
            q.correctAnswer = correctAnswer ? correctAnswer.answer : "";
          }
          return q;
        });

        quiz.value = {
          name: quizData.name,
          description: quizData.description,
          image: null,
          questions: processedQuestions,
        };
      } catch (error) {
        console.error("Failed to load quiz:", error);
      }
    };

    const submitQuiz = async () => {
      try {
        const formData = new FormData();
        formData.append("name", quiz.value.name);
        formData.append("description", quiz.value.description);
        if (quiz.value.image) {
          formData.append("image", quiz.value.image);
        }

        quiz.value.questions.forEach((question, qIndex) => {
          // append all images to a specific form data key
          if (question.image) {
            formData.append(`question_images_${qIndex}`, question.image);
          }

          let questionData = {
            question: question.question,
            type: question.type,
            points: question.points,
            difficulty: question.difficulty,
            time: question.time,
            answers: []
          };

          // Handle different question types
          if (question.type === 'TRUE_FALSE') {
            // For TRUE_FALSE, create two answers and mark one as correct
            questionData.answers = [
              {answer: "True", correct: question.trueAnswer === true},
              {answer: "False", correct: question.trueAnswer === false}
            ];
          } else if (question.type === 'MULTIPLE_CHOICE') {
            // For MULTIPLE_CHOICE, use the existing answers
            questionData.answers = question.answers.map((answer, aIndex) => {
              if (answer.image) {
                formData.append(`answer_images_${qIndex}_${aIndex}`, answer.image);
              }
              return {
                answer: answer.answer,
                correct: answer.correct,
              };
            });
          } else {
            // For OPEN questions, use the correctAnswer provided
            const correctAnswer = question.correctAnswer || "";
            questionData.answers = [{answer: correctAnswer, correct: true}];
          }

          formData.append(`questions`, JSON.stringify(questionData));
        });

        const endpoint = isEditing.value
          ? `/quizz/edit/${quizId.value}`
          : "/quizz/create";

        const response = await postFormData(endpoint, formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        });

        if (response.ok) await router.push("/admin");
      } catch (error) {
        console.error("Failed to create quiz:", error);
      }
    };

    onMounted(() => {
      const id = route.params.id;
      if (id) {
        isEditing.value = true;
        quizId.value = id;
        loadQuiz(id);
      }
    });

    return {
      quiz,
      selectedQuestion,
      selectQuestion,
      isQuestionCollapsed,
      toggleQuestion,
      removeQuestion,
      addQuestion,
      addAnswer,
      uploadQuizImage,
      uploadQuestionImage,
      uploadAnswerImage,
      submitQuiz,
      isEditing
    };
  },
};
</script>

<style scoped>
.input-field {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 4px;
}

.btn-primary {
  background-color: #1d4ed8;
  color: white;
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
