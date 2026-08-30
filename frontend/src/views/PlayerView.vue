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
    <div v-else-if="currentQuestion && !gameFinished" class="question">
      <h1>{{ currentQuestion.text }}</h1>
      <p v-if="currentQuestion.timeLimitSec > 0">
        Осталось: {{ timeLeft }} сек.
      </p>
      <div class="options">
        <button v-for="opt in currentQuestion.options" :key="opt.id" @click="selectOption(opt.id)" :disabled="answered"
          class="option-button">
          {{ opt.text }}
        </button>
      </div>
      <div v-if="answered" class="answer-result">
        <p v-if="lastAnswer?.correct" class="correct">Верно! +{{ lastAnswer.pointsAwarded }} очков</p>
        <p v-else class="incorrect">Неверно</p>
      </div>
    </div>

    <!-- Экран ожидания -->
    <div v-else-if="!gameFinished" class="waiting">
      <p>Ожидание вопроса...</p>
    </div>

    <!-- Финальный экран результатов -->
    <div v-if="gameFinished && finalScores" class="final-results">
      <h1>Игра завершена</h1>
      <p>Ваше место: <strong>{{ myPlace }}</strong> ({{ myScore }} баллов)</p>
      <table>
        <thead>
          <tr>
            <th>Место</th>
            <th>Игрок</th>
            <th>Баллы</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(entry, index) in sortedFinalScores" :key="entry[0]"
            :class="{ 'highlight': entry[0] === player?.id }">
            <td>{{ index + 1 }}</td>
            <td>{{ getPlayerName(entry[0]) }}</td>
            <td>{{ entry[1] }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import axios from 'axios';
import { gameSocket } from '@/services/gameSocket';
import type { AnswerRecord, Player, Question, ScoreMap } from '@/types/game';

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
/** Флаг завершения игры */
const gameFinished = ref(false);
/** Финальные очки */
const finalScores = ref<ScoreMap | null>(null);

/** Список всех игроков */
const players = ref<Player[]>([]);

/** Моё место в таблице */
const myPlace = computed(() => {
  if (!finalScores.value || !player.value) return null;
  const sorted = sortedFinalScores.value;
  const index = sorted.findIndex(([id]) => id === player.value!.id);
  return index === -1 ? null : index + 1;
});

/** Мои баллы */
const myScore = computed(() => {
  if (!finalScores.value || !player.value) return 0;
  return finalScores.value[player.value.id] ?? 0;
});

/** Отсортированный список финальных очков (по убыванию) */
const sortedFinalScores = computed(() => {
  if (!finalScores.value) return [];
  return Object.entries(finalScores.value).sort((a, b) => b[1] - a[1]);
});

/**
 * Загружает список всех игроков с сервера.
 */
async function fetchPlayers(): Promise<void> {
  try {
    const { data } = await axios.get<Record<string, Player>>('/api/players');
    players.value = Object.values(data);
  } catch (e) {
    console.error('Не удалось получить список игроков', e);
  }
}

/**
 * Возвращает отображаемое имя игрока по его идентификатору.
 * @param playerId — идентификатор
 */
function getPlayerName(playerId: string): string {
  if (playerId === player.value?.id) return 'Вы';
  const p = players.value.find((pl) => pl.id === playerId);
  return p ? p.name : playerId;
}

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
    localStorage.setItem('quizandar_player', JSON.stringify(data));
    setupSocket(data.id);
  } catch (e) {
    error.value = 'Не удалось подключиться. Попробуйте ещё раз.';
  } finally {
    isLoading.value = false;
  }
}

/**
 * Восстанавливает сессию игрока, если она была сохранена ранее.
 */
async function restoreOrJoin(): Promise<void> {
  const saved = localStorage.getItem('quizandar_player');
  if (!saved) return;

  try {
    const savedPlayer = JSON.parse(saved) as Player;
    if (savedPlayer.id && savedPlayer.name) {
      const { data } = await axios.post<Player>('/api/players/join', {
        id: savedPlayer.id,
        name: savedPlayer.name
      });
      player.value = data;
      localStorage.setItem('quizandar_player', JSON.stringify(data));
      setupSocket(data.id);
    }
  } catch (e) {
    localStorage.removeItem('quizandar_player');
    player.value = null;
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
    // Подписка на обновление списка игроков
    gameSocket.onPlayers((list) => {
      players.value = list;
    });
    gameSocket.onQuestion((q) => {
      currentQuestion.value = q;
      answered.value = false;
      lastAnswer.value = null;
      gameFinished.value = false;
      finalScores.value = null;
      if (q.timeLimitSec > 0) {
        timeLeft.value = q.timeLimitSec;
        startTimer();
      }
    });

    gameSocket.onResults((results) => {
      console.log('Results:', results);
    });

    gameSocket.onPersonalAnswerResult((record) => {
      lastAnswer.value = record;
      answered.value = true;
      if (timer !== undefined) {
        clearInterval(timer);
        timer = undefined;
      }
    });

    gameSocket.onFinalScores((scores) => {
      finalScores.value = scores;
      gameFinished.value = true;
      currentQuestion.value = null;
      answered.value = false;
      if (timer !== undefined) {
        clearInterval(timer);
        timer = undefined;
      }
      // Получаем актуальный список игроков для отображения имён
      fetchPlayers();
    });
  });
}

/**
 * Запуск таймера обратного отсчёта.
 */
function startTimer(): void {
  if (timer !== undefined) clearInterval(timer);
  timer = window.setInterval(() => {
    timeLeft.value--;
    if (timeLeft.value <= 0) {
      clearInterval(timer);
      timer = undefined;
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
  answered.value = true;
}

onMounted(() => {
  restoreOrJoin();
});

onBeforeUnmount(() => {
  // при необходимости можно отключить WebSocket
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

.final-results {
  text-align: center;
  padding: 2rem;
}

table {
  width: 100%;
  max-width: 400px;
  margin: 1rem auto;
  border-collapse: collapse;
}

th,
td {
  padding: 0.5rem;
  border: 1px solid #ccc;
  text-align: left;
}

th {
  background-color: #f0f0f0;
}

.highlight {
  background-color: #e0f2f1;
  font-weight: bold;
}
</style>