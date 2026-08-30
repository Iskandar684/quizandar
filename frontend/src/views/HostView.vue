<template>
  <div class="host-view">
    <h1>Quizandar — Ведущий</h1>

    <!-- QR-код -->
    <div class="qr-container">
      <qrcode-vue :value="playerUrl" :size="300" level="M" />
    </div>
    <p class="url-text">{{ playerUrl }}</p>

    <!-- Кнопка копирования ссылки -->
    <button @click="copyLink" class="copy-button">
      {{ copyButtonText }}
    </button>

    <div class="auto-next-toggle">
      <label>
        <input type="checkbox" v-model="autoNext" @change="onAutoNextChange" />
        Автоматическое переключение на следующий вопрос
      </label>
    </div>

    <!-- Управление игрой -->
    <div class="controls">
      <button @click="startGame">Начать игру</button>
      <button @click="nextQuestion" :disabled="!gameStarted">Следующий вопрос</button>
    </div>

    <!-- Список игроков -->
    <div v-if="players.length" class="players">
      <h2>Игроки:</h2>
      <ul>
        <li v-for="p in players" :key="p.id">{{ p.name }}</li>
      </ul>
    </div>

    <!-- Текущий вопрос -->
    <div v-if="currentQuestion" class="question">
      <h2>{{ currentQuestion.text }}</h2>
      <p>Тип: {{ currentQuestion.type }} | Время: {{ currentQuestion.timeLimitSec }} сек.</p>
    </div>

    <!-- Результаты -->
    <div v-if="results.length" class="results">
      <h2>Результаты:</h2>
      <ul>
        <li v-for="r in results" :key="r.playerId">
          {{ getPlayerName(r.playerId) }}: {{ r.correct ? 'Верно' : 'Неверно' }} (+{{ r.pointsAwarded }})
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import QrcodeVue from 'qrcode.vue';
import { gameSocket } from '@/services/gameSocket';
import type { AnswerRecord, Player, Question } from '@/types/game';

/** URL для QR-кода */
const playerUrl = computed(() => `${window.location.origin}/#/player`);

/** Список игроков */
const players = ref<Player[]>([]);
/** Текущий вопрос */
const currentQuestion = ref<Question | null>(null);
/** Результаты текущего вопроса */
const results = ref<AnswerRecord[]>([]);
/** Флаг, что игра началась */
const gameStarted = ref(false);
/** Текст на кнопке копирования */
const copyButtonText = ref('Копировать ссылку');

/** Флаг автоматического перехода (по умолчанию включён) */
const autoNext = ref(true);

/**
 * Обрабатывает изменение настройки автоперехода.
 */
function onAutoNextChange(): void {
  gameSocket.setAutoNext(autoNext.value);
}

/**
 * Копирует ссылку на страницу игрока в буфер обмена.
 * Использует navigator.clipboard, при недоступности — fallback.
 */
async function copyLink(): Promise<void> {
  const url = playerUrl.value;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      // Fallback для старых браузеров или небезопасного контекста
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
    // Показываем подтверждение
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
    });

    gameSocket.onResults((res) => {
      results.value = res;
    });

    gameSocket.onScores((scores) => {
      console.log('Scores:', scores);
    });
  });
}

/**
 * Отправка команды «Начать игру».
 */
function startGame(): void {
  gameSocket.startGame();
  gameStarted.value = true;
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

.controls {
  margin: 1rem 0;
}

.controls button {
  padding: 0.7rem 1.5rem;
  font-size: 1rem;
  margin: 0 0.5rem;
  cursor: pointer;
}

.players,
.results,
.question {
  margin: 1rem 0;
  text-align: left;
}
</style>
