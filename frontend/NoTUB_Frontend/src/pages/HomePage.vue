<template>
  <q-page class="home-page">
    <div class="home-content">
      <!-- Personalized Green Card -->
      <div class="user-card">
        <h2 class="user-card__greeting">Olá, {{ firstName }}!</h2>
        <p class="user-card__sub">Pronto para a sua viagem?</p>

        <div class="user-card__status-section">
          <p class="status-label">A cidade espera por si.</p>
          <div class="status-indicator">
            <q-icon name="check_circle" class="check-icon" />
            <span class="status-text">Viaje com a NoTUB!</span>
          </div>
        </div>

        <div class="bus-icon-container">
          <q-icon name="directions_bus" class="bus-icon" />
        </div>
      </div>

      <div class="section-summary">
        <p class="section-title">Os seus Títulos</p>

        <div class="info-card titles-card">
          <div class="card-header">
            <q-icon name="confirmation_number" size="20px" class="card-icon" />
            <span class="card-title">Os Seus Títulos</span>
          </div>
          
          <div class="card-content">
            <div class="section-title text-green q-mt-sm">
              Bilhetes <q-icon name="local_activity" size="18px" class="q-ml-xs" />
            </div>
            
            <div 
              v-for="zona in bilhetesPorZona" 
              :key="zona.numero" 
              class="status-item"
              :class="{ 'text-grey-6': zona.quantidade === 0, 'text-green font-bold': zona.quantidade > 0 }"
            >
              <span class="status-label">Zona {{ zona.numero }}:</span>
              <span class="status-value">{{ zona.quantidade }}</span>
            </div>

            <q-separator class="q-my-md bg-green-2" />

            <div v-if="activePassName" class="section-title text-green">
              {{ activePassName }}
            </div>

            <div v-else class="section-title text-green">
              Passe
            </div>
            
            <div v-if="dadosPasse && dadosPasse.ativo" class="status-item text-green font-bold">
              <span class="status-label">Zona: {{ dadosPasse.zona }}</span>
              <span class="status-value">Válido até: {{ dadosPasse.validade }}</span>
            </div>
            
            <div v-else class="status-item text-grey-6">
              <span class="status-label">Não possui passe</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Section -->
      <div class="action-section">
        <p class="section-title">Para onde vamos?</p>

        <div class="destination-box" @click="router.push('/routes')">
          <div class="destination-search">
            <q-icon name="search" class="search-icon" />
            <span class="search-text">Pesquisar destino ou paragem</span>
          </div>
        </div>

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

        <button class="btn-start q-mt-lg" @click="triggerBoarding">
          <span>Iniciar Viagem</span>
        </button>
      </div>
    </div>

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

const bilhetesPorZona = computed(() => {
  const zonasMap = {
    '1': { numero: '1', quantidade: 0 },
    '2': { numero: '2', quantidade: 0 },
    '3': { numero: '3', quantidade: 0 }
  }

  const listaTickets = ticketsStore.tickets || []
  listaTickets.forEach(t => {
    if (t.tipo === 'BILHETE' && !t.usado) {
      const zonaNum = String(t.zona?.num) 
      
      if (zonasMap[zonaNum]) {
        zonasMap[zonaNum].quantidade++
      } else if (t.zona?.num) {
        zonasMap[zonaNum] = { numero: zonaNum, quantidade: 1 }
      }
    }
  })

  return Object.values(zonasMap).sort((a, b) => Number(a.numero) - Number(b.numero))
})

const dadosPasse = computed(() => {
  if (!ticketsStore.activePass) return { ativo: false }

  const validadePasse = ticketsStore.activePass.fim
  const zonaPasse = ticketsStore.activePass.zona 

  let dataFormatada = 'Não definida'
  if (validadePasse) {
    const d = new Date(validadePasse)
    dataFormatada = d.toLocaleDateString('pt-PT') 
  }

  return {
    ativo: true,
    zona: zonaPasse?.num || 'X', 
    validade: dataFormatada
  }
})

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


const firstName = computed(() => {
  if (authStore.user?.primeiroNome) return authStore.user.primeiroNome
  return ''
})

// const activePassName = computed(() => {
//   if (!ticketsStore.activePass) return null
//   const modalidade = ticketsStore.activePass.modalidade
//   if (modalidade === 'MENSAL') return 'Passe Mensal'
//   if (modalidade === 'SEMANAL') return 'Passe Semanal'
//   if (modalidade === 'ANUAL') return 'Passe Anual'
//   if (modalidade === 'H24') return 'Passe 24H'
//   if (modalidade === 'H48') return 'Passe 48H'
//   if (modalidade === 'H72') return 'Passe 72H'
//   return 'Passe Ativo'
// })

// const activePassZoneLabel = computed(() => {
//   if (!ticketsStore.activePass?.zona) return null
//   return `Zona ${ticketsStore.activePass.zona.num}`
// })

// const activePassExpiryLabel = computed(() => {
//   const expiry = ticketsStore.activePass?.fim
//   if (!expiry) return null
//   const date = new Date(expiry)
//   if (Number.isNaN(date.getTime())) return null
//   return new Intl.DateTimeFormat('pt-PT', {
//     day: '2-digit',
//     month: '2-digit',
//     year: 'numeric'
//   }).format(date)
// })

// const unusedTicketZoneGroups = computed(() => {
//   const counts = {}
//   unusedTickets.value.forEach(ticket => {
//     const zoneNum = ticket?.zona?.num
//     const zoneLabel = zoneNum != null ? `Zona ${zoneNum}` : 'Zona -'
//     counts[zoneLabel] = (counts[zoneLabel] || 0) + 1
//   })
//   return Object.entries(counts).map(([zoneLabel, count]) => ({ zoneLabel, count }))
// })

// const unusedTickets = computed(() => (ticketsStore.tickets || []).filter(t => !t.usado))

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

  if (viagensStore.vehicleTrips?.length > 0) {
    selectedVehicleTrip.value = viagensStore.vehicleTrips[0].id
  }
  if (viagensStore.stops?.length > 0) {
    selectedStop.value = viagensStore.stops[0].id
  }
})

const stopOptions = computed(() => {
  return (viagensStore.stops || []).map(s => ({
    label: s.nome,
    value: s.id
  }))
})

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
  background: #f8f9fa;
}

.home-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
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
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #616161;
}

.info-card .status-label {
  color: #757575;
  font-weight: 500;
  font-size: 13px;
  text-transform: none;
}

.status-value {
  font-weight: 600;
  font-size: 13px;
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

.section-summary {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.titles-card {
  width: 100%;
  background: #fff;
  border: 1px solid #d7d6d6;
  border-radius: 12px;
  padding: 16px;
  display: grid;
  gap: 14px;
}

.title-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title-label {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #505050;
  text-transform: uppercase;
}

.title-value {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #121212;
}

.title-meta {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #737373;
}

.zone-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.zone-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #3c3c3c;
}

.zone-count {
  font-weight: 700;
}

.inactive-block {
  border-top: 1px solid #e2e2e2;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inactive-heading {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #9b9b9b;
  text-transform: uppercase;
}

.inactive-item {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #4f4f4f;
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
