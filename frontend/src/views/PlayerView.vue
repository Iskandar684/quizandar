<template>
  <div class="player-view">
    <!-- Экран регистрации -->
    <div v-if="!player" class="registration">
      <h1>Quizandar</h1>
      <p>Введите ваше имя, чтобы присоединиться</p>
      <form @submit.prevent="joinGame">
        <input v-model="name" type="text" placeholder="Ваше имя" required maxlength="30" />
        <button type="submit" :disabled="isLoading">
          {{ isLoading ? 'Подключение...' : 'Присоединиться' }}
        </button>
      </form>
      <div v-if="error" class="error">{{ error }}</div>
    </div>

    <!-- Экран вопроса -->
    <div v-else-if="currentQuestion" class="question">
      <h1>{{ currentQuestion.text }}</h1>
      <p v-if="currentQuestion.timeLimitSec > 0">
        Осталось: {{ timeLeft }} сек.
      </p>
      <div class="options">
        <button
          v-for="opt in currentQuestion.options"
          :key="opt.id"
          @click="selectOption(opt.id)"
          :disabled="answered"
          class="option-button"
        >
          {{ opt.text }}
        </button>
      </div>
      <div v-if="answered" class="answer-result">
        <p v-if="lastAnswer?.correct" class="correct">Верно! +{{ lastAnswer.pointsAwarded }} очков</p>
        <p v-else class="incorrect">Неверно</p>
      </div>
    </div>

    <!-- Ожидание -->
    <div v-else class="waiting">
      <p>Ожидание вопроса...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import axios from 'axios';
import { gameSocket } from '@/services/gameSocket';
import type { AnswerRecord, Player, Question } from '@/types/game';

/** Имя игрока из формы */
const name = ref('');
/** Флаг загрузки регистрации */
const isLoading = ref(false);
/** Ошибка регистрации */
const error = ref('');
/** Зарегистрированный игрок */
const player = ref<Player | null>(null);
/** Текущий вопрос */
const currentQuestion = ref<Question | null>(null);
/** Последний ответ игрока */
const lastAnswer = ref<AnswerRecord | null>(null);
/** Флаг, что уже ответили */
const answered = ref(false);
/** Оставшееся время (сек) */
const timeLeft = ref(0);
/** Идентификатор таймера */
let timer: number | undefined;

/**
 * Регистрация игрока через REST API.
 */
async function joinGame(): Promise<void> {
  if (!name.value.trim()) return;
  isLoading.value = true;
  error.value = '';
  try {
    const { data } = await axios.post<Player>('/api/players/register', {
      name: name.value.trim()
    });
    player.value = data;
    // Сохраняем в localStorage
    localStorage.setItem('quizandar_player', JSON.stringify(data));
    // Подключаем WebSocket и подписываемся на события
    setupSocket(data.id);
  } catch (e) {
    error.value = 'Не удалось подключиться. Попробуйте ещё раз.';
  } finally {
    isLoading.value = false;
  }
}

/**
 * Настройка WebSocket после регистрации.
 * @param playerId — идентификатор игрока
 */
function setupSocket(playerId: string): void {
  gameSocket.setPlayerId(playerId);
  gameSocket.connect(() => {
    console.log('WebSocket connected');
  });

  // Подписка на новый вопрос
  gameSocket.onQuestion((q) => {
    currentQuestion.value = q;
    answered.value = false;
    lastAnswer.value = null;
    if (q.timeLimitSec > 0) {
      timeLeft.value = q.timeLimitSec;
      startTimer();
    }
  });

  // Подписка на результаты (общие)
  gameSocket.onResults((results) => {
    console.log('Results:', results);
    // можно показать общий рейтинг
  });

  // Подписка на персональный результат
  gameSocket.onPersonalAnswerResult((record) => {
    lastAnswer.value = record;
    answered.value = true;
    if (timer) {
      clearInterval(timer);
       timer = undefined; 
    }
  });
}

/**
 * Запуск таймера обратного отсчёта.
 */
function startTimer(): void {
  if (timer) clearInterval(timer);
  timer = window.setInterval(() => {
    timeLeft.value--;
    if (timeLeft.value <= 0) {
      clearInterval(timer);
       timer = undefined; 
      // Если время вышло и не ответили — считаем, что ответа нет
      if (!answered.value) {
        answered.value = true;
      }
    }
  }, 1000);
}

/**
 * Выбор варианта ответа.
 * @param optionId — идентификатор выбранного варианта
 */
function selectOption(optionId: string): void {
  if (answered.value) return;
  gameSocket.sendAnswer(optionId);
  // Блокируем повторное нажатие
  answered.value = true;
}

// При монтировании проверяем, есть ли сохранённый игрок
onMounted(() => {
  const saved = localStorage.getItem('quizandar_player');
  if (saved) {
    try {
      const p = JSON.parse(saved) as Player;
      player.value = p;
      setupSocket(p.id);
    } catch (e) {
      localStorage.removeItem('quizandar_player');
    }
  }
});

// При размонтировании отписываемся (опционально)
onBeforeUnmount(() => {
  // можно отключить WebSocket, если нужно
});
</script>

<style scoped>
.player-view {
  max-width: 600px;
  margin: 2rem auto;
  padding: 1rem;
  text-align: center;
}
.registration input {
  width: 100%;
  padding: 0.7rem;
  font-size: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #ccc;
  border-radius: 6px;
}
.registration button,
.option-button {
  padding: 0.7rem 1.5rem;
  font-size: 1rem;
  border: none;
  border-radius: 6px;
  background-color: #4caf50;
  color: white;
  cursor: pointer;
  margin: 0.2rem;
}
.registration button:disabled,
.option-button:disabled {
  background-color: #aaa;
  cursor: not-allowed;
}
.options {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.5rem;
}
.correct {
  color: #2e7d32;
  font-weight: bold;
}
.incorrect {
  color: #c62828;
  font-weight: bold;
}
.error {
  color: red;
  margin-top: 0.5rem;
}
.waiting {
  font-size: 1.2rem;
  color: #666;
}
</style>