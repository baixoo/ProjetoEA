<template>
  <q-page class="driver-page">
    <div class="driver-content">
      
      <!-- STEP 1: SELECT VEHICLE -->
      <div v-if="!driverStore.selectedVehicleId" class="bus-selection animate-fade">
        <div class="selection-header">
          <div class="logo-wrapper">
            <q-icon name="directions_bus" size="48px" class="selection-icon" />
          </div>
          <h2 class="selection-title">Painel do Motorista</h2>
          <p class="selection-desc">Escolha ou faça scan do veículo para iniciar o serviço.</p>
        </div>

        <div v-if="driverStore.loading" class="spinner-container">
          <q-spinner size="44px" color="primary" />
        </div>

        <div v-else-if="driverStore.vehicles.length === 0" class="empty-state-card">
          <q-icon name="info" size="32px" color="grey-6" />
          <span>Nenhum autocarro com linha atribuída no sistema.</span>
        </div>

        <div v-else class="vehicle-list">
          <button
            v-for="v in driverStore.vehicles"
            :key="v.id"
            class="vehicle-card"
            @click="selecionarVeiculo(v.id)"
          >
            <div class="vehicle-card-left">
              <div class="bus-badge">
                <q-icon name="directions_bus" size="24px" />
              </div>
              <div class="vehicle-details">
                <span class="vehicle-plate">{{ v.matricula }}</span>
                <span class="vehicle-line">{{ v.linhaNome }}</span>
              </div>
            </div>
            <div class="vehicle-card-right">
              <q-badge color="accent" outline>{{ v.lotacaoAtual }}/{{ v.nLugares }} lug</q-badge>
              <q-icon name="chevron_right" size="20px" class="chevron-icon" />
            </div>
          </button>
        </div>
      </div>

      <!-- STEP 2: LIVE PANEL (VEHICLE SELECTED) -->
      <div v-else class="live-panel animate-fade">
        
        <!-- HEADER INFO -->
        <div class="panel-header">
          <div class="bus-info-chip">
            <q-icon name="directions_bus" color="primary" size="20px" />
            <span class="plate-text">{{ selectedVehiclePlate }}</span>
          </div>
          <button class="swap-bus-btn" @click="trocarBus">
            <q-icon name="swap_horiz" size="18px" />
            <span>TROCAR BUS</span>
          </button>
        </div>

        <!-- TRIP SETUP CARD (NOT STARTED) -->
        <div v-if="!driverStore.activeViagemVeiculo" class="setup-card">
          <div class="card-header">
            <h3 class="card-title">Preparar Viagem</h3>
            <p class="card-subtitle">Selecione o trajeto e a hora de partida programada.</p>
          </div>

          <!-- Direction selection -->
          <div class="setup-section">
            <span class="section-label">Sentido da Linha</span>
            <div class="direction-buttons">
              <button
                v-for="t in driverStore.trajetos"
                :key="t.id"
                class="direction-btn"
                :class="{ 'direction-btn--selected': selectedTrajetoId === t.id }"
                @click="selectedTrajetoId = t.id"
              >
                <div class="direction-badge" :class="t.direcao">
                  {{ t.direcao === 'IDA' ? 'IDA' : 'VOLTA' }}
                </div>
                <div class="direction-route-text">
                  {{ t.linha }} - {{ t.primeiraParagem }} → {{ t.ultimaParagem }}
                </div>
              </button>
            </div>
          </div>

          <!-- Stops list preview -->
          <div v-if="selectedTrajeto && selectedTrajeto.paragens" class="setup-section paragens-preview animate-fade">
            <span class="section-label">Paragens do Trajeto ({{ selectedTrajeto.paragens.length }})</span>
            <div class="paragens-list-inline">
              <div v-for="(p, idx) in selectedTrajeto.paragens" :key="p.pontoPassagemId" class="paragem-inline-item">
                <span class="paragem-ordem">{{ p.ordem + 1 }}.</span>
                <span class="paragem-nome">{{ p.nome }}</span>
                <q-badge color="grey-3" text-color="grey-8" class="q-ml-xs" style="font-size: 9px; padding: 2px 4px;">Z{{ p.zona }}</q-badge>
                <q-icon v-if="idx < selectedTrajeto.paragens.length - 1" name="arrow_forward" size="12px" color="grey-4" class="q-mx-xs" />
              </div>
            </div>
          </div>

          <!-- Schedule selection -->
          <div v-if="selectedTrajetoId" class="setup-section animate-fade">
            <span class="section-label">Partida Programada (Janela 2h)</span>
            <div v-if="driverStore.loading" class="q-py-md text-center">
              <q-spinner size="24px" color="primary" />
            </div>
            <div v-else-if="driverStore.scheduleOptions.length === 0" class="no-schedules">
              Não há partidas programadas para esta janela de horário.
            </div>
            <div v-else class="schedule-grid">
              <button
                v-for="sched in driverStore.scheduleOptions"
                :key="sched.gtfsTripId"
                class="schedule-btn"
                :class="{ 'schedule-btn--selected': selectedGtfsTripId === sched.gtfsTripId }"
                @click="selectedGtfsTripId = sched.gtfsTripId; selectedServiceId = sched.serviceId"
              >
                <q-icon name="schedule" size="16px" />
                <span>{{ formatTimeOnly(sched.horaPartida) }}</span>
                <span class="trip-id-sub">({{ sched.serviceId }})</span>
              </button>
            </div>
          </div>

          <!-- Start Button -->
          <button
            class="btn-start"
            :disabled="!selectedTrajetoId || !selectedGtfsTripId || driverStore.loading"
            @click="startTrip"
          >
            <q-spinner v-if="driverStore.loading" size="20px" class="q-mr-sm" />
            <q-icon v-else name="play_arrow" size="22px" class="q-mr-sm" />
            <span>INICIAR VIAGEM</span>
          </button>
        </div>

        <!-- ACTIVE RUN PANEL -->
        <div v-else class="active-run-container">
          
          <!-- STATE BAR -->
          <div class="active-status-bar">
            <div class="indicator">
              <span class="pulse-dot"></span>
              <span class="indicator-text">VIAGEM ATIVA</span>
            </div>
            <div class="route-label">
              {{ activeTripLinha }}
            </div>
          </div>

          <!-- PROGRESS CARD -->
          <div class="progress-card">
            <div class="progress-header">
              <div class="progress-stop-info">
                <span class="stop-label">PARAGEM ATUAL</span>
                <span class="stop-name">{{ currentStopName }}</span>
              </div>
              <div class="progress-stop-info text-right">
                <span class="stop-label">PARAGEM SEGUINTE</span>
                <span class="stop-name text-accent">{{ nextStopName }}</span>
              </div>
            </div>

            <!-- Visual Track -->
            <div class="progress-track-wrapper">
              <div class="terminal-dot hollow"></div>
              <div class="track-line">
                <div class="track-fill" :style="{ width: progressPercent + '%' }"></div>
                <!-- Animated Yellow Bus Marker -->
                <div class="bus-marker" :style="{ left: progressPercent + '%' }">
                  <q-icon name="directions_bus" size="18px" />
                </div>
              </div>
              <div class="terminal-dot hollow" :class="{ 'filled': progressPercent >= 100 }"></div>
            </div>

            <div class="progress-footer">
              <span>Progresso da rota: {{ Math.round(progressPercent) }}%</span>
              <span v-if="vehicleDelay > 0" class="delay-badge"> atraso: {{ vehicleDelay }} min </span>
              <span v-else class="ontime-badge">A Horas</span>
            </div>
          </div>

          <!-- LONG PRESS ACTION BUTTON -->
          <div class="action-button-container">
            <button
              class="btn-progress-longpress"
              :class="{ 'pressing': isPressing, 'btn-finish': isFinalStop }"
              @mousedown="handlePressStart"
              @touchstart.prevent="handlePressStart"
              @mouseup="handlePressEnd"
              @mouseleave="handlePressEnd"
              @touchend="handlePressEnd"
              @touchcancel="handlePressEnd"
            >
              <!-- Fill bar overlay -->
              <div class="progress-fill-overlay" :style="{ width: pressProgress + '%' }"></div>
              
              <div class="btn-label-content">
                <template v-if="isPressing">
                  <span class="holding-txt">A confirmar... {{ Math.ceil((100 - pressProgress) / 20) / 10 }}s</span>
                </template>
                <template v-else>
                  <q-icon :name="isFinalStop ? 'flag' : 'skip_next'" size="24px" class="q-mr-xs" />
                  <span>{{ isFinalStop ? 'Terminar Viagem (Premir 5s)' : 'Seguir para Próxima Paragem (Premir 5s)' }}</span>
                </template>
              </div>
            </button>
            <p class="hold-helper">Pressione continuamente o botão por 5 segundos para registar a paragem.</p>
          </div>

          <!-- VALIDATIONS METRICS -->
          <div class="metrics-grid">
            <div class="metric-card">
              <div class="metric-value text-primary">{{ validadasCount }}</div>
              <div class="metric-label">Total Validados</div>
            </div>
            <div class="metric-card">
              <div class="metric-value text-accent">{{ lotacaoAtual }}</div>
              <div class="metric-label">Lotação Atual</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDriverStore } from 'src/stores/driver'

const router = useRouter()
const driverStore = useDriverStore()

const selectedTrajetoId = ref(null)
const selectedGtfsTripId = ref(null)
const selectedServiceId = ref(null)

const selectedTrajeto = computed(() => {
  return driverStore.trajetos.find(t => t.id === selectedTrajetoId.value)
})

// Long press refs
const isPressing = ref(false)
const pressProgress = ref(0)
let pressInterval = null

onMounted(async () => {
  await driverStore.fetchVehicles()
  const savedVehicleId = driverStore.selectedVehicleId
  
  if (savedVehicleId) {
    selecionarVeiculo(Number(savedVehicleId))
  }
})

onUnmounted(() => {
  stopMetricsPolling()
  driverStore.disconnect()
})

const selectedVehiclePlate = computed(() => {
  const v = driverStore.vehicles.find(v => v.id === driverStore.selectedVehicleId)
  return v ? v.matricula : ''
})

const validadasCount = computed(() => driverStore.notifications.filter(n => n.valido).length)
const lotacaoAtual = computed(() => {
  const v = driverStore.vehicles.find(v => v.id === driverStore.selectedVehicleId)
  return v ? v.lotacaoAtual : 0
})
const vehicleDelay = computed(() => {
  const v = driverStore.vehicles.find(v => v.id === driverStore.selectedVehicleId)
  return v ? v.tempoAtraso : 0
})

const activeTripLinha = computed(() => {
  return driverStore.activeViagemVeiculo?.trajeto?.linha?.nome || ''
})

// Progress calculations
const sortedStops = computed(() => {
  const pp = driverStore.activeViagemVeiculo?.trajeto?.pontosDePassagem || []
  return [...pp].sort((a, b) => a.ordem - b.ordem)
})

const currentStopIndex = computed(() => {
  if (!driverStore.activeViagemVeiculo) return -1
  const activePontoId = driverStore.activeViagemVeiculo.pontoAtualId
  return sortedStops.value.findIndex(p => p.id === activePontoId)
})

const currentStopName = computed(() => {
  const idx = currentStopIndex.value
  if (idx === -1) return 'Não iniciada'
  return sortedStops.value[idx]?.paragem?.nome || ''
})

const nextStopName = computed(() => {
  const idx = currentStopIndex.value
  if (idx === -1) return ''
  if (idx + 1 >= sortedStops.value.length) return 'Final da Linha'
  return sortedStops.value[idx + 1]?.paragem?.nome || ''
})

const progressPercent = computed(() => {
  const total = sortedStops.value.length
  const current = currentStopIndex.value
  if (total <= 1 || current === -1) return 0
  return (current / (total - 1)) * 100
})

const isFinalStop = computed(() => {
  const total = sortedStops.value.length
  const current = currentStopIndex.value
  return total > 0 && current === total - 1
})

// Watch trajeto selection to load schedules
watch(selectedTrajetoId, async (newId) => {
  if (newId) {
    await driverStore.fetchScheduleOptions(newId)
    selectedGtfsTripId.value = null
    selectedServiceId.value = null
  } else {
    driverStore.scheduleOptions = []
  }
})

// Metrics polling
let metricsInterval = null
function startMetricsPolling() {
  stopMetricsPolling()
  metricsInterval = setInterval(() => {
    if (driverStore.selectedVehicleId) {
      driverStore.refreshVehicleMetrics()
    }
  }, 5000)
}

function stopMetricsPolling() {
  if (metricsInterval) {
    clearInterval(metricsInterval)
    metricsInterval = null
  }
}

function selecionarVeiculo(veiculoId) {
  const v = driverStore.vehicles.find(v => v.id === veiculoId)
  driverStore.connectWebSocket(veiculoId)
  driverStore.fetchActiveTrips(veiculoId)
  if (v?.linhaId) {
    driverStore.fetchTrajetos(v.linhaId)
  }
  startMetricsPolling()
}

function trocarBus() {
  stopMetricsPolling()
  driverStore.disconnect()
  selectedTrajetoId.value = null
  selectedGtfsTripId.value = null
  selectedServiceId.value = null
  // Clear query params
  router.replace('/driver')
}

async function startTrip() {
  if (!selectedTrajetoId.value || !selectedGtfsTripId.value || !selectedServiceId.value || !driverStore.selectedVehicleId) return
  await driverStore.startScheduledViagem(driverStore.selectedVehicleId, selectedTrajetoId.value, selectedServiceId.value, selectedGtfsTripId.value)
}

// Long press button handling
function handlePressStart() {
  isPressing.value = true
  pressProgress.value = 0
  
  if (pressInterval) clearInterval(pressInterval)
  
  pressInterval = setInterval(() => {
    pressProgress.value += 1 // 100 steps total
    if (pressProgress.value >= 100) {
      clearInterval(pressInterval)
      triggerAction()
      resetPress()
    }
  }, 30) // 30 * 100 = 3000ms (3 seconds)
}

function handlePressEnd() {
  resetPress()
}

function resetPress() {
  isPressing.value = false
  pressProgress.value = 0
  if (pressInterval) {
    clearInterval(pressInterval)
    pressInterval = null
  }
}

async function triggerAction() {
  if (isFinalStop.value) {
    await driverStore.endViagem()
    selectedTrajetoId.value = null
    selectedGtfsTripId.value = null
    selectedServiceId.value = null
  } else {
    await driverStore.advanceStop()
  }
}

function formatTimeOnly(timeStr) {
  if (!timeStr) return ''
  const parts = timeStr.split(':')
  return `${parts[0]}:${parts[1]}`
}

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.driver-page {
  background: #f8fafc;
  min-height: 100vh;
  font-family: 'Plus Jakarta Sans', sans-serif;
  color: #1e293b;
}

.driver-content {
  padding: calc(var(--header-h, 42px) + 20px) 16px 80px;
  max-width: 520px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Animations */
.animate-fade {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Vehicle Selection */
.selection-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-wrapper {
  width: 80px;
  height: 80px;
  background: #e6f7f0;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.selection-icon {
  color: #028e5c;
}

.selection-title {
  font-family: 'Outfit', sans-serif;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px;
}

.selection-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.spinner-container {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.empty-state-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.vehicle-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vehicle-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(15, 23, 42, 0.02);
  width: 100%;
}

.vehicle-card:hover {
  border-color: #028e5c;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(2, 142, 92, 0.08);
}

.vehicle-card-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bus-badge {
  width: 44px;
  height: 44px;
  background: #f1f5f9;
  border-radius: 14px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.vehicle-card:hover .bus-badge {
  background: #e6f7f0;
  color: #028e5c;
}

.vehicle-details {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.vehicle-plate {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.vehicle-line {
  font-size: 12px;
  color: #64748b;
}

.vehicle-card-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chevron-icon {
  color: #cbd5e1;
  transition: transform 0.2s;
}

.vehicle-card:hover .chevron-icon {
  color: #028e5c;
  transform: translateX(3px);
}

/* Panel Header */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 12px 18px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.bus-info-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #0f172a;
}

.swap-bus-btn {
  background: #f1f5f9;
  border: none;
  border-radius: 10px;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.swap-bus-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* Setup Card */
.setup-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-title {
  font-family: 'Outfit', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px;
}

.card-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.setup-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-label {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.direction-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.direction-btn {
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 18px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.direction-btn:hover {
  border-color: #cbd5e1;
}

.direction-btn--selected {
  border-color: #028e5c;
  background: #f4fbf7;
}

.direction-badge {
  font-size: 10px;
  font-weight: 800;
  padding: 4px 8px;
  border-radius: 6px;
}

.direction-badge.IDA {
  background: #e0f2fe;
  color: #0369a1;
}

.direction-badge.VOLTA {
  background: #fef3c7;
  color: #b45309;
}

.direction-route-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.no-schedules {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 8px;
}

.schedule-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.schedule-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.schedule-btn--selected {
  background: #028e5c;
  border-color: #028e5c;
  color: #ffffff;
}

.trip-id-sub {
  font-size: 9px;
  opacity: 0.7;
}

.btn-start {
  background: #028e5c;
  border: none;
  border-radius: 14px;
  padding: 14px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-start:hover:not(:disabled) {
  background: #02764d;
}

.btn-start:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Active Run Container */
.active-run-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.active-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #0f172a;
  border-radius: 14px;
  padding: 10px 16px;
  color: #ffffff;
}

.indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulseGreen 1.5s infinite;
}

@keyframes pulseGreen {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.indicator-text {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.route-label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
}

/* Progress Card */
.progress-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.01);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.progress-stop-info {
  display: flex;
  flex-direction: column;
  max-width: 45%;
}

.stop-label {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.stop-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-accent {
  color: #028e5c;
}

/* Visual Track */
.progress-track-wrapper {
  display: flex;
  align-items: center;
  position: relative;
  height: 24px;
  margin-bottom: 20px;
}

.terminal-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ffffff;
  border: 3px solid #cbd5e1;
  z-index: 2;
  flex-shrink: 0;
}

.terminal-dot.filled {
  border-color: #028e5c;
  background: #028e5c;
}

.track-line {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  position: relative;
  margin: 0 -1px;
}

.track-fill {
  height: 100%;
  background: #028e5c;
  width: 0%;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Yellow Bus Marker */
.bus-marker {
  position: absolute;
  top: 50%;
  width: 32px;
  height: 32px;
  background: #f59e0b;
  border: 3px solid #ffffff;
  border-radius: 50%;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translate(-50%, -50%);
  transition: left 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.4);
  z-index: 3;
}

.progress-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.delay-badge {
  background: #fef2f2;
  color: #ef4444;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 700;
}

.ontime-badge {
  background: #f0fdf4;
  color: #10b981;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 700;
}

/* Long press action button container */
.action-button-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-progress-longpress {
  position: relative;
  background: #1e293b;
  color: #ffffff;
  border: none;
  border-radius: 16px;
  height: 56px;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.15s ease, background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
  width: 100%;
}

.btn-progress-longpress:active {
  transform: scale(0.98);
}

.btn-progress-longpress.btn-finish {
  background: #ef4444;
}

.btn-progress-longpress.pressing {
  background: #0f172a;
}

.progress-fill-overlay {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: rgba(16, 185, 129, 0.45);
  transition: width 0.05s linear;
  z-index: 1;
}

.btn-progress-longpress.btn-finish .progress-fill-overlay {
  background: rgba(220, 38, 38, 0.5);
}

.btn-label-content {
  position: relative;
  z-index: 2;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
}

.holding-txt {
  letter-spacing: 0.5px;
  animation: pulsate 1s infinite alternate;
}

@keyframes pulsate {
  0% { transform: scale(1); }
  100% { transform: scale(1.03); }
}

.hold-helper {
  font-size: 11px;
  color: #64748b;
  text-align: center;
  margin: 0;
  line-height: 1.4;
}

/* Metrics Grid */
.metrics-grid {
  display: flex;
  gap: 12px;
}

.metric-card {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.01);
}

.metric-value {
  font-family: 'Outfit', sans-serif;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
}

.metric-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-top: 4px;
}

/* Feed Card */
.feed-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.01);
}

.feed-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 14px;
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.feed-body {
  min-height: 160px;
  max-height: 240px;
  overflow-y: auto;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feed-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  animation: itemSlide 0.3s ease-out;
}

@keyframes itemSlide {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.feed-valid {
  background: #f0fdf4;
}

.feed-invalid {
  background: #fef2f2;
}

.feed-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.feed-dot.green { background: #10b981; }
.feed-dot.red { background: #ef4444; }

.feed-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  text-align: left;
}

.feed-passenger {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.feed-ticket {
  font-size: 10px;
  color: #64748b;
}

.feed-time {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 500;
}

.feed-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #94a3b8;
  gap: 8px;
}

.feed-empty p {
  margin: 0;
  font-size: 12px;
}

.feed-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  margin-top: 10px;
}

.clear-feed-btn {
  background: none;
  border: none;
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.5px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.clear-feed-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

/* Footer */
.paragens-list-inline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e2e8f0;
}

.paragem-inline-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  color: #475569;
}

.paragem-ordem {
  font-weight: 700;
  color: #028e5c;
  margin-right: 2px;
}

.realtime-footer {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  margin-top: 10px;
}
</style>
