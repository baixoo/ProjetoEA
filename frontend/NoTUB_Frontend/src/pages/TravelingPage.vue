<template>
  <q-page class="traveling-page">
    <transition name="slide-fade">
      <div v-if="showSuccessBanner" class="success-banner">
        <div class="banner-content">
          <q-icon name="check_circle" size="24px" color="positive" class="banner-icon" />
          <div class="banner-text">
            <span class="banner-title">Viagem Iniciada!</span>
            <span class="banner-desc">Título validado com sucesso. Boa viagem!</span>
          </div>
        </div>
        <q-btn flat round dense icon="close" color="grey-6" @click="showSuccessBanner = false" />
      </div>
    </transition>

    <div class="traveling-content">
      <div class="trip-status-header text-center q-pt-md">
        <h1 class="traveling-title">Em Viagem</h1>
        
        <div class="q-px-lg q-mt-md row justify-center">
          <div 
            class="bg-white text-weight-bold text-h6 shadow-2 row items-center justify-center no-wrap"
            style="
              width: 100%; 
              max-width: 340px; 
              height: 60px; 
              border-radius: 30px; 
              border: 1px solid #e0e0e0;
            "
          >
            <q-icon name="place" color="primary" size="sm" class="q-mr-sm flex-shrink-0" />
            
            <div class="ellipsis text-center q-pr-sm">
              {{ currentStopName }}
            </div>
          </div>
        </div>

        <div class="row justify-center items-center q-mt-sm">
          <q-badge color="positive" rounded class="q-mr-xs" />
          <span class="text-caption text-grey-7">Viagem Ativa em Tempo Real</span>
        </div>
      </div>

      <div class="bus-illustration-container">
        <div class="bus-illustration">
          <div class="bus-body">
            <div class="parallax-layer layer-clouds"></div>
            <div class="parallax-layer layer-mountains"></div>
            <div class="parallax-layer layer-hills"></div>
            <div class="parallax-layer layer-trees"></div>
            <div class="road-container">
              <div class="road-stripes"></div>
            </div>
          </div>
          <img src="/assets/mask-group.svg" alt="" class="bus-mask" />
        </div>
      </div>

      <div v-if="activeTrip" class="trip-dashboard-card q-mx-md q-mb-md">
        <div class="dashboard-header">
          <div class="bus-badge">
            <q-icon name="directions_bus" size="20px" />
            <span>Autocarro {{ busPlate }}</span>
          </div>
          <div class="line-badge">
            <span>{{ lineName }}</span>
          </div>
        </div>
        
        <div class="dashboard-details">
          <div class="detail-row">
            <div class="detail-col">
              <span class="detail-label">Embarque</span>
              <span class="detail-value text-ellipsis">{{ entryStopName }}</span>
            </div>
            <div class="detail-col text-right">
              <span class="detail-label">Hora de Entrada</span>
              <span class="detail-value">{{ formatTime(activeTrip.dataHoraEntrada) }}</span>
            </div>
          </div>

          <div class="detail-row q-mt-sm">
            <div class="detail-col">
              <span class="detail-label">Título Utilizado</span>
              <span class="detail-value title-type">
                {{ activeTrip.titulo?.tipo === 'PASSE' ? 'Passe' : 'Bilhete Simples' }}
              </span>
            </div>
            <div class="detail-col text-right">
              <span class="detail-label">Zonas Autorizadas</span>
              <span class="detail-value text-positive font-bold">{{ formattedZones }}</span>
            </div>
          </div>
        </div>
        
        <!-- <div class="dashboard-footer">
          <span class="occupancy-info">
            <q-icon name="people" size="16px" color="grey-6" class="q-mr-xs" />
            Lotação aproximada: 23 pessoas
          </span>
        </div> -->
      </div>

      <div class="action-buttons-container q-px-md q-pb-lg">
        <button class="btn-validate" @click="qrDialogOpen = true">
          <img src="/assets/icon-qr.svg" alt="" class="btn-validate__icon" />
          <span>Validar Viagem</span>
        </button>

        <button class="btn-exit" @click="openExitConfirmation">
          <q-icon name="directions_run" size="28px" class="btn-exit__icon" />
          <span>Terminar Viagem</span>
        </button>
      </div>
    </div>

    <q-dialog v-model="qrDialogOpen">
      <q-card class="qr-dialog-card q-pa-lg text-center">
        <h3 class="qr-dialog-title">Código de Validação</h3>
        <p class="qr-dialog-desc">Apresente este código QR ao revisor para confirmar a sua viagem.</p>
        
        <div class="qr-container q-mx-auto q-my-md">
          <img src="/assets/icon-qr.svg" alt="QR Code" class="qr-image" />
          <div class="scanner-line"></div>
        </div>

        <div class="qr-trip-info q-mt-md">
          <div class="qr-info-item">
            <span class="label">ID Viagem</span>
            <span class="value">#{{ activeTrip?.id || '0000' }}</span>
          </div>
          <div class="qr-info-item">
            <span class="label">Autocarro</span>
            <span class="value">{{ busPlate }}</span>
          </div>
          <div class="qr-info-item">
            <span class="label">Validação</span>
            <span class="value text-positive">Válida</span>
          </div>
        </div>

        <q-btn label="Fechar" color="primary" flat class="full-width q-mt-md" @click="qrDialogOpen = false" />
      </q-card>
    </q-dialog>

    <q-dialog v-model="exitDialogOpen" position="bottom" transition-show="slide-up" transition-hide="slide-down">
      <div class="exit-sheet">
        <div class="drag-handle"></div>

        <h3 class="exit-title">Terminar Viagem</h3>
        <p class="exit-subtitle">Tem a certeza que deseja encerrar a viagem atual?</p>

        <div v-if="exitError" class="error-msg q-mb-md">{{ exitError }}</div>

        <div class="exit-actions">
          <button class="btn-exit-confirm bg-negative" @click="confirmEndTrip" :disabled="submitting">
            <q-spinner v-if="submitting" size="18px" class="q-mr-xs" />
            <span>Confirmar Terminar Viagem</span>
          </button>
          <button class="btn-exit-cancel" @click="exitDialogOpen = false" :disabled="submitting">
            <span>Cancelar</span>
          </button>
        </div>
      </div>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useViagensStore } from 'src/stores/viagens'

const router = useRouter()
const viagensStore = useViagensStore()

// State
const showSuccessBanner = ref(false)
const qrDialogOpen = ref(false)
const exitDialogOpen = ref(false)
const selectedExitStop = ref(null)
const exitError = ref('')
const submitting = ref(false)

const activeTrip = computed(() => viagensStore.activeTrip)

onMounted(async () => {
  const active = await viagensStore.fetchActiveTrip()
  if (!active) {
    router.push('/home')
    return
  }

  await viagensStore.fetchStops()

  const veiculoId = active.viagemVeiculo?.veiculo?.id
  if (veiculoId) {
    console.log(`[TravellingPage] A ativar WebSocket real-time para o veículo ${veiculoId}...`)
    viagensStore.connectPassengerWebSocket()
  }

  showSuccessBanner.value = true
  setTimeout(() => {
    showSuccessBanner.value = false
  }, 4500)
})

onUnmounted(() => {
  viagensStore.disconnectPassengerWebSocket()
})

function updateStopName() {
  const atualId = viagensStore.activeTrip?.viagemVeiculo?.pontoAtualId
  if (!atualId) {
    currentStopName.value = 'A carregar paragem...'
    return
  }
  
  const stop = viagensStore.stops.find(s => s.id === atualId)
  currentStopName.value = stop ? stop.nome : `Paragem ID: ${atualId}`
}

function updateStopId() {
  const atualId = viagensStore.activeTrip?.viagemVeiculo?.pontoAtualId
  if (!atualId) {
    selectedExitStop.value = null
    return
  }
  
  const stop = viagensStore.stops.find(s => s.id === atualId)
  selectedExitStop.value = stop ? stop.id : null
}

watch(
  () => viagensStore.activeTrip,
  () => {
    console.log('O watch detetou uma mudança na viagem ativa!')
    if (activeTrip.value.estado !== 'END') {
      updateStopName() 
      updateStopId()
    } else {
      console.log('A viagem ativa foi removida. A redirecionar para a página inicial...')
      confirmEndTrip(true)
    }
  },
  { deep: true } 
)

// Computeds
const busPlate = computed(() => {
  return activeTrip.value?.viagemVeiculo?.veiculo?.matricula || '728'
})

const lineName = computed(() => {
  return activeTrip.value?.viagemVeiculo?.trajeto?.linha?.nome || 'Linha Urbana'
})

const entryStopName = computed(() => {
  return activeTrip.value?.paragemEntrada?.nome || 'Paragem de Entrada'
})

const formattedZones = computed(() => {
  const zona = activeTrip.value?.titulo?.zona
  if (!zona) return 'Z1'
  return `Z${zona.num}`
})

// TODO: REMOVE
// const stopOptions = computed(() => {
//   return (viagensStore.stops || []).map(s => ({
//     label: s.nome,
//     value: s.id
//   }))
// })

const sortedStops = computed(() => {
  const pp = viagensStore.activeTrip?.viagemVeiculo?.trajeto?.pontosDePassagem || []
  return [...pp].sort((a, b) => a.ordem - b.ordem)
})

const currentStopIndex = computed(() => {
  if (!viagensStore.activeTrip?.viagemVeiculo) return -1
  
  const activePontoId = viagensStore.activeTrip.viagemVeiculo.pontoAtualId
  if (!activePontoId) return -1
  
  return sortedStops.value.findIndex(p => p.id === activePontoId)
})

const currentStopName = computed(() => {
  const idx = currentStopIndex.value
  if (idx === -1) return entryStopName.value || 'Paragem Desconhecida'
  return sortedStops.value[idx]?.paragem?.nome || 'Paragem Desconhecida'
})


// Formatter
function formatTime(dateTimeStr) {
  if (!dateTimeStr) return ''
  try {
    const d = new Date(dateTimeStr)
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    const ss = String(d.getSeconds()).padStart(2, '0')
    return `${hh}:${mm}:${ss}`
  } catch {
    return ''
  }
}

// Exit action
function openExitConfirmation() {
  exitError.value = ''
  
  const atualId = viagensStore.activeTrip?.viagemVeiculo?.pontoAtualId
  const currStop = viagensStore.stops.find(s => s.id === atualId)

  selectedExitStop.value = currStop ? currStop.id : null
  exitDialogOpen.value = true
}

// Quero muitos logs nesta função para debug
async function confirmEndTrip(isForcedByDriver = false) {

  if (!isForcedByDriver) {
    if (!selectedExitStop.value || !activeTrip.value) {
      return 
    }
  }

  submitting.value = true
  exitError.value = ''
  
  try {

    await viagensStore.endTrip(activeTrip.value.id, selectedExitStop.value)

    viagensStore.disconnectPassengerWebSocket()

    exitDialogOpen.value = false
    router.push('/home')
  } catch (err) {
    exitError.value = err.message || 'Erro ao terminar a viagem.'
  } finally {
    submitting.value = false
  }
}

</script>

<style scoped>
/* Estilos adicionados para a paragem atual */
.current-stop-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 8px 18px;
  border-radius: 30px;
  border: 1px solid #e2e8f0;
  max-width: 90%;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.current-stop-name {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.traveling-page {
  position: relative;
  min-height: 100vh;
  background: #f1f5f9;
}

.traveling-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  min-height: calc(100vh - var(--header-h, 42px) - var(--tabbar-h, 78px));
  /* Ajustado o padding-top de 44px para 24px para acomodar o novo bloco sem empurrar o layout */
  padding: calc(var(--header-h, 42px) + 24px) 0 calc(var(--tabbar-h, 78px) + 16px) 0;
  width: 100%;
}

/* Success Banner (Design 68:136) */
.success-banner {
  position: fixed;
  top: calc(var(--header-h, 42px) + 10px);
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 32px);
  max-width: 480px;
  background: #ffffff;
  border-left: 4px solid #10b981;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  z-index: 1000;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.banner-icon {
  flex-shrink: 0;
}

.banner-text {
  display: flex;
  flex-direction: column;
}

.banner-title {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #0b1a16;
}

.banner-desc {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #64748b;
}

/* Transitions */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease-out;
}

.slide-fade-enter-from {
  transform: translate(-50%, -20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translate(-50%, -20px);
  opacity: 0;
}

/* Header */
.traveling-title {
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 6px 0;
}

.pulse-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.pulse-text {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

/* Bus Parallax Illustration (Design 121:374) */
.bus-illustration-container {
  width: 100%;
  padding: 0 var(--page-pad, 20px);
  display: flex;
  justify-content: center;
}

.bus-illustration {
  position: relative;
  width: 100%;
  height: 170px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  animation: bus-vibration 0.25s infinite linear;
}

@keyframes bus-vibration {
  0% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-0.8px) rotate(0.05deg); }
  50% { transform: translateY(0.4px) rotate(-0.05deg); }
  75% { transform: translateY(-0.4px) rotate(0deg); }
  100% { transform: translateY(0) rotate(0deg); }
}

.bus-body {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, #7dd3fc, #fef08a);
  overflow: hidden;
}

.bus-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
  pointer-events: none;
  z-index: 10;
}

/* Parallax Scrolling layers */
.parallax-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 300%;
  height: 100%;
  background-position: bottom left;
  background-repeat: repeat-x;
}

.layer-clouds {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='60' viewBox='0 0 160 60'%3E%3Cpath d='M20 20a10 10 0 0 1 18-4 8 8 0 0 1 13 3 12 12 0 0 1 21-2 10 10 0 0 1 17 2 8 8 0 0 1 12-2 10 10 0 0 1 17 4 10 10 0 0 1 18-3a6 6 0 0 1 8 4h-122z' fill='%23ffffff' fill-opacity='0.4'/%3E%3C/svg%3E");
  background-size: 160px 60px;
  height: 50px;
  top: 15px;
  z-index: 1;
  opacity: 0.6;
  animation: parallax-scroll 35s linear infinite;
}

.layer-mountains {
  background-image: url('/assets/union1.svg');
  background-size: 260px auto;
  height: 85px;
  z-index: 2;
  opacity: 0.85;
  animation: parallax-scroll 18s linear infinite;
}

.layer-hills {
  background-image: url('/assets/union2.svg');
  background-size: 200px auto;
  height: 65px;
  z-index: 3;
  opacity: 0.9;
  animation: parallax-scroll 11s linear infinite;
}

.layer-trees {
  background-image: url('/assets/union3.svg');
  background-size: 160px auto;
  height: 48px;
  z-index: 4;
  animation: parallax-scroll 5.5s linear infinite;
}

/* Road Simulation */
.road-container {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 18px;
  background: #475569;
  z-index: 5;
}

.road-stripes {
  width: 300%;
  height: 2px;
  border-bottom: 2px dashed rgba(255, 255, 255, 0.4);
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  animation: parallax-scroll 1.2s linear infinite;
}

@keyframes parallax-scroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-33.333%); }
}

/* Dashboard Card */
.trip-dashboard-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px;
  width: calc(100% - 32px);
  max-width: 343px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.bus-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 20px;
}

.bus-badge span {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.bus-badge i {
  color: #475569;
}

.line-badge {
  background: #e0f2fe;
  color: #0369a1;
  padding: 4px 10px;
  border-radius: 20px;
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
}

.detail-col {
  display: flex;
  flex-direction: column;
}

.detail-col.text-right {
  align-items: flex-end;
}

.detail-label {
  font-family: 'Inter', sans-serif;
  font-size: 10px;
  color: #94a3b8;
  text-transform: uppercase;
  margin-bottom: 2px;
}

.detail-value {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.title-type {
  color: #0f766e;
}

.font-bold {
  font-weight: 800;
}

.dashboard-footer {
  border-top: 1px solid #f1f5f9;
  padding-top: 10px;
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.occupancy-info {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #64748b;
  display: flex;
  align-items: center;
}

/* Action Buttons */
.action-buttons-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.btn-validate {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 52px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  transition: all 0.2s;
}

.btn-validate:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.08);
  border-color: #94a3b8;
}

.btn-validate:active {
  transform: translateY(0);
}

.btn-validate__icon {
  width: 24px;
  height: 24px;
}

.btn-exit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 52px;
  background: #ef4444;
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.2);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-exit:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(239, 68, 68, 0.3);
  background: #dc2626;
}

.btn-exit:active {
  transform: translateY(0);
}

.btn-exit span {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

.btn-exit__icon {
  color: #ffffff;
}

/* QR Code Dialog Card */
.qr-dialog-card {
  width: 320px;
  border-radius: 16px;
}

.qr-dialog-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px 0;
}

.qr-dialog-desc {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.qr-container {
  width: 180px;
  height: 180px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.scanner-line {
  position: absolute;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(to right, transparent, #10b981, transparent);
  box-shadow: 0 0 8px #10b981;
  animation: qr-scan 2s infinite ease-in-out;
}

@keyframes qr-scan {
  0% { top: 0%; }
  50% { top: 100%; }
  100% { top: 0%; }
}

.qr-trip-info {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qr-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.qr-info-item .label {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #64748b;
}

.qr-info-item .value {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}

/* Exit Confirmation Sheet */
.exit-sheet {
  background: #ffffff;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  width: 100%;
  max-width: 100vw;
  padding: 12px 20px 34px;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);
}

.exit-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px 0;
}

.exit-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #64748b;
  margin: 0 0 16px 0;
}

.exit-field {
  width: 100%;
}

.exit-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.btn-exit-confirm {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  border: none;
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.2);
}

.btn-exit-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-exit-cancel {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
</style>