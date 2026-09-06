<template>
  <div class="host-view">
    <h1>Quizandar — Ведущий</h1>

    <!-- QR-код -->
    <div class="qr-container">
      <qrcode-vue :value="playerUrl" :size="250" level="M" />
    </div>
    <p class="url-text">{{ playerUrl }}</p>

    <!-- Кнопка копирования ссылки -->
    <button @click="copyLink" class="copy-button">
      {{ copyButtonText }}
    </button>

    <!-- Настройка автоперехода -->
    <div class="auto-next-toggle">
      <label>
        <input type="checkbox" v-model="autoNext" @change="onAutoNextChange" />
        Автоматическое переключение на следующий вопрос
      </label>
    </div>

    <!-- Импорт вопросов -->
    <div class="import-questions">
      <input type="file" accept=".json" ref="fileInput" @change="onFileChange" />
      <button @click="importQuestions" :disabled="!selectedFile || importLoading">
        {{ importLoading ? 'Импорт...' : 'Импортировать' }}
      </button>
      <span v-if="importMessage" class="import-message">{{ importMessage }}</span>
    </div>
    <div class="select-questions">
      <label for="questionFile">Тема вопросов:</label>
      <select id="questionFile" v-model="selectedFile" @change="onSelectFile">
        <option value="">-- Выберите файл --</option>
        <option v-for="file in availableFiles" :key="file.fileName" :value="file.fileName">
          {{ file.title }} ({{ file.questionCount }})
        </option>
      </select>
      <span v-if="selectMessage" class="select-message">{{ selectMessage }}</span>
    </div>

    <!-- Управление игрой -->
    <div class="controls">
      <button @click="startGame">Начать игру</button>
      <button @click="nextQuestion" :disabled="!gameStarted">Следующий вопрос</button>
    </div>

    <!-- Текущий вопрос -->
    <div v-if="currentQuestion" class="question">
      <p class="question-progress">Вопрос {{ currentQuestion.questionNumber }} из {{ currentQuestion.totalQuestions }}
      </p>
      <h2>{{ currentQuestion.text }}</h2>
      <p>Тип: {{ currentQuestion.type }} | Время: {{ currentQuestion.timeLimitSec }} сек.</p>
    </div>

    <!-- Результаты текущего вопроса -->
    <div v-if="results.length" class="results">
      <h2>Результаты:</h2>
      <ul>
        <li v-for="r in results" :key="r.playerId">
          {{ getPlayerName(r.playerId) }}: {{ r.correct ? 'Верно' : 'Неверно' }} (+{{ r.pointsAwarded }})
        </li>
      </ul>
    </div>

    <!-- Финальное сообщение -->
    <div v-if="gameFinished" class="final-message">
      <h2>Игра завершена!</h2>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import QrcodeVue from 'qrcode.vue';
import axios from 'axios';
import { gameSocket } from '@/services/gameSocket';
import type { AnswerRecord, Player, Question, ScoreMap } from '@/types/game';

/** URL для QR-кода */
const playerUrl = computed(() => `${window.location.origin}/#/player`);

/** Список игроков */
const players = ref<Player[]>([]);
/** Текущий вопрос */
const currentQuestion = ref<Question | null>(null);
/** Результаты текущего вопроса */
const results = ref<AnswerRecord[]>([]);
/** Текущие очки (не используются, оставлено для будущего) */
const scores = ref<ScoreMap | null>(null);
/** Флаг, что игра началась */
const gameStarted = ref(false);
/** Флаг, что игра завершена */
const gameFinished = ref(false);
/** Текст на кнопке копирования */
const copyButtonText = ref('Копировать ссылку');
/** Флаг автоматического перехода */
const autoNext = ref(true);

// === Импорт вопросов ===
/** Выбранный файл */
const selectedFile = ref<File | null>(null);
/** Флаг загрузки при импорте */
const importLoading = ref(false);
/** Сообщение об импорте */
const importMessage = ref('');
/** Ссылка на input для сброса */
const fileInput = ref<HTMLInputElement | null>(null);

/** Список доступных файлов */
const availableFiles = ref<Array<{ fileName: string; title: string; description: string; questionCount: number }>>([]);

/** Сообщение о выборе */
const selectMessage = ref('');

/**
 * Загружает список доступных файлов вопросов.
 */
async function fetchQuestionFiles(): Promise<void> {
  try {
    const { data } = await axios.get('/api/questions/files');
    availableFiles.value = data;
  } catch (e) {
    console.error('Не удалось получить список файлов', e);
    // Повторить через 3 секунды, если список пуст
    setTimeout(() => {
      if (availableFiles.value.length === 0) fetchQuestionFiles();
    }, 3000);
  }
}

/**
 * Обрабатывает выбор файла и отправляет команду на сервер.
 */
async function onSelectFile(): Promise<void> {
  if (!selectedFile.value) return;
  try {
    const { data } = await axios.post('/api/questions/select', { fileName: selectedFile.value });
    selectMessage.value = `Выбрана тема: ${selectedFile.value} (${data.questionCount} вопросов)`;
  } catch (e) {
    console.error('Ошибка выбора файла', e);
    selectMessage.value = 'Ошибка выбора файла';
  }
}

/**
 * Копирует ссылку на страницу игрока в буфер обмена.
 */
async function copyLink(): Promise<void> {
  const url = playerUrl.value;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = url;
      textArea.style.position = 'fixed';
      textArea.style.left = '-9999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
    copyButtonText.value = 'Скопировано!';
    setTimeout(() => {
      copyButtonText.value = 'Копировать ссылку';
    }, 2000);
  } catch (err) {
    console.error('Ошибка копирования:', err);
    copyButtonText.value = 'Ошибка копирования';
    setTimeout(() => {
      copyButtonText.value = 'Копировать ссылку';
    }, 2000);
  }
}

/**
 * Обрабатывает выбор файла в input.
 */
function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    selectedFile.value = input.files[0];
  } else {
    selectedFile.value = null;
  }
  importMessage.value = '';
}

/**
 * Отправляет выбранный файл на сервер для импорта вопросов.
 */
async function importQuestions(): Promise<void> {
  if (!selectedFile.value) return;
  importLoading.value = true;
  importMessage.value = '';
  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    const { data } = await axios.post<{ importedCount: number; totalCount: number }>('/api/questions/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    importMessage.value = `Импортировано: ${data.importedCount}. Всего вопросов: ${data.totalCount}. Начните новую игру.`;
    // Сбрасываем input
    if (fileInput.value) {
      fileInput.value.value = '';
    }
    selectedFile.value = null;
    fetchQuestionFiles();
  } catch (err) {
    console.error('Ошибка импорта:', err);
    importMessage.value = 'Ошибка импорта. Проверьте формат файла.';
  } finally {
    importLoading.value = false;
  }
}

/**
 * Подключение к WebSocket и подписка на события.
 */
function setupSocket(): void {
  gameSocket.connect(() => {
    console.log('Host connected');
    gameSocket.onPlayers((list) => {
      players.value = list;
    });

    gameSocket.onQuestion((q) => {
      currentQuestion.value = q;
      results.value = [];
      gameStarted.value = true;
      gameFinished.value = false;
    });

    gameSocket.onResults((res) => {
      results.value = res;
    });

    gameSocket.onScores((newScores) => {
      scores.value = newScores;
    });

    gameSocket.onFinalScores((finalScores) => {
      scores.value = finalScores;
      gameFinished.value = true;
      currentQuestion.value = null;
      results.value = [];
    });
  });
}

/**
 * Обрабатывает изменение настройки автоперехода.
 */
function onAutoNextChange(): void {
  gameSocket.setAutoNext(autoNext.value);
}

/**
 * Отправка команды «Начать игру».
 */
function startGame(): void {
  gameSocket.startGame();
  gameStarted.value = true;
  gameFinished.value = false;
}

/**
 * Отправка команды «Следующий вопрос».
 */
function nextQuestion(): void {
  gameSocket.nextQuestion();
}

/**
 * Получить имя игрока по ID.
 * @param playerId — идентификатор игрока
 */
function getPlayerName(playerId: string): string {
  const p = players.value.find((pl) => pl.id === playerId);
  return p ? p.name : playerId;
}

onMounted(() => {
  setupSocket();
  fetchQuestionFiles();
});
</script>

<style scoped>
.host-view {
  text-align: center;
  padding: 2rem;
}

.qr-container {
  display: inline-block;
  padding: 1rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.url-text {
  margin-top: 1rem;
  font-family: monospace;
  font-size: 1.1rem;
  word-break: break-all;
}

.copy-button {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  font-size: 1rem;
  cursor: pointer;
  border: none;
  border-radius: 6px;
  background-color: #2196f3;
  color: white;
  transition: background-color 0.2s;
}

.copy-button:hover {
  background-color: #1976d2;
}

.auto-next-toggle {
  margin: 1rem 0;
}

.import-questions {
  margin: 1rem 0;
}

.import-questions input {
  margin-right: 0.5rem;
}

.import-message {
  margin-left: 0.5rem;
  color: #2e7d32;
  font-size: 0.9rem;
}

.controls {
  margin: 1rem 0;
}

.controls button {
  padding: 0.7rem 1.5rem;
  font-size: 1rem;
  margin: 0 0.5rem;
  cursor: pointer;
}

.question,
.results,
.final-message {
  margin-top: 1rem;
}

.question-progress {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 0.5rem;
}
</style>