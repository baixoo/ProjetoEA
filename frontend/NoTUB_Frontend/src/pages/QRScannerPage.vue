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

      <p class="scanner-instructions">
        Aponte com a câmara do telemóvel para o código QR do autocarro e aguarde.
      </p>

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
              <span class="status-label">Passe Social:</span>
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
        <div class="info-card guide-card">
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
              <p class="step-text">Confirme e boa viagem! A animação começará de seguida.</p>
            </div>
          </div>
        </div>
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
import { useQuasar } from 'quasar'
import { useViagensStore } from 'src/stores/viagens'
import { useTicketsStore } from 'src/stores/tickets'
import { useAuthStore } from 'src/stores/auth'
import BoardingDialog from 'src/components/BoardingDialog.vue'
import { Html5Qrcode } from 'html5-qrcode'

const $q = useQuasar()
const router = useRouter()
const viagensStore = useViagensStore()
const ticketsStore = useTicketsStore()
const authStore = useAuthStore()

const boardingOpen = ref(false)
const selectedVehicleTrip = ref(null)
const selectedStop = ref(null)

const scannerRunning = ref(false)
const scannerError = ref(null)
const lastScannedText = ref('')
const scanStatus = ref('Aguardando QR...')
const scanLocked = ref(false)
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
  if (scanLocked.value) return
  scanLocked.value = true

  console.log("QR Code lido com sucesso:", text)
  lastScannedText.value = text
  scanStatus.value = 'QR lido, a selecionar bilhete...'
  $q.notify({ type: 'positive', message: `QR lido: ${text}`, position: 'top', timeout: 2000 })

  let tripMatched = false

  // 1. Try to parse as JSON first
  try {
    const data = JSON.parse(text)
    if (data.viagemVeiculoId || data.veiculoId) {
      const vId = data.viagemVeiculoId || data.veiculoId
      const sId = data.paragemEntradaId || data.paragemId

      const tripMatch = (viagensStore.vehicleTrips || []).find(vt => vt.id === Number(vId) || vt.veiculo?.id === Number(vId))
      const stopMatch = (viagensStore.stops || []).find(s => s.id === Number(sId))

      if (tripMatch) selectedVehicleTrip.value = tripMatch.id
      if (stopMatch) selectedStop.value = stopMatch.id

      if (tripMatch) tripMatched = true
    }
  } catch {
    // Not a JSON
  }

  // 2. Try to parse as URL parameters
  if (!tripMatched) {
    try {
      if (text.includes('?') || text.includes('&')) {
        const urlParams = new URLSearchParams(text.split('?')[1] || text)
        const vId = urlParams.get('viagemVeiculoId') || urlParams.get('veiculo') || urlParams.get('viagem')
        const sId = urlParams.get('paragemEntradaId') || urlParams.get('paragem') || urlParams.get('entrada')

        if (vId) {
          const tripMatch = (viagensStore.vehicleTrips || []).find(vt => vt.id === Number(vId) || vt.veiculo?.id === Number(vId) || vt.veiculo?.matricula === vId)
          if (tripMatch) selectedVehicleTrip.value = tripMatch.id

          if (sId) {
            const stopMatch = (viagensStore.stops || []).find(s => s.id === Number(sId) || s.nome?.toLowerCase() === sId.toLowerCase())
            if (stopMatch) selectedStop.value = stopMatch.id
          }

          if (tripMatch) tripMatched = true
        }
      }
    } catch {
      // Not URL
    }
  }

  // 3. Try comma-separated values: "viagemVeiculoId,paragemEntradaId" or just "viagemVeiculoId"
  if (!tripMatched) {
    const parts = text.split(',')
    if (parts.length >= 1) {
      const vId = parseInt(parts[0].trim(), 10)
      if (!isNaN(vId)) {
        const tripMatch = (viagensStore.vehicleTrips || []).find(vt => vt.id === vId)
        if (tripMatch) {
          selectedVehicleTrip.value = tripMatch.id
          if (parts.length >= 2) {
            const sId = parseInt(parts[1].trim(), 10)
            if (!isNaN(sId)) {
              const stopMatch = (viagensStore.stops || []).find(s => s.id === sId)
              if (stopMatch) selectedStop.value = stopMatch.id
            }
          }
          tripMatched = true
        }
      }
    }
  }

  // 4. If raw text matches a stop name or vehicle plate directly
  if (!tripMatched) {
    const tripByPlate = (viagensStore.vehicleTrips || []).find(vt => vt.veiculo?.matricula?.toLowerCase() === text.trim().toLowerCase())
    if (tripByPlate) {
      selectedVehicleTrip.value = tripByPlate.id
      tripMatched = true
    }
  }

  if (!tripMatched) {
    const stopByName = (viagensStore.stops || []).find(s => s.nome?.toLowerCase() === text.trim().toLowerCase())
    if (stopByName) {
      selectedStop.value = stopByName.id
      // Keep scanning until a vehicle trip is also identified
    }
  }

  if (selectedVehicleTrip.value) {
    await stopScanner()
    triggerBoarding()
    return
  }

  scanStatus.value = 'QR lido mas não corresponde a viagem válida'
  $q.notify({ type: 'warning', message: 'QR lido mas não corresponde a uma viagem válida.', position: 'top', timeout: 3000 })
  scanLocked.value = false
}



const selectedBusNumber = computed(() => {
  const trip = (viagensStore.vehicleTrips || []).find(vt => vt.id === selectedVehicleTrip.value)
  return trip?.veiculo?.matricula || '728'
})

function triggerBoarding() {
  if (!selectedVehicleTrip.value) return

  if (!selectedStop.value) {
    detectNearestStop()
    return
  }

  boardingOpen.value = true
}

function detectNearestStop() {
  if (!navigator.geolocation) {
    fallbackStop()
    return
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const userLat = pos.coords.latitude
      const userLng = pos.coords.longitude
      const allStops = viagensStore.stops || []

      let nearest = null
      let minDist = Infinity

      for (const stop of allStops) {
        const loc = stop.localizacao
        if (!loc) continue
        const d = haversineKm(userLat, userLng, loc.latitude, loc.longitude)
        if (d < minDist) {
          minDist = d
          nearest = stop
        }
      }

      if (nearest) {
        selectedStop.value = nearest.id
        $q.notify({ type: 'info', message: `Paragem detetada: ${nearest.nome}`, position: 'top', timeout: 3000 })
      } else {
        fallbackStop()
      }

      boardingOpen.value = true
    },
    () => {
      fallbackStop()
    },
    { timeout: 5000, enableHighAccuracy: true }
  )
}

function fallbackStop() {
  const first = (viagensStore.stops || [])[0]
  if (first) {
    selectedStop.value = first.id
    $q.notify({ type: 'warning', message: 'Localização indisponível, paragem preenchida automaticamente', position: 'top', timeout: 3000 })
  }
  boardingOpen.value = true
}

function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371
  const toRad = (deg) => deg * Math.PI / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

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
  background: #121212;
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

.scanner-viewport-fullscreen {
  display: none;
}

.scanner-viewport {
  position: relative;
  width: 100%;
  max-width: 340px;
  aspect-ratio: 6 / 5;
  border-radius: 12px;
  margin-top: 15px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  background: #000;
}

.scanner-camera-view {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* Force video to fill container properly */
.scanner-camera-view :deep(video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

/* Hide html5-qrcode default white corner borders */
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

.scanner-subtract {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  opacity: 0.3;
  z-index: 3;
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

.corner--tl {
  top: -8px;
  left: -8px;
  transform: rotate(-90deg);
}

.corner--tr {
  top: -8px;
  right: -8px;
}

.corner--bl {
  bottom: -8px;
  left: -8px;
  transform: rotate(180deg);
}

.corner--br {
  bottom: -8px;
  right: -8px;
  transform: rotate(90deg);
}

/* Animated premium laser line scanner */
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
  0% {
    top: 0%;
    opacity: 0.3;
  }
  15% {
    opacity: 1;
  }
  85% {
    opacity: 1;
  }
  100% {
    top: 100%;
    opacity: 0.3;
  }
}

.scanner-error-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
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
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  opacity: 0.9;
}

.scanner-instructions {
  margin: 15px 0;
  padding: 0 20px;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
}

.scanner-instruction-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.92), transparent);
  padding: 30px 20px 18px;
  text-align: center;
  z-index: 10;
}

.scanner-instruction-text {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 500;
  color: #fff;
  margin: 0;
}

.scanner-info-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  margin-top: 10px;
}

.info-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 16px;
  transition: transform 0.2s, border-color 0.2s;
}

.info-card:hover {
  border-color: rgba(1, 188, 116, 0.4);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 8px;
}

.card-icon {
  color: #01bc74;
}

.card-title {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
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
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
}

.badge--active {
  background: rgba(1, 188, 116, 0.15);
  color: #01bc74;
}

.badge--inactive {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.4);
}

.points-item {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px dashed rgba(255, 255, 255, 0.08);
}

.pass-zone-item {
  padding-top: 0;
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

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scanner-status-panel {
  display: flex;
  width: 100%;
  max-width: 340px;
  flex-direction: column;
  gap: 8px;
  margin: 0 auto 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.scanner-status-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.scanner-status-label {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.last-qrcode {
  overflow-wrap: anywhere;
  text-align: right;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #01bc74;
  color: #121212;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-text {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}


</style>

