<template>
  <q-dialog
    v-model="isOpen"
    position="bottom"
    class="boarding-dialog"
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <div class="boarding-sheet">
      <div class="drag-handle"></div>

      <div class="boarding-header">
        <q-icon name="directions_bus" class="bus-header-icon" />
        <div class="boarding-header__info">
          <span class="boarding-header__plate">{{ busNumber || '---' }}</span>
          <span v-if="linhaNome" class="boarding-header__line">{{ linhaNome }}</span>
          <span v-if="stopName" class="boarding-header__stop">Embarque: {{ stopName }}</span>
        </div>
      </div>

      <div v-if="loading" class="flex flex-center q-py-md">
        <q-spinner color="primary" size="30px" />
        <span class="q-ml-sm text-grey-7">A validar...</span>
      </div>

      <div v-else class="boarding-options">
        <div v-if="showRoutePreview" class="route-preview-panel">
          <div class="route-preview-header">
            <q-icon name="alt_route" color="green-8" size="24px" />
            <div>
              <div class="route-preview-title">Confirmar embarque</div>
              <div class="route-preview-subtitle">{{ routePreviewLineLabel }}</div>
            </div>
          </div>

          <div class="route-preview-rail">
            <template v-for="(item, index) in routePreviewItems" :key="item.key">
              <div v-if="item.type === 'ellipsis'" class="route-stop-ellipsis">- - -&gt;</div>
              <div v-else class="route-stop" :class="{
                'route-stop--first': item.kind === 'first',
                'route-stop--current': item.kind === 'current',
                'route-stop--last': item.kind === 'last',
                'route-stop--middle': item.kind === 'middle'
              }">
                <div class="route-stop__dot"></div>
                <span class="route-stop__name">{{ item.name }}</span>
              </div>
              <div v-if="index < routePreviewItems.length - 1" class="route-connector"></div>
            </template>
          </div>

          <div class="route-preview-actions">
            <q-btn flat color="grey-7" label="Cancelar" @click="closeDialog" />
            <q-btn color="green-8" label="Continuar" @click="showRoutePreview = false" />
          </div>
        </div>

        <template v-else>
          <q-btn flat dense icon="arrow_back" label="Voltar à câmara" class="back-camera-btn" @click="closeDialog" />
          <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

          <button
            v-if="hasActivePass"
            class="option-card"
            :class="{ 'option-card--selected': selectedType === 'passe' }"
            @click="selectedType = 'passe'"
            :disabled="submitting"
          >
            <div class="option-icon-container bg-pass">
              <q-icon name="credit_card" class="option-icon" />
            </div>
            <div class="option-details">
              <span class="option-title">Usar Passe Ativo</span>
              <span class="option-subtitle">Zona: {{ activePass?.zona?.num || '-' }}</span>
            </div>
            <q-icon v-if="selectedType === 'passe'" name="check_circle" class="selected-icon" />
          </button>

          <button
            v-if="unusedTicketsCount > 0"
            class="option-card"
            :class="{ 'option-card--selected': selectedType === 'bilhete' }"
            @click="selectedType = 'bilhete'"
            :disabled="submitting"
          >
            <div class="option-icon-container bg-ticket">
              <q-icon name="confirmation_number" class="option-icon" />
            </div>
            <div class="option-details">
              <span class="option-title">Usar Bilhete{{ ticketQty > 1 ? 's' : '' }}</span>
              <span class="option-subtitle">Restam {{ unusedTicketsCount }} bilhetes</span>
            </div>
            <q-icon v-if="selectedType === 'bilhete'" name="check_circle" class="selected-icon" />
          </button>

          <div v-if="selectedType === 'bilhete' && unusedTicketsCount > 1" class="qty-row">
            <span class="qty-label">Quantidade:</span>
            <q-btn round flat dense icon="remove" size="sm" @click="ticketQty = Math.max(1, ticketQty - 1)" />
            <span class="qty-value">{{ ticketQty }}</span>
            <q-btn round flat dense icon="add" size="sm" @click="ticketQty = Math.min(unusedTicketsCount, ticketQty + 1)" />
          </div>

          <div v-if="!hasActivePass && unusedTicketsCount === 0" class="no-tickets-warning">
            <p class="warning-message">Nao tem bilhetes ou passes ativos disponiveis.</p>
            <q-btn color="positive" label="Comprar na Loja" class="full-width q-mt-sm" @click="goToShop" />
          </div>

          <button
            v-if="selectedType"
            class="btn-start"
            @click="confirmSelection"
            :disabled="submitting"
          >
            <q-spinner v-if="submitting" size="18px" class="q-mr-sm" />
            {{ submitting ? 'A iniciar...' : 'Confirmar Embarque' }}
          </button>
        </template>
      </div>
    </div>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketsStore } from 'src/stores/tickets'
import { useViagensStore } from 'src/stores/viagens'
import { useAuthStore } from 'src/stores/auth'

const useAuthStore_inst = useAuthStore()

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  busNumber: { type: String, default: '' },
  viagemVeiculoId: { type: [Number, String], default: null },
  paragemEntradaId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'tripStarted'])
const router = useRouter()
const ticketsStore = useTicketsStore()
const viagensStore = useViagensStore()

const isOpen = ref(props.modelValue)
const loading = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const selectedType = ref(null)
const ticketQty = ref(1)
const showRoutePreview = ref(true)

const linhaNome = computed(() => {
  const vt = (viagensStore.vehicleTrips || []).find(v => v.id == props.viagemVeiculoId)
  return vt?.trajeto?.linha?.nome || ''
})

const stopName = computed(() => {
  if (!props.paragemEntradaId) return ''
  const stop = (viagensStore.stops || []).find(s => s.id == props.paragemEntradaId)
  return stop?.nome || ''
})

const selectedTrip = computed(() => {
  return (viagensStore.vehicleTrips || []).find(v => v.id == props.viagemVeiculoId) || null
})

const routePreviewLineLabel = computed(() => {
  return selectedTrip.value?.trajeto?.linha?.nome || ''
})

const routePreviewItems = computed(() => {
  const pontos = [...(selectedTrip.value?.trajeto?.pontosDePassagem || [])]
    .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
    .map(ponto => ({ id: ponto.paragem?.id ?? null, name: ponto.paragem?.nome || 'Paragem' }))
    .filter(stop => stop.id !== null)

  if (!pontos.length) {
    return stopName.value
      ? [{ key: 'current-fallback', type: 'stop', kind: 'current', name: stopName.value }]
      : []
  }

  const currentIndex = pontos.findIndex(s => Number(s.id) === Number(props.paragemEntradaId))
  const cur = currentIndex >= 0 ? currentIndex : 0
  const last = pontos[pontos.length - 1]

  const items = []
  const pushStop = (stop, kind) => items.push({ key: `${kind}-${stop.id}`, type: 'stop', kind, name: stop.name })
  const pushEllipsis = (key) => items.push({ key, type: 'ellipsis' })

  pushStop(pontos[cur], 'current')

  let added = 0
  for (let i = cur + 1; i < pontos.length - 1 && added < 2; i++) {
    pushStop(pontos[i], 'middle')
    added++
  }

  const lastShownIndex = cur + 1 + added - 1
  if (lastShownIndex < pontos.length - 2) {
    pushEllipsis('ellipsis-right')
  }

  // Última paragem sempre (se não for a atual)
  if (last.id !== pontos[cur].id) {
    pushStop(last, 'last')
  }

  return items
})

watch(() => props.modelValue, (val) => {
  if (val) {
    console.log('BoardingDialog abriu. Props:', {
      viagemVeiculoId: props.viagemVeiculoId,
      paragemEntradaId: props.paragemEntradaId,
      busNumber: props.busNumber
    })
    console.log('selectedTrip encontrado:', selectedTrip.value)
    console.log('routePreviewItems:', routePreviewItems.value)
  }
  isOpen.value = val
  if (val) {
    showRoutePreview.value = true
    loadTitulos()
  }
})

watch(isOpen, (val) => emit('update:modelValue', val))

async function loadTitulos() {
  loading.value = true
  errorMsg.value = ''
  selectedType.value = null
  ticketQty.value = 1
  try {
    await Promise.all([ticketsStore.fetchMyTickets(), ticketsStore.fetchMyPass()])
  } catch {
    errorMsg.value = 'Erro ao carregar titulos.'
  } finally {
    loading.value = false
  }
}

onMounted(() => { if (isOpen.value) loadTitulos() })

const activePass = computed(() => ticketsStore.activePass)
const hasActivePass = computed(() => !!activePass.value)
const unusedTickets = computed(() => (ticketsStore.tickets || []).filter(t => !t.usado))
const unusedTicketsCount = computed(() => unusedTickets.value.length)

function closeDialog() {
  isOpen.value = false
}

async function confirmSelection() {
  submitting.value = true
  errorMsg.value = ''
  try {
    if (!hasActivePass.value && unusedTicketsCount.value === 0) {
      errorMsg.value = 'Nao tem bilhetes nem passe ativos disponiveis.'
      return
    }

    if (!selectedType.value) {
      errorMsg.value = 'Selecione um bilhete ou passe para continuar.'
      return
    }

    if (props.paragemEntradaId) {
      try {
        const pos = await new Promise((resolve) => {
          if (!navigator.geolocation) {
            resolve(null)
            return
          }
          navigator.geolocation.getCurrentPosition(
            p => resolve({ lat: p.coords.latitude, lng: p.coords.longitude }),
            () => resolve(null),
            { timeout: 5000 }
          )
        })

        if (pos) {
          const geoResp = await fetch('/api/geo/verificar', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${useAuthStore_inst.token}`
            },
            body: JSON.stringify({
              paragemId: props.paragemEntradaId,
              latitude: pos.lat,
              longitude: pos.lng
            })
          })
          if (geoResp.ok) {
            const geoData = await geoResp.json()
            if (!geoData.proximo) {
              errorMsg.value = 'Nao esta proximo da paragem selecionada. A verificacao geografica falhou.'
              submitting.value = false
              return
            }
          }
        }
      } catch (e) {
        console.warn('Geo verification skipped:', e)
      }
    }

    if (selectedType.value === 'passe') {
      await viagensStore.startTrip(activePass.value.id, props.paragemEntradaId, props.viagemVeiculoId)
    } else if (selectedType.value === 'bilhete') {
      for (let i = 0; i < ticketQty.value; i++) {
        const ticket = unusedTickets.value[i]
        if (!ticket) break
        await viagensStore.startTrip(ticket.id, props.paragemEntradaId, props.viagemVeiculoId)
      }
    }
    emit('tripStarted', { message: 'Viagem iniciada com sucesso!' })
    isOpen.value = false
    router.push('/traveling')
  } catch (e) {
    errorMsg.value = e.message || 'Erro ao iniciar viagem.'
  } finally {
    submitting.value = false
  }
}

function goToShop() {
  isOpen.value = false
  router.push('/tickets')
}
</script>

<script>
export default { name: 'BoardingDialog' }
</script>

<style scoped>
.boarding-dialog :deep(.q-dialog__inner) {
  padding: 0;
}

.boarding-sheet {
  background: #f0f0f0;
  border-top-left-radius: 15px;
  border-top-right-radius: 15px;
  width: 100%;
  max-width: 100vw;
  padding: 12px 24px 34px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.drag-handle {
  width: 73px;
  height: 5px;
  background: #bfbfbf;
  border-radius: 100px;
  margin-bottom: 20px;
}

.boarding-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 12px;
  background: #e6f7f0;
  border-radius: 10px;
  padding: 10px 14px;
}

.bus-header-icon {
  font-size: 24px;
  color: #028e5c;
}

.boarding-header__info {
  display: flex;
  flex-direction: column;
}

.boarding-header__plate {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #0b1a16;
}

.boarding-header__line {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #028e5c;
}

.boarding-header__stop {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #6c757d;
}

.boarding-question {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 400;
  color: #505050;
  align-self: flex-start;
  margin: 0 0 16px 3px;
}

.boarding-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.error-text {
  color: #d32f2f;
  font-size: 13px;
  margin: 0 0 8px;
  text-align: center;
}

.option-card {
  background: #fff;
  border: 2px solid #d7d6d6;
  border-radius: 10px;
  height: 65px;
  width: 100%;
  padding: 6px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.option-card:hover {
  border-color: #028e5c;
}

.option-card--selected {
  border-color: #028e5c;
  box-shadow: 0 2px 8px rgba(2, 142, 92, 0.15);
}

.option-icon-container {
  width: 41px;
  height: 41px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-pass { background: #eae1cc; }
.bg-ticket { background: #bbd3ff; }

.option-icon {
  font-size: 24px;
  color: #333;
}

.option-details {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.option-title {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 500;
  color: #000;
}

.option-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #505050;
}

.selected-icon {
  font-size: 22px;
  color: #028e5c;
}

.qty-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.qty-label {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #495057;
  font-weight: 600;
}

.qty-value {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  min-width: 24px;
  text-align: center;
}

.btn-start {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #028e5c 0%, #01bc74 100%);
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(2, 142, 92, 0.2);
  transition: transform 0.2s;
  margin-top: 4px;
}

.btn-start:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-start:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.no-tickets-warning {
  background: #fff;
  border: 1px solid #d7d6d6;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  width: 100%;
}

.warning-message {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #505050;
  margin: 0;
}

.route-preview-panel {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  width: 100%;
}

.route-preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.route-preview-title {
  font-size: 14px;
  font-weight: 700;
  color: #121212;
}

.route-preview-subtitle {
  font-size: 12px;
  color: #028e5c;
  font-weight: 500;
}

.route-preview-rail {
  display: flex;
  align-items: center;
  overflow-x: auto;
  padding-top: 10px;
  padding-bottom: 25px; 
  padding-left: 20px;   
  padding-right: 20px;  
  margin-bottom: 16px;
  gap: 0;
  scrollbar-width: none;
}

.route-preview-rail::-webkit-scrollbar {
  display: none;
}

.route-stop {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex-shrink: 0;
}

.route-stop__dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #028e5c;
  flex-shrink: 0;
  z-index: 2;
}

.route-stop__name {
  position: absolute;
  top: 22px;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  white-space: nowrap;
  text-align: center;
}

.route-connector {
  flex-shrink: 0;
  height: 3px;
  width: 28px;
  background: #028e5c;
  z-index: 1;
}

.route-stop--first .route-stop__dot,
.route-stop--last .route-stop__dot {
  background: #028e5c;
  width: 14px;
  height: 14px;
}

.route-stop--current .route-stop__dot {
  background: #1876d2;
  width: 14px;
  height: 14px;
}

.route-stop--current .route-stop__name {
  font-weight: 700;
  color: #1876d2;
}

.route-stop--middle .route-stop__name,
.route-stop--last .route-stop__name {
  font-weight: 600;
  color: #028e5c;
}

.route-stop-ellipsis {
  font-size: 13px;
  color: #028e5c;
  flex-shrink: 0;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.route-preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
</style>