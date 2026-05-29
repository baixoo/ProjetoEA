<template>
  <q-page class="admin-page" padding>
    <h2 class="page-title">Dashboard</h2>

    <div v-if="loading" class="text-center q-py-lg">
      <q-spinner color="primary" size="40px" />
    </div>

    <div v-else-if="stats" class="stats-grid">
      <div class="stat-card">
        <q-icon name="people" size="32px" color="primary" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.utilizadores ?? 0 }}</span>
          <span class="stat-label">Utilizadores</span>
        </div>
      </div>
      <div class="stat-card">
        <q-icon name="confirmation_number" size="32px" color="teal" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.bilhetes ?? 0 }}</span>
          <span class="stat-label">Bilhetes</span>
        </div>
      </div>
      <div class="stat-card">
        <q-icon name="credit_card" size="32px" color="orange" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.passes ?? 0 }}</span>
          <span class="stat-label">Passes</span>
        </div>
      </div>
      <div class="stat-card">
        <q-icon name="directions_bus" size="32px" color="blue" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.veiculos ?? 0 }}</span>
          <span class="stat-label">Veiculos</span>
        </div>
      </div>
      <div class="stat-card">
        <q-icon name="payments" size="32px" color="green" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.transacoes ?? 0 }}</span>
          <span class="stat-label">Transacoes</span>
        </div>
      </div>
      <div class="stat-card">
        <q-icon name="schedule" size="32px" color="purple" />
        <div class="stat-info">
          <span class="stat-value">{{ stats.viagens ?? 0 }}</span>
          <span class="stat-label">Viagens</span>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from 'src/stores/admin'

const adminStore = useAdminStore()
const loading = ref(true)
const stats = ref(null)

onMounted(async () => {
  try {
    await adminStore.fetchStats()
    stats.value = adminStore.stats
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0e2d24;
  margin: 0 0 24px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #0e2d24;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #757575;
  font-weight: 500;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
</style>
