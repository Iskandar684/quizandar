import { createRouter, createWebHistory } from 'vue-router'
import HostView from '../views/HostView.vue'
import PlayerView from '../views/PlayerView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'host', component: HostView },
    { path: '/player', name: 'player', component: PlayerView },
  ],
})

export default router