<template>
  <div class="player-view">
    <h1>Quizandar</h1>
    <p>Введите ваше имя, чтобы присоединиться</p>
    <form @submit.prevent="joinGame">
      <input
        v-model="name"
        type="text"
        placeholder="Ваше имя"
        required
        maxlength="30"
      />
      <button type="submit" :disabled="isLoading">
        {{ isLoading ? 'Подключение...' : 'Присоединиться' }}
      </button>
    </form>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="player" class="success">
      Вы присоединились как <strong>{{ player.name }}</strong> (ID: {{ player.id }})
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

const name = ref('')
const isLoading = ref(false)
const error = ref('')
const player = ref<{ id: string; name: string } | null>(null)

async function joinGame() {
  if (!name.value.trim()) return
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await axios.post('/api/players/register', {
      name: name.value.trim()
    })
    player.value = data
    // Сохраним в localStorage, чтобы знать, что игрок зарегистрирован
    localStorage.setItem('quizandar_player', JSON.stringify(data))
  } catch (e) {
    error.value = 'Не удалось подключиться. Попробуйте ещё раз.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.player-view {
  max-width: 400px;
  margin: 2rem auto;
  text-align: center;
}
input {
  width: 100%;
  padding: 0.7rem;
  font-size: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #ccc;
  border-radius: 6px;
}
button {
  width: 100%;
  padding: 0.7rem;
  font-size: 1rem;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
button:disabled {
  background-color: #aaa;
}
.error {
  color: red;
  margin-top: 1rem;
}
.success {
  margin-top: 1rem;
  color: #2e7d32;
}
</style>
