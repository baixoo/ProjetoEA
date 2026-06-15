<template>
  <q-page class="scanner-page">
    <div class="scanner-container">
      <h5 class="text-weight-bold q-my-md text-center text-primary">Associar Veículo</h5>
      
      <div class="scanner-viewport">
        <div id="qr-reader" class="scanner-camera-view"></div>
        
        <img v-if="!scannerRunning" src="/assets/scanner-bg-fallback.png" alt="" class="scanner-bg-img" />
        
        <div class="scanner-frame">
          <img src="/assets/corner-tl.svg" alt="" class="corner corner--tl" />
          <img src="/assets/corner-tr.svg" alt="" class="corner corner--tr" />
          <img src="/assets/corner-bl.svg" alt="" class="corner corner--bl" />
          <img src="/assets/corner-br.svg" alt="" class="corner corner--br" />
          
          <div class="scanner-line"></div>
        </div>

        <div v-if="scannerError" class="scanner-error-overlay">
          <q-icon name="videocam_off" size="32px" color="negative" />
          <span class="error-msg">{{ scannerError }}</span>
        </div>
      </div>

      <p class="scanner-instructions">
        Motorista, aponte a câmara para o código QR do veículo para iniciar o seu turno.
      </p>

      <div class="scanner-status-panel q-mt-lg">
        <div class="scanner-status-row">
          <span class="scanner-status-label">Estado do Scanner:</span>
          <span class="scanner-status-value text-weight-medium">{{ scanStatus }}</span>
        </div>
      </div>
    </div> 
  </q-page>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'
import { useDriverStore } from 'src/stores/driver'
import { Html5Qrcode } from 'html5-qrcode'

const router = useRouter()
const authStore = useAuthStore()
const driverStore = useDriverStore()

const scannerRunning = ref(false)
const scannerError = ref(null)
const scanStatus = ref('Aguardando QR...')
const scanLocked = ref(false)
let html5QrcodeInstance = null

onMounted(async () => {
  await authStore.fetchUser()
  startScanner()
})

onUnmounted(() => {
  stopScanner()
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
    scanStatus.value = 'A iniciar câmara...'

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
          console.debug('QR scan procurando...', errorMessage)
        }
      )
    }

    try {
      await tryStart({ facingMode: { exact: "environment" } })
    } catch (err) {
      console.warn("Câmara traseira indisponível, a tentar alternativa:", err)
      const cameras = await Html5Qrcode.getCameras()
      if (!cameras || cameras.length === 0) throw err
      await tryStart(cameras[0].id)
    }

    scannerRunning.value = true
    scanStatus.value = 'Scanner ativo. Aponte para o QR do autocarro.'
  } catch (err) {
    console.error("Falha ao iniciar câmara:", err)
    if (String(err).toLowerCase().includes('permission')) {
      scannerError.value = "Permissão de câmara negada"
    } else if (String(err).toLowerCase().includes('notfound') || String(err).toLowerCase().includes('nomedia')) {
      scannerError.value = "Câmara não encontrada"
    } else {
      scannerError.value = "Câmara indisponível"
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
  console.log('QR detetado pelo motorista:', text)
  if (scanLocked.value) return
  scanLocked.value = true

  let veiculoId = null
  let matricula = null

  try {
    const data = JSON.parse(text)
    veiculoId = data.veiculoId || data.viagemVeiculoId
    matricula = data.matricula
  } catch {
    matricula = text.trim()
  }

  try {
    scanStatus.value = 'A validar veículo no sistema...'
    const response = await fetch('/api/driver/veiculos', {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    
    if (response.ok) {
      const vehicles = await response.json()
      let matched = null
      
      if (veiculoId) {
        matched = vehicles.find(v => Number(v.id) === Number(veiculoId))
      }
      if (!matched && matricula) {
        const cleanMat = matricula.toLowerCase().trim()
        matched = vehicles.find(v => v.matricula?.toLowerCase().trim() === cleanMat)
      }
      
      if (matched) {
        await stopScanner()
        
        localStorage.setItem('driver_veiculo_id', matched.id)
        localStorage.setItem('driver_matricula', matched.matricula)

        driverStore.selectedVehicleId = matched.id
        driverStore.matriculaAtiva = matched.matricula
        
        router.push(`/driver/dashboard`)
        return
        }
      
    }
    
    scanStatus.value = 'Veículo não cadastrado nesta empresa'
    scanLocked.value = false
  } catch (e) {
    console.error(e)
    scanStatus.value = 'Erro ao processar validação'
    scanLocked.value = false
  }
}

// Injeção global para testes em ambiente de desenvolvimento/consola
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
  margin: 16px 0 0;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  color: #616161;
}

.scanner-status-panel {
  display: flex;
  width: 100%;
  max-width: 340px;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
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

.scanner-status-value {
  color: #028e5c;
}
</style>