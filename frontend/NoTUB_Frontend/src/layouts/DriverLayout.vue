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

        <q-btn flat dense icon="logout" label="Sair" @click="handleLogout" />
      </q-toolbar>
    </q-header>

    <q-page-container class="driver-container">
      <router-view />
    </q-page-container>

    <q-footer class="bg-transparent">
      <AppTabBarDriver :active-tab="activeTab" />
    </q-footer>
  </q-layout>
</template>

<script setup>

// import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDriverStore } from 'src/stores/driver'
import { useAuthStore } from 'src/stores/auth'

import AppTabBarDriver from 'components/AppTabBarDriver.vue'

const router = useRouter()
const authStore = useAuthStore()
const driverStore = useDriverStore()

// const isVehicleActive = computed(() => {
//   return driverStore.connected || !!driverStore.selectedVehicleId
// })

function handleLogout() {

  if (typeof driverStore.clearVehicle === 'function') {
    driverStore.clearVehicle()
  } else {
    driverStore.selectedVehicleId = null
    driverStore.matriculaAtiva = null
    localStorage.removeItem('driver_veiculo_id')
    localStorage.removeItem('driver_matricula')
  }

  authStore.logout()
  router.push('/signin')
}
</script>

<style scoped>
.driver-header {
  background: #0e2d24;
}

.driver-container {
  padding-bottom: 60px !important; 
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