<template>
  <q-page class="driver-page">
    <div class="driver-content">
      <div v-if="!driverStore.selectedVehicleId" class="bus-selection">
        <div class="selection-header">
          <q-icon name="directions_bus" size="48px" color="primary" />
          <h2 class="selection-title">Selecionar Autocarro</h2>
          <p class="selection-desc">Escolha o autocarro que esta a conduzir para comecar a receber validacoes em tempo real.</p>
        </div>

        <div v-if="driverStore.loading" class="text-center q-py-lg">
          <q-spinner size="40px" color="primary" />
        </div>

        <div v-else-if="driverStore.vehicles.length === 0" class="text-center q-py-lg text-grey-6">
          Nenhum veiculo encontrado
        </div>

        <div v-else class="vehicle-list">
          <button
            v-for="v in driverStore.vehicles"
            :key="v.id"
            class="vehicle-card"
            @click="selecionarVeiculo(v.id)"
          >
            <q-icon name="directions_bus" size="28px" class="vehicle-icon" />
            <div class="vehicle-info">
              <span class="vehicle-plate">{{ v.matricula }}</span>
              <span class="vehicle-line">{{ v.linhaNome }}</span>
              <span class="vehicle-seats">{{ v.lotacaoAtual }}/{{ v.nLugares }} lugares</span>
            </div>
            <q-icon name="chevron_right" size="24px" class="vehicle-arrow" />
          </button>
        </div>
      </div>

      <div v-else class="live-panel">
        <div class="live-header">
          <div class="live-bus-info">
            <q-icon name="directions_bus" size="24px" />
            <span>{{ selectedVehiclePlate }}</span>
          </div>
          <q-btn flat dense icon="swap_horiz" label="Trocar Bus" @click="trocarBus" />
        </div>

        <!-- Trip Control Panel -->
        <div v-if="!driverStore.activeViagemVeiculo" class="trip-control-card">
          <h3 class="trip-control-title">Iniciar Viagem</h3>
          <p class="trip-control-desc">Selecione a direcao e prima iniciar.</p>

          <div class="direction-buttons">
            <button
              v-for="t in driverStore.trajetos"
              :key="t.id"
              class="direction-btn"
              :class="{ 'direction-btn--selected': selectedTrajetoId === t.id }"
              @click="selectedTrajetoId = t.id"
            >
              <span class="direction-label">{{ t.direcao === 'IDA' ? 'Ida' : 'Volta' }}</span>
              <span class="direction-route">{{ t.primeiraParagem }} → {{ t.ultimaParagem }}</span>
            </button>
          </div>

          <button
            class="btn-start-trip"
            :disabled="!selectedTrajetoId || driverStore.loading"
            @click="startTrip"
          >
            <q-spinner v-if="driverStore.loading" size="18px" class="q-mr-sm" />
            <q-icon v-else name="play_arrow" size="20px" class="q-mr-sm" />
            <span>Iniciar Viagem</span>
          </button>
        </div>

        <!-- Active Trip Status -->
        <div v-else class="active-trip-card">
          <div class="active-trip-header">
            <div class="active-trip-indicator">
              <span class="pulse-dot-sm"></span>
              <span class="active-trip-label">Viagem Ativa</span>
            </div>
            <span class="active-trip-route">
              {{ activeTripLinha }} - {{ activeTripDirecao === 'IDA' ? 'Ida' : 'Volta' }}
            </span>
          </div>

          <button
            class="btn-end-trip"
            :disabled="driverStore.loading"
            @click="endTrip"
          >
            <q-spinner v-if="driverStore.loading" size="18px" class="q-mr-sm" />
            <q-icon v-else name="stop" size="20px" class="q-mr-sm" />
            <span>Terminar Viagem</span>
          </button>
        </div>

        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-value">{{ driverStore.notifications.length }}</span>
            <span class="stat-label">Validacoes</span>
          </div>
          <div class="stat-item">
            <span class="stat-value text-positive">{{ validadasCount }}</span>
            <span class="stat-label">Validas</span>
          </div>
          <div class="stat-item">
            <span class="stat-value text-negative">{{ recusadasCount }}</span>
            <span class="stat-label">Recusadas</span>
          </div>
        </div>

        <div class="notification-feed">
          <transition-group name="notif-slide" tag="div" class="notif-list">
            <div
              v-for="n in driverStore.notifications"
              :key="n.id"
              class="notif-card"
              :class="n.valido ? 'notif-valid' : 'notif-invalid'"
            >
              <div class="notif-indicator">
                <span class="indicator-dot" :class="n.valido ? 'dot-green' : 'dot-red'"></span>
              </div>
              <div class="notif-body">
                <span class="notif-name">{{ n.nomePassageiro }}</span>
                <span class="notif-tipo">{{ n.tituloTipo }}</span>
              </div>
              <div class="notif-time">
                {{ formatTime(n.timestamp) }}
              </div>
            </div>
          </transition-group>

          <div v-if="driverStore.notifications.length === 0" class="empty-feed">
            <q-icon name="wifi_tethering" size="48px" color="grey-5" />
            <p>A aguardar validacoes...</p>
            <p class="text-caption">As validacoes dos passageiros aparecerao aqui em tempo real</p>
          </div>
        </div>

        <div class="feed-footer" v-if="driverStore.notifications.length > 0">
          <q-btn flat dense icon="delete_sweep" label="Limpar" @click="driverStore.clearNotifications()" />
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useDriverStore } from 'src/stores/driver'

const driverStore = useDriverStore()
const selectedTrajetoId = ref(null)

onMounted(async () => {
  await driverStore.fetchVehicles()
})

onUnmounted(() => {
  driverStore.disconnect()
})

const selectedVehiclePlate = computed(() => {
  const v = driverStore.vehicles.find(v => v.id === driverStore.selectedVehicleId)
  return v ? v.matricula : ''
})

const validadasCount = computed(() => driverStore.notifications.filter(n => n.valido).length)
const recusadasCount = computed(() => driverStore.notifications.filter(n => !n.valido).length)

function selecionarVeiculo(veiculoId) {
  const v = driverStore.vehicles.find(v => v.id === veiculoId)
  driverStore.connectWebSocket(veiculoId)
  driverStore.fetchActiveTrips(veiculoId)
  if (v?.linhaId) {
    driverStore.fetchTrajetos(v.linhaId)
  }
}

function trocarBus() {
  driverStore.disconnect()
  selectedTrajetoId.value = null
}

const activeTripLinha = computed(() => {
  return driverStore.activeViagemVeiculo?.trajeto?.linha?.nome || ''
})

const activeTripDirecao = computed(() => {
  return driverStore.activeViagemVeiculo?.trajeto?.direcao || ''
})

async function startTrip() {
  if (!selectedTrajetoId.value || !driverStore.selectedVehicleId) return
  await driverStore.startViagem(driverStore.selectedVehicleId, selectedTrajetoId.value)
}

async function endTrip() {
  await driverStore.endViagem()
  selectedTrajetoId.value = null
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  try {
    const d = new Date(timestamp)
    return d.toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return ''
  }
}
</script>

<style scoped>
.driver-page {
  background: #f1f5f9;
  min-height: 100vh;
}

.driver-content {
  padding: calc(var(--header-h, 42px) + 16px) 16px 16px;
  max-width: 600px;
  margin: 0 auto;
}

.selection-header {
  text-align: center;
  padding: 32px 0 24px;
}

.selection-title {
  font-size: 22px;
  font-weight: 800;
  color: #0b1a16;
  margin: 12px 0 4px;
}

.selection-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.vehicle-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vehicle-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  width: 100%;
}

.vehicle-card:hover {
  border-color: #028e5c;
  box-shadow: 0 2px 8px rgba(2, 142, 92, 0.12);
}

.vehicle-icon {
  color: #028e5c;
}

.vehicle-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.vehicle-plate {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.vehicle-seats {
  font-size: 11px;
  color: #64748b;
}

.vehicle-line {
  font-size: 12px;
  color: #0369a1;
  font-weight: 600;
}

.vehicle-arrow {
  color: #94a3b8;
}

.live-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.live-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
}

.trip-control-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
}

.trip-control-title {
  font-size: 16px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px;
}

.trip-control-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 12px;
}

.direction-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.direction-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 10px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.direction-btn:hover {
  border-color: #028e5c;
}

.direction-btn--selected {
  border-color: #028e5c;
  background: #e6f7f0;
}

.direction-label {
  font-size: 15px;
  font-weight: 700;
  color: #0b1a16;
}

.direction-route {
  font-size: 11px;
  color: #64748b;
  text-align: center;
}

.btn-start-trip {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #028e5c 0%, #01bc74 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(2, 142, 92, 0.2);
  transition: transform 0.2s;
}

.btn-start-trip:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-start-trip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.active-trip-card {
  background: #fff;
  border: 2px solid #10b981;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.active-trip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.active-trip-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-dot-sm {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: dot-pulse-green 1.6s infinite;
}

.active-trip-label {
  font-size: 14px;
  font-weight: 700;
  color: #10b981;
}

.active-trip-route {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.btn-end-trip {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  border: none;
  background: #ef4444;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
  transition: transform 0.2s;
}

.btn-end-trip:hover:not(:disabled) {
  transform: translateY(-2px);
  background: #dc2626;
}

.btn-end-trip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.live-bus-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
}

.stats-bar {
  display: flex;
  gap: 8px;
}

.stat-item {
  flex: 1;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
}

.stat-label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.notification-feed {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  min-height: 300px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notif-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  border-left: 4px solid;
}

.notif-valid {
  background: #f0fdf4;
  border-left-color: #10b981;
}

.notif-invalid {
  background: #fef2f2;
  border-left-color: #ef4444;
}

.notif-indicator {
  flex-shrink: 0;
}

.indicator-dot {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
}

.dot-green {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
  animation: dot-pulse-green 0.6s ease-out;
}

.dot-red {
  background: #ef4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
  animation: dot-pulse-red 0.6s ease-out;
}

@keyframes dot-pulse-green {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.3); }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes dot-pulse-red {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.3); }
  100% { transform: scale(1); opacity: 1; }
}

.notif-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.notif-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.notif-tipo {
  font-size: 11px;
  color: #64748b;
}

.notif-time {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

.notif-slide-enter-active {
  transition: all 0.4s ease-out;
}

.notif-slide-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.empty-feed {
  text-align: center;
  padding: 48px 16px;
  color: #94a3b8;
}

.empty-feed p {
  margin: 8px 0 0;
  font-size: 14px;
}

.feed-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
