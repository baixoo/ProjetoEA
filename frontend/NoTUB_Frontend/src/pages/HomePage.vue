<template>
  <q-page class="home-page">
    <div class="home-content">
      <!-- Personalized Green Card (Design 162:357) -->
      <div class="user-card">
        <h2 class="user-card__greeting">Olá, {{ firstName }}!</h2>
        <p class="user-card__sub">Pronto para a sua viagem?</p>

        <div class="user-card__status-section">
          <p class="status-label">Estado Atual</p>
          <div class="status-indicator">
            <q-icon name="check_circle" class="check-icon" />
            <span class="status-text">{{ currentStatusText }}</span>
          </div>
        </div>

        <!-- Bus Icon (Design 162:374) -->
        <div class="bus-icon-container">
          <q-icon name="directions_bus" class="bus-icon" />
        </div>
      </div>

      <!-- Action Section -->
      <div class="action-section">
        <p class="section-title">Para onde vamos?</p>

        <!-- Destination Selector -->
        <div class="destination-box" @click="router.push('/routes')">
          <div class="destination-search">
            <q-icon name="search" class="search-icon" />
            <span class="search-text">Pesquisar destino ou paragem</span>
          </div>
        </div>

        <!-- Boarding Options Simulator -->
        <div class="boarding-simulator q-mt-md">
          <p class="simulator-label">Simulador de Embarque Rápido</p>
          
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <q-select
                v-model="selectedVehicleTrip"
                :options="vehicleTripOptions"
                label="Veículo / Rota"
                dense
                outlined
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <q-select
                v-model="selectedStop"
                :options="stopOptions"
                label="Paragem Entrada"
                dense
                outlined
                emit-value
                map-options
              />
            </div>
          </div>
        </div>

        <!-- Iniciar Button -->
        <button class="btn-start q-mt-lg" @click="triggerBoarding">
          <span>Iniciar Viagem</span>
        </button>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'
import { useTicketsStore } from 'src/stores/tickets'
import { useViagensStore } from 'src/stores/viagens'
import BoardingDialog from 'src/components/BoardingDialog.vue'

const router = useRouter()
const authStore = useAuthStore()
const ticketsStore = useTicketsStore()
const viagensStore = useViagensStore()

const boardingOpen = ref(false)
const selectedVehicleTrip = ref(null)
const selectedStop = ref(null)

const firstName = computed(() => {
  if (authStore.user?.primeiroNome) return authStore.user.primeiroNome
  return 'Rui'
})

onMounted(async () => {
  // Check if we are already in an active trip
  const active = await viagensStore.fetchActiveTrip()
  if (active) {
    router.push('/traveling')
    return
  }

  // Load backend metadata & ticket counts
  await Promise.all([
    ticketsStore.fetchMyTickets(),
    ticketsStore.fetchMyPass(),
    viagensStore.fetchStops(),
    viagensStore.fetchVehicleTrips()
  ])

  // Select defaults if metadata is populated
  if (viagensStore.vehicleTrips?.length > 0) {
    selectedVehicleTrip.value = viagensStore.vehicleTrips[0].id
  }
  if (viagensStore.stops?.length > 0) {
    selectedStop.value = viagensStore.stops[0].id
  }
})

// Current status string
const currentStatusText = computed(() => {
  if (ticketsStore.activePass) {
    return 'Passe Ativo'
  }
  const unusedCount = (ticketsStore.tickets || []).filter(t => !t.usado).length
  if (unusedCount > 0) {
    return `${unusedCount} Bilhetes Disponíveis`
  }
  return 'Sem Títulos Ativos'
})

// Format stops for q-select
const stopOptions = computed(() => {
  return (viagensStore.stops || []).map(s => ({
    label: s.nome,
    value: s.id
  }))
})

// Format vehicle trips for q-select
const vehicleTripOptions = computed(() => {
  return (viagensStore.vehicleTrips || []).map(vt => ({
    label: `${vt.veiculo?.matricula || 'Autocarro'} - ${vt.trajeto?.linha?.nome || 'Rota'}`,
    value: vt.id,
    matricula: vt.veiculo?.matricula
  }))
})

const selectedBusNumber = computed(() => {
  const trip = (viagensStore.vehicleTrips || []).find(vt => vt.id === selectedVehicleTrip.value)
  return trip?.veiculo?.matricula || '728'
})

function triggerBoarding() {
  if (!selectedVehicleTrip.value || !selectedStop.value) {
    // If no values from backend, use fallback IDs to guarantee simulation works
    selectedVehicleTrip.value = selectedVehicleTrip.value || 1
    selectedStop.value = selectedStop.value || 1
  }
  boardingOpen.value = true
}
</script>

<style scoped>
.home-page {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: calc(var(--header-h, 42px) + 24px) var(--page-pad, 20px) calc(var(--tabbar-h, 78px) + 16px);
  background: #f0f0f0;
}

.home-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.user-card {
  width: 100%;
  min-height: 150px;
  border-radius: var(--radius-card, 12px);
  background: linear-gradient(135deg, rgba(3,111,69,1) 0%, rgba(1,157,97,1) 100%);
  padding: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(1, 157, 97, 0.25);
}

.user-card__greeting {
  font-family: 'Inter', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  line-height: normal;
}

.user-card__sub {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.8);
  margin: 2px 0 0 0;
  line-height: normal;
}

.user-card__status-section {
  position: absolute;
  left: 16px;
  bottom: 12px;
}

.status-label {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 4px 0;
  text-transform: uppercase;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.check-icon {
  font-size: 18px;
  color: #82fda2;
}

.status-text {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 400;
  color: #82fda2;
}

.bus-icon-container {
  position: absolute;
  right: 15px;
  bottom: 15px;
  transform: rotate(16deg);
}

.bus-icon {
  font-size: 32px;
  color: #fff;
}

/* Action Section */
.action-section {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: #000;
  margin: 0 0 10px 0;
}

.destination-box {
  background: #fff;
  border: 1px solid #d7d6d6;
  border-radius: 5px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.destination-search {
  background: #f0f0f0;
  border-radius: 5px;
  height: 38px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.destination-search:hover {
  background: #e5e5e5;
}

.search-icon {
  font-size: 18px;
  color: #505050;
}

.search-text {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #505050;
}

.boarding-simulator {
  background: #fff;
  border: 1px solid #d7d6d6;
  border-radius: 5px;
  padding: 10px;
}

.simulator-label {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #505050;
  margin: 0 0 8px 0;
}

.btn-start {
  width: 100%;
  height: 50px;
  border-radius: 8px;
  border: none;
  background: #1876d2;
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(24, 118, 210, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-start:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(24, 118, 210, 0.4);
}

.btn-start:active {
  transform: translateY(0);
}
</style>
