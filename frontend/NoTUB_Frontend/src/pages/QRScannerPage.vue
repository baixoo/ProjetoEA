<template>
  <q-page class="scanner-page">
    <div class="scanner-container">
      <div class="scanner-viewport">
        <!-- Element for html5-qrcode camera stream -->
        <div id="qr-reader" class="scanner-camera-view"></div>
        
        <!-- Fallback static image if camera is not running -->
        <img v-if="!scannerRunning" src="/assets/scanner-bg-fallback.png" alt="" class="scanner-bg-img" />
        
        <div class="scanner-frame">
          <img src="/assets/corner-tl.svg" alt="" class="corner corner--tl" />
          <img src="/assets/corner-tr.svg" alt="" class="corner corner--tr" />
          <img src="/assets/corner-bl.svg" alt="" class="corner corner--bl" />
          <img src="/assets/corner-br.svg" alt="" class="corner corner--br" />
          
          <!-- Animated scanning line -->
          <div class="scanner-line"></div>
        </div>

        <!-- Camera error/unavailability overlay -->
        <div v-if="scannerError" class="scanner-error-overlay">
          <q-icon name="videocam_off" size="32px" color="negative" />
          <span class="error-msg">{{ scannerError }}</span>
        </div>
      </div>

      <q-dialog v-model="titlesDialogOpen" persistent>
        <q-card class="titles-dialog-card">
          <q-card-section class="row items-center q-gutter-sm">
            <q-icon name="confirmation_number" color="primary" size="24px" />
            <div class="text-h6">Sem títulos válidos</div>
          </q-card-section>

          <q-card-section>
            <div>Nao tem bilhetes nem passe ativo para embarcar neste autocarro.</div>
          </q-card-section>

          <q-card-actions align="right" class="q-pa-md q-pt-none">
            <q-btn flat color="grey-7" label="Cancelar" @click="cancelTitlesDialog" />
            <q-btn color="primary" label="Comprar títulos" @click="goToTicketsFromDialog" />
          </q-card-actions>
        </q-card>
      </q-dialog>

      <p class="scanner-instructions">
        Aponte com a câmara do telemóvel para o código QR do autocarro e aguarde.
      </p>

      <div class="guide-floating-container">
        <q-btn round color="primary" icon="help_outline" class="guide-circle-btn">
          <q-menu anchor="top middle" self="bottom middle" class="guide-speech-bubble">
            <div class="q-pa-md guide-bubble-content">
              <h6 class="bubble-title">Como Validar?</h6>
              <div class="guide-steps">
                <div class="step-item">
                  <div class="step-num">1</div>
                  <p class="step-text">Aponte a câmara para o código QR do veículo.</p>
                </div>
                <div class="step-item">
                  <div class="step-num">2</div>
                  <p class="step-text">Selecione o bilhete ou passe a utilizar no ecrã.</p>
                </div>
                <div class="step-item">
                  <div class="step-num">3</div>
                  <p class="step-text">Confirme e boa viagem!</p>
                </div>
              </div>
            </div>
          </q-menu>
        </q-btn>
      </div>

      <div class="scanner-status-panel">
        <div class="scanner-status-row">
          <span class="scanner-status-label">Estado:</span>
          <span class="scanner-status-value">{{ scanStatus }}</span>
        </div>
        <div v-if="lastScannedText" class="scanner-status-row">
          <span class="scanner-status-label">Último QR:</span>
          <span class="scanner-status-value last-qrcode">{{ lastScannedText }}</span>
        </div>
      </div>

      <!-- Info Cards below the instructions -->
      <div class="scanner-info-cards">
        <!-- Títulos Card -->
        <div class="info-card titles-card">
          <div class="card-header">
            <q-icon name="confirmation_number" size="20px" class="card-icon" />
            <span class="card-title">Os Seus Títulos</span>
          </div>
          <div class="card-content">
            <div class="status-item">
              <span class="status-label">Bilhetes Simples:</span>
              <span class="status-badge" :class="unusedTicketsCount > 0 ? 'badge--active' : 'badge--inactive'">
                {{ unusedTicketsCount }} {{ unusedTicketsCount === 1 ? 'bilhete' : 'bilhetes' }}
              </span>
            </div>
            <div class="status-item">
              <span class="status-label">Passe:</span>
              <span class="status-badge" :class="activePassName ? 'badge--active' : 'badge--inactive'">
                {{ activePassName || 'Não ativo' }}
              </span>
            </div>
            <div v-if="activePassZoneLabel" class="status-item pass-zone-item">
              <span class="status-label">Zonas:</span>
              <span class="status-badge badge--active">{{ activePassZoneLabel }}</span>
            </div>
            <div class="status-item points-item">
              <span class="status-label">Pontos Acumulados:</span>
              <span class="points-badge">
                <q-icon name="stars" size="16px" color="amber-8" />
                {{ pointsBalance }} Pts
              </span>
            </div>
          </div>
        </div>

        <!-- Guia Card -->
        <!-- <div class="info-card guide-card">
          <div class="card-header">
            <q-icon name="help_outline" size="20px" class="card-icon" />
            <span class="card-title">Como Validar?</span>
          </div>
          <div class="guide-steps">
            <div class="step-item">
              <div class="step-num">1</div>
              <p class="step-text">Aponte a câmara para o código QR do veículo.</p>
            </div>
            <div class="step-item">
              <div class="step-num">2</div>
              <p class="step-text">Selecione o bilhete ou passe a utilizar no ecrã.</p>
            </div>
            <div class="step-item">
              <div class="step-num">3</div>
              <p class="step-text">Confirme e boa viagem!</p>
            </div>
          </div>
        </div> -->
      </div> 
    </div> 

    <!-- Boarding Dialog / Bottom Sheet (Design 120:804) -->
    <BoardingDialog
      v-model="boardingOpen"
      :bus-number="selectedBusNumber"
      :viagem-veiculo-id="selectedVehicleTrip"
      :paragem-entrada-id="selectedStop"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
// import { useQuasar } from 'quasar'
// import { Notify } from 'quasar'
import { useViagensStore } from 'src/stores/viagens'
import { useTicketsStore } from 'src/stores/tickets'
import { useAuthStore } from 'src/stores/auth'
import BoardingDialog from 'src/components/BoardingDialog.vue'
import { Html5Qrcode } from 'html5-qrcode'

// const $q = useQuasar()
const router = useRouter()
const viagensStore = useViagensStore()
const ticketsStore = useTicketsStore()
const authStore = useAuthStore()

const boardingOpen = ref(false)
const selectedVehicleTrip = ref(null)
const selectedStop = ref(null)
const titlesDialogOpen = ref(false)

const scannerRunning = ref(false)
const scannerError = ref(null)
const lastScannedText = ref('')
const scanStatus = ref('Aguardando QR...')
const scanLocked = ref(false)
const ignoredScanText = ref('')
let html5QrcodeInstance = null

const pointsBalance = computed(() => authStore.user?.nrPontos ?? 0)
const unusedTicketsCount = computed(() => (ticketsStore.tickets || []).filter(t => !t.usado).length)
const activePassName = computed(() => {
  if (!ticketsStore.activePass) return null
  const modalidade = ticketsStore.activePass.modalidade
  if (modalidade === 'MENSAL') return 'Passe Mensal'
  if (modalidade === 'SEMANAL') return 'Passe Semanal'
  if (modalidade === 'ANUAL') return 'Passe Anual'
  if (modalidade === 'H24') return 'Passe 24H'
  if (modalidade === 'H48') return 'Passe 48H'
  if (modalidade === 'H72') return 'Passe 72H'
  return 'Passe Ativo'
})

const activePassZoneLabel = computed(() => {
  if (!ticketsStore.activePass?.zona) return null
  return `Zona ${ticketsStore.activePass.zona.num}`
})

onMounted(async () => {
  // Check if we are already in an active trip
  const active = await viagensStore.fetchActiveTrip()
  if (active) {
    router.push('/traveling')
    return
  }

  // Load vehicles, stops, and user profile
  await Promise.all([
    ticketsStore.fetchMyTickets(),
    ticketsStore.fetchMyPass(),
    viagensStore.fetchStops(),
    viagensStore.fetchVehicleTrips(),
    authStore.fetchUser()
  ])

  // Start the actual HTML5 QR scanner camera
  startScanner()
})

onUnmounted(() => {
  stopScanner()
})

// Restart/stop camera based on boarding dialog visibility
watch(boardingOpen, (isOpen) => {
  if (!isOpen) {
    if (!viagensStore.activeTrip) {
      startScanner()
    }
  } else {
    stopScanner()
  }
})

async function startScanner() {
  scannerError.value = null
  try {
    scanLocked.value = false
    await new Promise(resolve => setTimeout(resolve, 100))

    if (html5QrcodeInstance) {
      await stopScanner()
    }

    html5QrcodeInstance = new Html5Qrcode("qr-reader")
    scanStatus.value = 'A iniciar scanner...'

    const tryStart = async (cameraConfig) => {
      await html5QrcodeInstance.start(
        cameraConfig,
        {
          fps: 15,
          qrbox: { width: 220, height: 220 },
          disableFlip: false,
          aspectRatio: 1.333
        },
        (decodedText) => {
          handleScannedCode(decodedText)
        },
        (errorMessage) => {
          console.debug('QR scan falhou neste frame:', errorMessage)
        }
      )
    }

    try {
      await tryStart({ facingMode: { exact: "environment" } })
    } catch (err) {
      console.warn("Câmara environment não disponível, a tentar outra câmara:", err)
      const cameras = await Html5Qrcode.getCameras()
      if (!cameras || cameras.length === 0) {
        throw err
      }
      await tryStart(cameras[0].id)
    }

    scannerRunning.value = true
    scanStatus.value = 'Scanner activo, a apontar para o QR'
  } catch (err) {
    console.error("Falha ao iniciar câmara:", err)
    if (String(err).toLowerCase().includes('permission')) {
      scannerError.value = "Permissão de câmara negada"
    } else if (String(err).toLowerCase().includes('notfound') || String(err).toLowerCase().includes('nomedia')) {
      scannerError.value = "Câmara não encontrada"
    } else {
      scannerError.value = "Câmara indisponível ou permissão negada"
    }
    scannerRunning.value = false
    html5QrcodeInstance = null
  }
}

async function stopScanner() {
  if (html5QrcodeInstance) {
    try {
      if (html5QrcodeInstance.isScanning) {
        await html5QrcodeInstance.stop()
      }
      scannerRunning.value = false
    } catch (err) {
      console.error("Erro ao parar câmara:", err)
    } finally {
      html5QrcodeInstance = null
    }
  }
}

async function handleScannedCode(text) {
  console.log('handleScannedCode iniciado. Texto:', text)
  if (scanLocked.value) return
  if (ignoredScanText.value && ignoredScanText.value === text) return
  scanLocked.value = true

  lastScannedText.value = text
  let tripMatched = false

  // 1. Tentar JSON
  try {
    const data = JSON.parse(text)
    console.log('Trying match on JSON...')
    const vId = data.viagemVeiculoId || data.veiculoId
    const sId = data.paragemEntradaId || data.paragemId

    const tripMatch = (viagensStore.vehicleTrips || []).find(vt => Number(vt.id) === Number(vId))
    if (tripMatch) {
      selectedVehicleTrip.value = tripMatch.id
      tripMatched = true
      const stopMatch = (viagensStore.stops || []).find(s => Number(s.id) === Number(sId))
      if (stopMatch) selectedStop.value = stopMatch.id
    }
  } catch { 
    console.log('Not Json...') 
  }

  // 2. Tenta dar match com matrícula
  if (!tripMatched) {
    console.log('Trying match on plate...');
    const cleanText = text.trim().toLowerCase();
    
    // Verificamos se vehicleTrips existe antes de usar o find
    console.log('O objeto viagensStore existe?', !!viagensStore);
    console.log('Conteúdo real de vehicleTrips:', JSON.parse(JSON.stringify(viagensStore.vehicleTrips)));
    const trips = viagensStore.vehicleTrips || [];
    const tripByPlate = trips.find(vt => 
      vt.veiculo?.matricula?.toLowerCase() === cleanText
    );

    if (tripByPlate) {
      console.log('✅ Match on plate found:', tripByPlate.veiculo.matricula);
      selectedVehicleTrip.value = tripByPlate.id;
      tripMatched = true;
    }
  }

  // 3. Tenta dar match com paragem
  if (!tripMatched) {
    console.log('Trying match on bus stop name...');
    const stopByName = (viagensStore.stops || []).find(s => 
      s.nome?.toLowerCase() === text.trim().toLowerCase()
    );
    if (stopByName) {
      selectedStop.value = stopByName.id;
      console.log('✅ bus stop identified:', stopByName.nome);
    }
  }

  // Final decision
  if (tripMatched && selectedVehicleTrip.value) {
    console.log('✅  Success. Starting board...');
    await stopScanner()
    triggerBoarding()
  } else {
    console.log('❌ Error: No trip found.');
    scanStatus.value = 'QR not known'
    scanLocked.value = false // Libera para tentar de novo
  }
}

const selectedBusNumber = computed(() => {
  const trip = (viagensStore.vehicleTrips || []).find(vt => vt.id === selectedVehicleTrip.value)
  return trip?.veiculo?.matricula || '728'
})

function triggerBoarding() {
  console.log('triggerBoarding chamado', {
    selectedVehicleTrip: selectedVehicleTrip.value,
    hasTickets: unusedTicketsCount.value > 0,
    hasPass: !!activePassName.value,
    selectedStop: selectedStop.value
  })

  if (!selectedVehicleTrip.value) return

  const hasTickets = unusedTicketsCount.value > 0
  const hasPass = !!activePassName.value

  if (!hasTickets && !hasPass) {
    ignoredScanText.value = lastScannedText.value
    titlesDialogOpen.value = true
    stopScanner()
    return
  }

  if (!selectedStop.value) {
    console.log('Sem paragem, a detetar...')
    detectNearestStop()
    return
  }

  console.log('A abrir boardingOpen!')
  boardingOpen.value = true
}

function cancelTitlesDialog() {
  titlesDialogOpen.value = false
  scanLocked.value = false
  ignoredScanText.value = ''
  startScanner()
}

function goToTicketsFromDialog() {
  titlesDialogOpen.value = false
  scanLocked.value = false
  router.push('/tickets')
}

function detectNearestStop() {
  const trip = (viagensStore.vehicleTrips || []).find(vt => vt.id === selectedVehicleTrip.value)
  const pontos = trip?.trajeto?.pontosDePassagem || []

  if (!pontos.length) {
    fallbackStop()
    return
  }

  const now = new Date()
  const nowMinutes = now.getHours() * 60 + now.getMinutes()

  let nearest = null
  let minDiff = Infinity

  for (const ponto of pontos) {
    if (!ponto.horaChegada || !ponto.paragem?.id) continue
    const [h, m] = ponto.horaChegada.split(':').map(Number)
    const diff = Math.abs((h * 60 + m) - nowMinutes)
    if (diff < minDiff) {
      minDiff = diff
      nearest = ponto
    }
  }

  if (nearest) {
    selectedStop.value = nearest.paragem.id
  } else {
    fallbackStop()
    return
  }

  boardingOpen.value = true
}

function fallbackStop() {
  const trip = (viagensStore.vehicleTrips || []).find(vt => vt.id === selectedVehicleTrip.value)
  const primeiro = trip?.trajeto?.pontosDePassagem?.[0]
  if (primeiro?.paragem?.id) {
    selectedStop.value = primeiro.paragem.id
  }
  boardingOpen.value = true
}

// function haversineKm(lat1, lng1, lat2, lng2) {
//   const R = 6371
//   const toRad = (deg) => deg * Math.PI / 180
//   const dLat = toRad(lat2 - lat1)
//   const dLng = toRad(lng2 - lng1)
//   const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
//   return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
// }

// Expose simulation helper globally for tests/automation
if (typeof window !== 'undefined') {
  window.simulateQRScan = (text) => {
    handleScannedCode(text)
  }
}
</script>

<style scoped>
.scanner-page {
  position: relative;
  min-height: 100vh;
  padding-top: calc(var(--header-h, 42px) + 10px);
  padding-bottom: calc(var(--tabbar-h, 78px) + 16px);
  background: #f7f9fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
}

.scanner-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding: 0 var(--page-pad, 20px);
}

.scanner-viewport {
  position: relative;
  width: 100%;
  max-width: 340px;
  aspect-ratio: 6 / 5;
  border-radius: 16px;
  margin-top: 15px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.10);
  background: #000;
  border: 1px solid #e0e0e0;
}

.scanner-camera-view {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.scanner-camera-view :deep(video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.scanner-camera-view :deep(#qr-shaded-region > div) {
  display: none !important;
}

.scanner-bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  filter: brightness(0.6);
  z-index: 2;
}

.scanner-frame {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 180px;
  height: 180px;
  z-index: 4;
}

.corner {
  position: absolute;
  width: 40px;
  height: 40px;
}

.corner--tl { top: -8px; left: -8px; transform: rotate(-90deg); }
.corner--tr { top: -8px; right: -8px; }
.corner--bl { bottom: -8px; left: -8px; transform: rotate(180deg); }
.corner--br { bottom: -8px; right: -8px; transform: rotate(90deg); }

.scanner-line {
  position: absolute;
  left: 0;
  width: 100%;
  height: 3px;
  background: #01bc74;
  box-shadow: 0 0 8px #01bc74, 0 0 15px rgba(1, 188, 116, 0.8);
  animation: scan-laser 2.5s infinite ease-in-out;
}

@keyframes scan-laser {
  0%   { top: 0%;   opacity: 0.3; }
  15%  { opacity: 1; }
  85%  { opacity: 1; }
  100% { top: 100%; opacity: 0.3; }
}

.scanner-error-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  text-align: center;
  z-index: 5;
}

.error-msg {
  font-size: 13px;
  font-weight: 500;
  color: #121212;
}

.scanner-instructions {
  margin: 12px 0 0;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  color: #757575;
}

/* Status panel */
.scanner-status-panel {
  display: flex;
  width: 100%;
  max-width: 340px;
  flex-direction: column;
  gap: 8px;
  margin: 12px auto;
  padding: 12px 16px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.scanner-status-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  font-size: 13px;
  color: #616161;
}

.scanner-status-label {
  font-weight: 600;
  color: #121212;
}

.last-qrcode {
  overflow-wrap: anywhere;
  text-align: right;
}

/* Info cards */
.scanner-info-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  margin-top: 4px;
}

.info-card {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s;
}

.info-card:hover {
  border-color: rgba(1, 188, 116, 0.4);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.card-icon {
  color: #01bc74;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #121212;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #616161;
}

.status-label {
  color: #757575;
  font-weight: 500;
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
}

.badge--active {
  background: rgba(1, 188, 116, 0.10);
  color: #028e5c;
}

.badge--inactive {
  background: #f0f0f0;
  color: #9e9e9e;
}

.points-item {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px dashed #f0f0f0;
}

.pass-zone-item {
  margin-top: -4px;
}

.points-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #ffb300;
}

/* Help button & bubble */
.guide-floating-container {
  display: flex;
  justify-content: center;
  margin-top: 15px;
  margin-bottom: 15px;
}

.guide-circle-btn {
  width: 45px;
  height: 45px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.10);
  transition: transform 0.2s ease;
}

.guide-circle-btn:active {
  transform: scale(0.95);
}

.guide-speech-bubble {
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.10) !important;
  max-width: 280px;
  border: 1px solid #e0e0e0;
  background-color: white;
}

.guide-bubble-content {
  padding: 16px;
}

.bubble-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 700;
  color: #121212;
}

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.step-num {
  background-color: rgba(1, 188, 116, 0.12);
  color: #028e5c;
  font-weight: 700;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-text {
  margin: 0;
  font-size: 13px;
  color: #616161;
  line-height: 1.4;
}

/* Dialog */
.titles-dialog-card {
  width: min(92vw, 420px);
  border-radius: 16px;
}
</style>

