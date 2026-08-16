<template>
  <div class="host-view">
    <h1>Quizandar — Ведущий</h1>
    <p>Отсканируйте QR-код, чтобы присоединиться к игре</p>
    <div class="qr-container">
      <qrcode-vue :value="playerUrl" :size="300" level="M" />
    </div>
    <p class="url-text">{{ playerUrl }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import QrcodeVue from 'qrcode.vue'

const playerUrl = computed(() => {
  // Берём текущий origin (http://host:port) и добавляем hash-маршрут
  return `${window.location.origin}/#/player`
})
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.url-text {
  margin-top: 1rem;
  font-family: monospace;
  font-size: 1.1rem;
}
</style>
