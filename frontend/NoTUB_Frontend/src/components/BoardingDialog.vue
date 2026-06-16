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

          <div v-if="zonaMin !== null" class="zone-range">
            <q-icon name="map" size="14px" color="green-8" />
            <span>Zonas {{ zonaMin }}
              <span v-if="zonaMax !== zonaMin"> — {{ zonaMax }}</span>
            </span>
          </div>

          <div class="route-preview-rail">
            <template v-for="(item, index) in routePreviewItems" :key="item.key">
              <div v-if="item.type === 'ellipsis'" class="route-connector route-stop-ellipsis"></div>
              
              <div 
                v-else 
                class="route-stop" 
                :class="[
                  routePreviewItems.filter(i => i.type !== 'ellipsis').indexOf(item) % 2 === 0 ? 'node--up' : 'node--down',
                  {
                    'route-stop--first': item.kind === 'first',
                    'route-stop--current': item.kind === 'current',
                    'route-stop--last': item.kind === 'last',
                    'route-stop--middle': item.kind === 'middle'
                  }
                ]"
              >
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
          <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

          <div v-if="!travelModeSelected" class="travel-mode-selection">
            <p class="section-title">Como deseja viajar hoje?</p>
            
            <div class="mode-cards-grid">
              <button 
                class="mode-card" 
                :class="{ 'mode-card--selected': tempTravelMode === 'individual' }"
                @click="tempTravelMode = 'individual'"
              >
                <q-icon name="person" size="28px" class="mode-icon" />
                <span class="mode-title">VIAGEM INDIVIDUAL</span>
              </button>

              <button 
                class="mode-card" 
                :class="{ 'mode-card--selected': tempTravelMode === 'grupo' }"
                @click="tempTravelMode = 'grupo'"
              >
                <q-icon name="groups" size="28px" class="mode-icon" />
                <span class="mode-title">VIAGEM EM GRUPO</span>
                <span class="mode-desc">Apenas com Bilhetes.</span>
              </button>
            </div>

            <div v-if="tempTravelMode === 'grupo'" class="group-qty-box q-mt-md">
              <span class="qty-label">Número de Pessoas:</span>
              <div class="flex items-center gap-sm">
                <q-btn round flat dense icon="remove" size="sm" @click="groupSize = Math.max(2, groupSize - 1)" />
                <span class="qty-value">{{ groupSize }}</span>
                <q-btn round flat dense icon="add" size="sm" @click="groupSize = groupSize + 1" />
              </div>
            </div>

            <div class="row justify-between items-center q-mt-lg">
              <q-btn flat color="grey-7" label="Voltar à câmara" icon="arrow_back" dense @click="closeDialog" />
              <q-btn 
                color="green-8" 
                label="Continuar" 
                :disabled="!tempTravelMode" 
                @click="confirmTravelMode" 
              />
            </div>
          </div>

          <div v-else class="titles-selection-flow">
            
            <div class="mode-indicator mb-sm">
              <q-icon :name="travelMode === 'grupo' ? 'groups' : 'person'" size="18px" />
              <span>Modo: <strong>{{ travelMode === 'grupo' ? `Grupo (${groupSize} pessoas)` : 'Individual' }}</strong></span>
              <q-btn flat dense round icon="edit" size="xs" color="grey-7" @click="travelModeSelected = false" />
            </div>

            <button
              v-if="travelMode !== 'grupo'"
              class="option-card"
              :class="{ 
                'option-card--selected': selectedType === 'passe',
                'option-card--disabled': !hasActivePass || !passValido
              }"
              @click="(hasActivePass && passValido) ? selectedType = 'passe' : null"
              :disabled="submitting || !hasActivePass || !passValido"
            >
              <div class="option-icon-container bg-pass">
                <q-icon name="credit_card" class="option-icon" />
              </div>
              <div class="option-details">
                <span class="option-title">Usar Passe Ativo</span>
                <span class="option-subtitle">
                  <template v-if="!hasActivePass">Nenhum passe ativo</template>
                  <template v-else-if="!passValido">Zona {{ activePass?.zona?.num }} — insuficiente para zona {{ zonaViagem }}</template>
                  <template v-else>Zona: {{ activePass?.zona?.num }}</template>
                </span>
              </div>
              <q-icon v-if="selectedType === 'passe'" name="check_circle" class="selected-icon" />
            </button>

            <button
              class="option-card"
              :class="{ 
                'option-card--selected': selectedType === 'bilhete',
                'option-card--disabled': !isTicketOptionValid || bilhetesValidosCount === 0
              }"
              @click="(isTicketOptionValid && bilhetesValidosCount > 0) ? selectedType = 'bilhete' : null"
              :disabled="submitting || !isTicketOptionValid || bilhetesValidosCount === 0"
            >
              <div class="option-icon-container bg-ticket">
                <q-icon name="confirmation_number" class="option-icon" />
              </div>
              <div class="option-details">
                <span class="option-title">Usar Bilhetes</span>
                <span class="option-subtitle">
                  <template v-if="unusedTicketsCount === 0">Sem bilhetes disponíveis</template>
                  <template v-else-if="bilhetesValidosCount === 0">Sem bilhetes válidos para zona {{ zonaViagem }}</template>
                  <template v-else-if="travelMode === 'grupo' && bilhetesValidosCount < groupSize">
                    Insuficiente (Tem {{ bilhetesValidosCount }} de {{ groupSize }} válidos para zona {{ zonaViagem }})
                  </template>
                  <template v-else>Restam {{ bilhetesValidosCount }} bilhetes válidos</template>
                </span>
              </div>
              <q-icon v-if="selectedType === 'bilhete'" name="check_circle" class="selected-icon" />
            </button>

            <div v-if="selectedType === 'bilhete' && travelMode === 'grupo'" class="qty-row q-mt-sm">
              <span class="qty-label">Número de Pessoas:</span>
              <q-btn round flat dense icon="remove" size="sm" @click="groupSize = Math.max(2, groupSize - 1)" />
              <span class="qty-value">{{ groupSize }}</span>
              <q-btn round flat dense icon="add" size="sm" @click="groupSize = Math.min(bilhetesValidosCount, groupSize + 1)" />
            </div>

            <div v-if="shouldShowShopWarning" class="no-tickets-warning q-mt-md">
              <p class="warning-message">Não possui títulos suficientes para o modo selecionado.</p>
              <p class="warning-question">Deseja adquirir novos bilhetes?</p>
              <button class="btn-shop q-mt-sm" @click="goToShop">
                <q-icon name="shopping_bag" class="q-mr-xs" size="18px" />
                Ir para a Loja
              </button>
            </div>

            <button
              v-if="selectedType"
              class="btn-start q-mt-md"
              @click="confirmSelection"
              :disabled="submitting"
            >
              <q-spinner v-if="submitting" size="18px" class="q-mr-sm" />
              {{ submitting ? 'A iniciar...' : 'Confirmar Embarque' }}
            </button>
          </div>
        </template>
      </div>

      <!-- No Valid Tickets Dialog -->
      <q-dialog v-model="titlesDialogOpen" persistent>
        <q-card class="titles-dialog-card" style="min-width: 350px;">
          <q-card-section class="row items-center q-gutter-sm">
            <q-icon name="confirmation_number" color="primary" size="24px" />
            <div class="text-h6">Sem títulos válidos</div>
          </q-card-section>

          <q-card-section class="q-pt-none">
            <div v-if="tempTravelMode === 'grupo'">
              Não tem bilhetes suficientes para todos os elementos do grupo.
            </div>
            <div v-else>
              Não tem bilhetes nem passe ativo para embarcar neste autocarro.
            </div>
          </q-card-section>

          <q-card-actions align="right" class="q-pa-md q-pt-none">
            <q-btn flat color="grey-7" label="Cancelar" @click="cancelTitlesDialog" />
            <q-btn color="primary" label="Comprar títulos" @click="goToTicketsFromDialog" />
          </q-card-actions>
        </q-card>
      </q-dialog>

    </div>
  </q-dialog>
</template>

<script setup>
  import { ref, computed, watch, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { useTicketsStore } from 'src/stores/tickets'
  import { useViagensStore } from 'src/stores/viagens'

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

  const zonaMin = ref(null)
  const zonaMax = ref(null)

  const travelModeSelected = ref(false)
  const tempTravelMode = ref(null) 
  const travelMode = ref(null)   
  const groupSize = ref(2)

  const zonaViagem = computed(() => {
  const stop = (viagensStore.stops || []).find(s => Number(s.id) === Number(props.paragemEntradaId))
  return stop?.zonaNum ?? null
})

const passValido = computed(() => {
  if (!ticketsStore.activePass?.zona?.num) return false
  return ticketsStore.activePass.zona.num >= zonaViagem.value
})

const bilhetesValidosCount = computed(() => {
  return (ticketsStore.tickets || []).filter(
    t => !t.usado && t.zona.num >= zonaViagem.value
  ).length
})

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

    if (last.id !== pontos[cur].id) {
      pushStop(last, 'last')
    }

    return items
  })

  const isTicketOptionValid = computed(() => {
    if (unusedTicketsCount.value === 0) return false
    if (travelMode.value === 'grupo' && unusedTicketsCount.value < groupSize.value) return false
    return true
  })

  const shouldShowShopWarning = computed(() => {
    if (travelMode.value === 'individual') {
      return !hasActivePass.value && unusedTicketsCount.value === 0
    }
    if (travelMode.value === 'grupo') {
      return unusedTicketsCount.value < groupSize.value
    }
    return false
  })

  const activePass = computed(() => ticketsStore.activePass)
  const hasActivePass = computed(() => !!activePass.value)
  const unusedTickets = computed(() => (ticketsStore.tickets || []).filter(t => !t.usado))
  const unusedTicketsCount = computed(() => unusedTickets.value.length)

  const titlesDialogOpen = ref(false)
  const titlesDialogOption = ref('NO_TITLES') // 'NO_TITLES' ou 'LOW_ZONE'

  function cancelTitlesDialog() {
    titlesDialogOpen.value = false
    closeDialog()
  }

  function goToTicketsFromDialog() {
    titlesDialogOpen.value = false
    closeDialog()
    router.push('/tickets')
  }

function confirmTravelMode() {
  const stop = viagensStore.stops.find(s => Number(s.id) === Number(props.paragemEntradaId))
  const zonaViagem = stop?.zonaNum ?? null

  if (tempTravelMode.value === 'individual') {
    const passOk = ticketsStore.activePass?.zona?.num >= zonaViagem
    const ticketOk = (ticketsStore.tickets || []).some(t => !t.usado && t.zona.num >= zonaViagem)

    if (!passOk && !ticketOk) {
      titlesDialogOption.value = 'NO_TITLES'
      titlesDialogOpen.value = true  
      return
    }
  }

  if (tempTravelMode.value === 'grupo') {
    const validTickets = (ticketsStore.tickets || []).filter(
      t => !t.usado && t.zona.num >= zonaViagem
    )
    if (validTickets.length < groupSize.value) {
      titlesDialogOption.value = 'NO_TITLES'
      titlesDialogOpen.value = true  
      return
    }
  }

  travelMode.value = tempTravelMode.value
  travelModeSelected.value = true
  errorMsg.value = ''
}

  async function loadTitulos() {
    loading.value = true
    errorMsg.value = ''
    selectedType.value = null
    ticketQty.value = 1
    travelModeSelected.value = false
    tempTravelMode.value = null
    travelMode.value = null
    groupSize.value = 2
    try {
      await Promise.all([ticketsStore.fetchMyTickets(), ticketsStore.fetchMyPass()])
    } catch {
      errorMsg.value = 'Erro ao carregar titulos.'
    } finally {
      loading.value = false
    }
  }  

  async function loadZonas() {
    if (!props.viagemVeiculoId) return

    const trajetoId = selectedTrip.value?.trajeto?.id
  
    if (!trajetoId) {
      console.warn('Aguardando que os dados da viagem carreguem para obter o trajetoId...')
      return
    }
    
    try {
      const data = await viagensStore.fetchZonasVeiculo(props.viagemVeiculoId, props.paragemEntradaId)
      zonaMin.value = data.zonaMin
      zonaMax.value = data.zonaMax
      console.log('Zonas carregadas:', zonaMin.value, zonaMax.value)
    } catch (e) {
      console.warn('Erro ao carregar zonas no ecrã:', e)
      zonaMin.value = null
      zonaMax.value = null
    }
  }

  function closeDialog() {
    isOpen.value = false
  }

  async function confirmSelection() {
    submitting.value = true
    errorMsg.value = ''
    try {
      const qtdAValidar = travelMode.value === 'grupo' ? groupSize.value : ticketQty.value

      await viagensStore.startTrip({
        tipoTitulo: selectedType.value.toUpperCase(), // 'PASSE' ou 'BILHETE'
        quantidade: selectedType.value === 'passe' ? 1 : qtdAValidar,
        paragemEntradaId: props.paragemEntradaId,
        viagemVeiculoId: props.viagemVeiculoId
      })

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

  watch(() => props.modelValue, (val) => {
    isOpen.value = val

    if (val) {
      console.log('BoardingDialog abriu. Props:', {
        viagemVeiculoId: props.viagemVeiculoId,
        paragemEntradaId: props.paragemEntradaId,
        busNumber: props.busNumber
      })
      
      showRoutePreview.value = true
      loadTitulos()
      loadZonas() 
    }
  })

  watch(isOpen, (val) => emit('update:modelValue', val))

  onMounted(() => { 
      if (isOpen.value) {
      loadTitulos()
      loadZonas()
      viagensStore.fetchStops()
    }
  })
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
  background: #ffffff;
  border: 1px dashed #d7d6d6;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  width: 100%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.warning-message {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #212529;
  font-weight: 500;
  margin: 0 0 4px 0;
}

.warning-question {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #6c757d;
  margin: 0 0 12px 0;
}

.btn-shop {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  border: none;
  background: #028e5c;
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, transform 0.2s;
}

.btn-shop:hover {
  background: #027a4f;
  transform: translateY(-1px);
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
  padding-top: 42px;    
  padding-bottom: 42px; 
  padding-left: 45px;   
  padding-right: 45px;  
  margin-bottom: 16px;
  gap: 0;
  scrollbar-width: none;
}

.route-preview-rail::-webkit-scrollbar {
  display: none;
}

.route-stop__dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #028e5c;
  flex-shrink: 0;
  z-index: 2;
}

.route-stop {
  display: flex;
  align-items: center;
  justify-content: center;  
  position: relative;
  flex-shrink: 0;
  width: 14px; 
  height: 14px;          
}

.route-stop__name {
  position: absolute;
  width: 92px;
  text-align: center;
  font-size: 11px;
  line-height: 1.25;
  white-space: normal;
  left: 50%;
  transform: translateX(-50%);
}

.route-stop.node--up .route-stop__name   { bottom: 22px; top: auto; }
.route-stop.node--down .route-stop__name { top: 22px; bottom: auto; }

.route-connector {
  flex-shrink: 0;
  height: 3px;
  width: 30px; 
  background: #028e5c;
  z-index: 1;
}

.route-connector.route-stop-ellipsis {
  background: transparent; 
  background-image: linear-gradient(to right, #028e5c 60%, transparent 40%);
  background-size: 6px 3px; 
  background-repeat: repeat-x;
  width: 25px; 
  height: 3px;
  flex-shrink: 0;
  z-index: 1;
  margin: 0 2px; 
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

.route-preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.zone-range {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #028e5c;
  margin-bottom: 10px;
}

.section-title {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 12px;
}

.mode-cards-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.mode-card {
  background: #ffffff;
  border: 2px solid #d7d6d6;
  border-radius: 12px;
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
}

.mode-card:hover {
  border-color: #028e5c;
}

.mode-card--selected {
  border-color: #028e5c;
  background: #f4fbf7;
  box-shadow: 0 4px 10px rgba(2, 142, 92, 0.08);
}

.mode-card .mode-icon {
  color: #6c757d;
  margin-bottom: 4px;
}

.mode-card--selected .mode-icon {
  color: #028e5c;
}

.mode-title {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #121212;
}

.mode-desc {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #6c757d;
  margin-top: 2px;
}

.group-qty-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.mode-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #e9ecef;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  color: #495057;
  align-self: flex-start;
  width: fit-content;
  margin-bottom: 8px;
}

.gap-sm { gap: 8px; }
.mb-sm { margin-bottom: 8px; }
</style>