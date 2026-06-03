<template>
  <q-page class="route-page">
    <div class="route-content">
      <div class="route-header">
        <h2 class="route-title">Planeamento de Rota</h2>
        <p class="route-subtitle">Encontre a melhor ligacao entre paragens</p>
      </div>

      <div class="route-form">
        <div class="form-field">
          <q-select
            v-model="origemId"
            :options="stopOptions"
            label="Paragem de Origem"
            outlined
            dense
            emit-value
            map-options
            use-input
            @filter="filterStops"
          >
            <template v-slot:prepend>
              <q-icon name="trip_origin" color="positive" />
            </template>
          </q-select>
        </div>

        <div class="swap-btn-container">
          <q-btn flat round dense icon="swap_vert" color="primary" @click="swapStops" />
        </div>

        <div class="form-field">
          <q-select
            v-model="destinoId"
            :options="filteredStopOptions"
            label="Paragem de Destino"
            outlined
            dense
            emit-value
            map-options
            use-input
            @filter="filterStopsDest"
          >
            <template v-slot:prepend>
              <q-icon name="place" color="negative" />
            </template>
          </q-select>
        </div>

        <button class="btn-search" @click="searchRoute" :disabled="loading || !origemId || !destinoId">
          <q-spinner v-if="loading" size="20px" class="q-mr-sm" />
          <q-icon v-else name="route" size="20px" class="q-mr-sm" />
          <span>Procurar Rota</span>
        </button>
      </div>

      <div v-if="error" class="error-msg q-mt-md">{{ error }}</div>

      <div v-if="rota" class="route-result q-mt-md">
        <div class="result-summary">
          <div class="summary-item">
            <q-icon name="schedule" size="18px" />
            <span>{{ rota.totalMinutos }} min</span>
          </div>
          <div class="summary-item">
            <q-icon name="swap_horiz" size="18px" />
            <span>{{ rota.trocas }} troca{{ rota.trocas !== 1 ? 's' : '' }}</span>
          </div>
        </div>

        <div class="segments-list">
          <div v-for="(seg, idx) in rota.segmentos" :key="idx" class="segment-card">
            <div class="segment-header">
              <div class="line-badge">
                <q-icon name="directions_bus" size="16px" />
                <span>{{ seg.linhaNome }}</span>
              </div>
              <span class="segment-duration">{{ seg.duracaoMinutos }} min</span>
            </div>

            <div class="segment-stops">
              <div v-for="(p, pIdx) in seg.paragens" :key="pIdx" class="stop-point">
                <div class="stop-line-container">
                  <div class="stop-dot" :class="{ 'first': pIdx === 0, 'last': pIdx === seg.paragens.length - 1 }"></div>
                  <div v-if="pIdx < seg.paragens.length - 1" class="stop-connector"></div>
                </div>
                <span class="stop-name" :class="{ 'bold': pIdx === 0 || pIdx === seg.paragens.length - 1 }">{{ p.nome }}</span>
              </div>
            </div>

            <div v-if="idx < rota.segmentos.length - 1" class="transfer-indicator">
              <q-icon name="transfer_within_a_station" size="18px" color="grey-6" />
              <span>Troca de linha</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="noRoute" class="no-route q-mt-md">
        <q-icon name="bus_alert" size="48px" color="grey-5" />
        <p>Nao foi encontrada rota direta entre estas paragens.</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useViagensStore } from 'src/stores/viagens'
import { useAuthStore } from 'src/stores/auth'

const authStore = useAuthStore()
const viagensStore = useViagensStore()

const origemId = ref(null)
const destinoId = ref(null)
const rota = ref(null)
const loading = ref(false)
const error = ref(null)
const noRoute = ref(false)
const filterText = ref('')
const filterTextDest = ref('')

onMounted(async () => {
  if (!viagensStore.stops || viagensStore.stops.length === 0) {
    await viagensStore.fetchStops()
  }
})

const stopOptions = computed(() => {
  const stops = viagensStore.stops || []
  if (!filterText.value) {
    return stops.slice(0, 100).map(s => ({ label: s.nome, value: s.id }))
  }
  const q = filterText.value.toLowerCase()
  return stops
    .filter(s => s.nome.toLowerCase().includes(q))
    .slice(0, 100)
    .map(s => ({ label: s.nome, value: s.id }))
})

const filteredStopOptions = computed(() => {
  const stops = viagensStore.stops || []
  if (!filterTextDest.value) {
    return stops.slice(0, 100).map(s => ({ label: s.nome, value: s.id }))
  }
  const q = filterTextDest.value.toLowerCase()
  return stops
    .filter(s => s.nome.toLowerCase().includes(q))
    .slice(0, 100)
    .map(s => ({ label: s.nome, value: s.id }))
})

function filterStops(val, update) {
  filterText.value = val
  update()
}

function filterStopsDest(val, update) {
  filterTextDest.value = val
  update()
}

function swapStops() {
  const temp = origemId.value
  origemId.value = destinoId.value
  destinoId.value = temp
}

async function searchRoute() {
  if (!origemId.value || !destinoId.value) return
  loading.value = true
  error.value = null
  rota.value = null
  noRoute.value = false

  try {
    const response = await fetch(`/api/network/route?from=${origemId.value}&to=${destinoId.value}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.status === 404) {
      noRoute.value = true
      return
    }
    if (!response.ok) throw new Error('Erro ao procurar rota')
    rota.value = await response.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.route-page {
  background: #f1f5f9;
  min-height: 100vh;
}

.route-content {
  padding: calc(var(--header-h, 42px) + 16px) 16px calc(var(--tabbar-h, 78px) + 16px);
  max-width: 500px;
  margin: 0 auto;
}

.route-header {
  text-align: center;
  margin-bottom: 20px;
}

.route-title {
  font-size: 22px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px;
}

.route-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.route-form {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-field {
  width: 100%;
}

.swap-btn-container {
  text-align: center;
}

.btn-search {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 46px;
  background: #028e5c;
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.2s;
}

.btn-search:hover {
  background: #03694a;
  transform: translateY(-1px);
}

.btn-search:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.error-msg {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  color: #dc2626;
  font-size: 13px;
}

.result-summary {
  display: flex;
  gap: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.segments-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.segment-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.line-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #e0f2fe;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  color: #0369a1;
}

.segment-duration {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.segment-stops {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.stop-point {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 32px;
}

.stop-line-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
  flex-shrink: 0;
}

.stop-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.stop-dot.first {
  background: #10b981;
  width: 14px;
  height: 14px;
}

.stop-dot.last {
  background: #ef4444;
  width: 14px;
  height: 14px;
}

.stop-connector {
  width: 2px;
  height: 22px;
  background: #cbd5e1;
}

.stop-name {
  font-size: 13px;
  color: #475569;
}

.stop-name.bold {
  font-weight: 700;
  color: #1e293b;
}

.transfer-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  padding: 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
}

.no-route {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
}

.no-route p {
  margin: 8px 0 0;
  font-size: 14px;
}
</style>
