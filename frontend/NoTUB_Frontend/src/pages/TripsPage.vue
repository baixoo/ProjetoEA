<template>
  <q-page class="trips-page">
    <div class="trips-container">
      <div class="page-header q-mb-md">
        <h1 class="page-title">As Minhas Viagens</h1>
        <p class="page-subtitle">Historico de todas as viagens realizadas</p>
      </div>

      <div v-if="loading" class="flex flex-center q-py-xl">
        <q-spinner color="primary" size="36px" />
      </div>

      <div v-else-if="trips.length === 0" class="empty-state">
        <q-icon name="directions_bus" size="56px" color="grey-5" />
        <p class="empty-title">Sem viagens</p>
        <p class="empty-desc">As suas viagens concluidas aparecerao aqui.</p>
      </div>

      <div v-else class="trips-list">
        <div
          v-for="trip in trips"
          :key="trip.id"
          class="trip-card"
          :class="{ 'trip-card--active': trip.estado === 'ATIVA' }"
        >
          <div class="trip-card__header">
            <div class="trip-card__status-badge" :class="statusClass(trip.estado)">
              {{ statusLabel(trip.estado) }}
            </div>
            <span class="trip-card__date">{{ formatDate(trip.inicio) }}</span>
          </div>

          <div class="trip-card__body">
            <div class="trip-card__route">
              <div class="route-dot route-dot--start"></div>
              <div class="route-line"></div>
              <div class="route-dot route-dot--end"></div>
              <div class="route-info">
                <span class="route-label">{{ trip.paragemEntrada?.nome || 'Paragem de entrada' }}</span>
                <span class="route-label">{{ trip.paragemSaida?.nome || 'Em curso...' }}</span>
              </div>
            </div>
          </div>

          <div class="trip-card__footer">
            <div class="trip-detail">
              <q-icon name="schedule" size="14px" color="grey-6" />
              <span>{{ formatTime(trip.inicio) }}{{ trip.fim ? ' - ' + formatTime(trip.fim) : '' }}</span>
            </div>
            <div class="trip-detail" v-if="trip.viagemVeiculo?.veiculo?.matricula">
              <q-icon name="directions_bus" size="14px" color="grey-6" />
              <span>{{ trip.viagemVeiculo.veiculo.matricula }}</span>
            </div>
            <div class="trip-detail" v-if="trip.titulo">
              <q-icon name="confirmation_number" size="14px" color="grey-6" />
              <span>{{ trip.titulo.tipo || 'Titulo' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const trips = ref([])
const loading = ref(true)

onMounted(async () => {
  if (!authStore.token) {
    router.push('/signin')
    return
  }

  try {
    const response = await fetch('/api/viagens/utilizador', {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (!response.ok) throw new Error('Falha ao carregar viagens')
    trips.value = await response.json()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function statusClass(estado) {
  if (estado === 'ATIVA') return 'badge--active'
  if (estado === 'CONCLUIDA') return 'badge--done'
  return 'badge--other'
}

function statusLabel(estado) {
  if (estado === 'ATIVA') return 'Em Curso'
  if (estado === 'CONCLUIDA') return 'Concluida'
  return estado
}

function formatDate(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleDateString('pt-PT', { day: '2-digit', month: 'short', year: 'numeric' })
}

function formatTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.trips-page {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: calc(var(--header-h, 42px) + 24px) var(--page-pad, 20px) calc(var(--tabbar-h, 78px) + 16px);
  background: #f8f9fa;
}

.trips-container {
  width: 100%;
}

.page-header {
  text-align: left;
}

.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 24px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0;
  line-height: 1.2;
}

.page-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #6c757d;
  margin: 4px 0 0 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 60px;
}

.empty-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #0b1a16;
  margin: 0;
}

.empty-desc {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  text-align: center;
}

.trips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trip-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.trip-card--active {
  border-color: #028e5c;
  box-shadow: 0 2px 8px rgba(2, 142, 92, 0.1);
}

.trip-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trip-card__status-badge {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 20px;
  text-transform: uppercase;
}

.badge--active {
  background: #e6f7f0;
  color: #028e5c;
}

.badge--done {
  background: #e9ecef;
  color: #495057;
}

.badge--other {
  background: #fff3e0;
  color: #e65100;
}

.trip-card__date {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #6c757d;
}

.trip-card__body {
  margin-bottom: 12px;
}

.trip-card__route {
  display: flex;
  align-items: stretch;
  gap: 8px;
  position: relative;
}

.route-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
}

.route-dot--start {
  background: #028e5c;
}

.route-dot--end {
  background: #d32f2f;
  margin-top: auto;
  margin-bottom: 4px;
}

.route-line {
  width: 2px;
  min-height: 28px;
  background: #dee2e6;
  flex-shrink: 0;
  margin-left: 4px;
}

.route-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 4px;
  flex: 1;
  padding: 0 0 0 4px;
}

.route-label {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #0b1a16;
  line-height: 1.3;
}

.trip-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  border-top: 1px solid #f1f3f5;
  padding-top: 10px;
}

.trip-detail {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #6c757d;
}
</style>
