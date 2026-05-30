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

      <p class="boarding-question">Como deseja viajar?</p>

      <div v-if="loading" class="flex flex-center q-py-md">
        <q-spinner color="primary" size="30px" />
        <span class="q-ml-sm text-grey-7">A validar...</span>
      </div>

      <div v-else class="boarding-options">
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

const linhaNome = computed(() => {
  const vt = (viagensStore.vehicleTrips || []).find(v => v.id == props.viagemVeiculoId)
  return vt?.trajeto?.linha?.nome || ''
})

const stopName = computed(() => {
  if (!props.paragemEntradaId) return ''
  const stop = (viagensStore.stops || []).find(s => s.id == props.paragemEntradaId)
  return stop?.nome || ''
})

watch(() => props.modelValue, (val) => {
  isOpen.value = val
  if (val) loadTitulos()
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

async function confirmSelection() {
  submitting.value = true
  errorMsg.value = ''
  try {
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
</style>
