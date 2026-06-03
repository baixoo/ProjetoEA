<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="driver-header">
      <q-toolbar>
        <q-toolbar-title class="driver-toolbar-title">
          <img src="/assets/logo.png" alt="NoTUB" class="driver-logo" />
          <span class="driver-title-text">Painel do Motorista</span>
        </q-toolbar-title>
        <div class="driver-status" v-if="driverStore.selectedVehicleId">
          <span class="status-dot" :class="driverStore.connected ? 'connected' : 'disconnected'"></span>
          <span class="status-text">{{ driverStore.connected ? 'Conectado' : 'Desconectado' }}</span>
        </div>
        <q-btn flat dense icon="home" label="Sair" @click="sair" />
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useDriverStore } from 'src/stores/driver'

const router = useRouter()
const driverStore = useDriverStore()

function sair() {
  driverStore.disconnect()
  router.push('/home')
}
</script>

<style scoped>
.driver-header {
  background: #0e2d24;
}

.driver-toolbar-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.driver-logo {
  height: 28px;
  object-fit: contain;
}

.driver-title-text {
  font-size: 15px;
  font-weight: 600;
}

.driver-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.connected {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
}

.status-dot.disconnected {
  background: #ef4444;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
}
</style>
