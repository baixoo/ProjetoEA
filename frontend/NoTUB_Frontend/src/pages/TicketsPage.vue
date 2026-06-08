<template>
  <q-page class="tickets-page">
    <div class="tickets-container">
      <div class="page-header q-mb-md">
        <h1 class="page-title">Loja de Titulos</h1>
        <p class="page-subtitle">Compre bilhetes e passes de forma rapida e segura</p>
      </div>

      <!-- Shop Offerings List -->
      <div class="shop-list">
        <h2 class="section-title">Bilhetes Individuais</h2>
        <div class="grid-layout">
          <div class="offer-card">
            <div class="offer-icon bg-blue">
              <q-icon name="confirmation_number" size="28px" color="primary" />
            </div>
            <div class="offer-info">
              <h3 class="offer-name">Bilhete Simples</h3>
              <p class="offer-desc">Valido para 1 viagem na rede NoTUB</p>
            </div>
            <div class="offer-price-action">
              <button class="btn-add" @click="openCheckout('ticket_single')">
                <span>Adicionar</span>
              </button>
            </div>
          </div>

          <div class="offer-card">
            <div class="offer-icon bg-blue">
              <q-icon name="filter_5" size="28px" color="primary" />
            </div>
            <div class="offer-info">
              <h3 class="offer-name">Pack 5 Viagens</h3>
              <p class="offer-desc">Desconto especial para viajantes frequentes</p>
            </div>
            <div class="offer-price-action">
              <button class="btn-add" @click="openCheckout('ticket_pack5')">
                <span>Adicionar</span>
              </button>
            </div>
          </div>
        </div>

        <h2 class="section-title q-mt-lg">Passes Mensais & Anuais</h2>
        <div class="grid-layout">
          <div class="offer-card">
            <div class="offer-icon bg-orange">
              <q-icon name="credit_card" size="28px" color="warning" />
            </div>
            <div class="offer-info">
              <h3 class="offer-name">Passe Mensal</h3>
              <p class="offer-desc">Viagens ilimitadas durante 1 mes</p>
            </div>
            <div class="offer-price-action">
              <button class="btn-add" @click="openCheckout('pass_monthly')">
                <span>Adicionar</span>
              </button>
            </div>
          </div>

          <div class="offer-card">
            <div class="offer-icon bg-orange">
              <q-icon name="workspace_premium" size="28px" color="warning" />
            </div>
            <div class="offer-info">
              <h3 class="offer-name">Passe Anual</h3>
              <p class="offer-desc">Viagens ilimitadas durante 1 ano (Melhor Preco)</p>
            </div>
            <div class="offer-price-action">
              <button class="btn-add" @click="openCheckout('pass_annual')">
                <span>Adicionar</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Checkout Popup Dialog -->
    <q-dialog v-model="checkoutOpen" position="bottom" transition-show="slide-up" transition-hide="slide-down">
      <div class="checkout-sheet">
        <div class="drag-handle"></div>

        <h3 class="checkout-title">Finalizar Compra</h3>
        <p class="checkout-subtitle">Configure o seu titulo e proceda ao pagamento.</p>

        <!-- Product Summary -->
        <div class="checkout-summary-card q-mb-md">
          <div class="summary-details">
            <span class="product-name">{{ currentProduct.name }}</span>
            <span class="product-price-base">{{ checkoutUnitPrice != null ? checkoutUnitPrice.toFixed(2) + '€ / unid' : '...' }}</span>
          </div>
          <div class="summary-details summary-user-type">
            <span class="product-category-label">Tarifa aplicada:</span>
            <span class="product-category-value">{{ userCategoryLabel }}</span>
          </div>
          <div v-if="currentProduct.type === 'ticket'" class="quantity-selector">
            <q-btn round flat dense icon="remove" color="primary" @click="decreaseQty" :disabled="quantity <= 1" />
            <span class="quantity-value">{{ quantity }}</span>
            <q-btn round flat dense icon="add" color="primary" @click="increaseQty" />
          </div>
        </div>

        <!-- Seletor de mês/ano (apenas para passes) -->
        <div v-if="currentProduct.type === 'pass'" class="month-section q-mb-md">
          <label class="section-label">Mês de inicio:</label>
          <div class="month-year-row">
            <div class="month-grid">
              <div
                v-for="m in monthOptions"
                :key="m.value"
                class="month-pill"
                :class="{
                  'month-pill--selected': selectedMonth === m.value && selectedYear === selectedYear,
                  'month-pill--occupied': isMonthOccupied(selectedYear, m.value),
                  'month-pill--past': isMonthPast(selectedYear, m.value)
                }"
                @click="selectMonth(m.value)"
              >
                {{ m.label }}
              </div>
            </div>
            <div class="year-selector">
              <button class="year-btn" @click="selectedYear--" :disabled="selectedYear <= currentYear">‹</button>
              <span class="year-value">{{ selectedYear }}</span>
              <button class="year-btn" @click="selectedYear++" :disabled="selectedYear >= currentYear + 2">›</button>
            </div>
          </div>
          <p v-if="selectedMonthOccupied" class="month-conflict-msg">
            Ja tem um passe para este periodo. Escolha outro mes.
          </p>
          <p v-else-if="selectedMonthLabel" class="month-selected-info">
            {{ currentProduct.modalidade === 'MENSAL'
              ? `Valido de 1 de ${selectedMonthLabel} de ${selectedYear} ao fim do mes`
              : `Valido de 1 de ${selectedMonthLabel} de ${selectedYear} a 31 de dezembro de ${selectedYear}` }}
          </p>
        </div>

        <div v-if="errorMessage" class="error-msg q-mb-sm">{{ errorMessage }}</div>

        <!-- Zone selection -->
        <div class="zone-section q-mb-md">
          <label class="section-label">Selecione a Zona:</label>
          <div class="zones-grid">
            <div
              v-for="zone in zoneOptions"
              :key="zone.id"
              class="zone-checkbox-pill"
              :class="{ 'zone-checkbox-pill--selected': selectedZoneId === zone.id }"
              @click="selectedZoneId = zone.id"
            >
              <span>Zona {{ zone.num }}</span>
            </div>
          </div>
          <p class="zone-hint">
            Zona 1: apenas zona 1. Zona 2: zonas 1 e 2. Zona 3: zonas 1, 2 e 3.
          </p>
          <p class="zone-info" v-if="selectedZoneId">
            O seu titulo sera valido para as zonas <strong>1 a {{ selectedZoneNum }}</strong>.
          </p>
        </div>

        <!-- Total Price -->
        <div class="total-bar q-mb-lg">
          <span class="total-label">Total a pagar:</span>
          <span class="total-value">{{ totalPrice.toFixed(2) }}€</span>
        </div>

        <!-- Pay Action Button -->
        <button
          class="btn-pay"
          @click="processPayment"
          :disabled="submitting || !selectedZoneId || checkoutUnitPrice == null || (currentProduct.type === 'pass' && selectedMonthOccupied)"
        >
          <q-spinner v-if="submitting" size="20px" class="q-mr-sm" />
          <span>{{ submitting ? 'A redirecionar para o pagamento...' : 'Confirmar e Pagar' }}</span>
        </button>

        <p class="stripe-hint">Pagamento seguro via Stripe</p>
      </div>
    </q-dialog>

    <!-- Success Confirmation Dialog -->
    <q-dialog v-model="successOpen">
      <q-card class="success-card text-center q-pa-lg">
        <div class="success-icon-container q-mx-auto q-mb-md">
          <q-icon name="check_circle" size="64px" color="positive" class="animated scale-up" />
        </div>
        <h3 class="success-title">Pagamento Efetuado!</h3>
        <p class="success-desc">
          O seu titulo de transporte foi adquirido com sucesso e ja se encontra disponivel na sua carteira.
        </p>
        <q-btn label="Excelente" color="positive" class="full-width q-mt-md" @click="closeSuccess" />
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useTicketsStore } from 'src/stores/tickets'
import { useViagensStore } from 'src/stores/viagens'
import { useAuthStore } from 'src/stores/auth'

const route = useRoute()
const ticketsStore = useTicketsStore()
const viagensStore = useViagensStore()
const authStore = useAuthStore()

const checkoutOpen = ref(false)
const successOpen = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const quantity = ref(1)
const selectedZoneId = ref(null)
const checkoutUnitPrice = ref(null)

// mês/ano para passes
const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1 // 1-12
const selectedMonth = ref(currentMonth)
const selectedYear = ref(currentYear)

const monthOptions = [
  { label: 'Jan', value: 1 },
  { label: 'Fev', value: 2 },
  { label: 'Mar', value: 3 },
  { label: 'Abr', value: 4 },
  { label: 'Mai', value: 5 },
  { label: 'Jun', value: 6 },
  { label: 'Jul', value: 7 },
  { label: 'Ago', value: 8 },
  { label: 'Set', value: 9 },
  { label: 'Out', value: 10 },
  { label: 'Nov', value: 11 },
  { label: 'Dez', value: 12 }
]

const monthNames = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
]

const currentProduct = ref({ id: '', name: '', type: '', modalidade: '' })

const products = {
  ticket_single: { id: 'ticket_single', name: 'Bilhete Simples', type: 'ticket', ticketsQty: 1 },
  ticket_pack5:  { id: 'ticket_pack5',  name: 'Pack 5 Viagens',  type: 'ticket', ticketsQty: 5 },
  pass_monthly:  { id: 'pass_monthly',  name: 'Passe Mensal',    type: 'pass',   modalidade: 'MENSAL' },
  pass_annual:   { id: 'pass_annual',   name: 'Passe Anual',     type: 'pass',   modalidade: 'ANUAL'  }
}

onMounted(async () => {
  await Promise.all([
    ticketsStore.fetchMyTickets(),
    ticketsStore.fetchMyPasses(),
    viagensStore.fetchZones()
  ])

  if (viagensStore.zones?.length > 0) {
    selectedZoneId.value = viagensStore.zones[0].id
  } else {
    selectedZoneId.value = 1
  }

  if (route.query.stripe_success === 'true' && route.query.t) {
    await pollStripeResult(route.query.t)
  }
  if (route.query.stripe_cancel === 'true') {
    errorMessage.value = 'Pagamento cancelado.'
  }
})

// ---- helpers de mês ----

function isMonthOccupied(year, month) {
  const key = `${year}-${String(month).padStart(2, '0')}`
  // Para passe anual, verificar se qualquer mês do ano está ocupado
  if (currentProduct.value.modalidade === 'ANUAL') {
    for (let m = month; m <= 12; m++) {
      const k = `${year}-${String(m).padStart(2, '0')}`
      if (ticketsStore.mesesOcupados.has(k)) return true
    }
    return false
  }
  return ticketsStore.mesesOcupados.has(key)
}

function isMonthPast(year, month) {
  if (year < currentYear) return true
  if (year === currentYear && month < currentMonth) return true
  return false
}

function selectMonth(month) {
  if (isMonthOccupied(selectedYear.value, month)) return
  if (isMonthPast(selectedYear.value, month)) return
  selectedMonth.value = month
}

const selectedMonthOccupied = computed(() =>
  isMonthOccupied(selectedYear.value, selectedMonth.value)
)

const selectedMonthLabel = computed(() =>
  monthNames[selectedMonth.value - 1]
)

// ---- preço ----

async function loadCheckoutPrice() {
  const nr = selectedZoneNum.value
  const prod = currentProduct.value
  if (!prod.id) return
  const modalidade = prod.type === 'pass' ? products[prod.id]?.modalidade : null
  checkoutUnitPrice.value = await ticketsStore.fetchPrice(
    prod.type === 'ticket' ? 'BILHETE' : 'PASSE',
    nr,
    modalidade
  )
}

const zoneOptions = computed(() => {
  if (viagensStore.zones?.length > 0) return viagensStore.zones
  return [{ id: 1, num: 1 }, { id: 2, num: 2 }, { id: 3, num: 3 }]
})

const selectedZoneNum = computed(() => {
  const zone = zoneOptions.value.find(z => z.id === selectedZoneId.value)
  return zone ? zone.num : 1
})

const userCategoryLabel = computed(() => {
  const tipo = authStore.tipoUtilizador || authStore.user?.tipoUtilizador
  if (!tipo) return 'Sem categoria'
  if (tipo === 'CRIANCA') return 'Criança'
  if (tipo === 'SENIOR') return 'Senior'
  if (tipo === 'ESTUDANTE') return 'Estudante'
  return 'Adulto'
})

const totalPrice = computed(() => {
  if (checkoutUnitPrice.value == null) return 0
  if (currentProduct.value.type === 'pass') return checkoutUnitPrice.value
  const unitQty = products[currentProduct.value.id]?.ticketsQty || 1
  return checkoutUnitPrice.value * unitQty * quantity.value
})

function openCheckout(productId) {
  const prod = products[productId]
  if (!prod) return
  currentProduct.value = { ...prod }
  quantity.value = 1
  errorMessage.value = ''
  selectedMonth.value = currentMonth
  selectedYear.value = currentYear

  if (viagensStore.zones?.length > 0) {
    selectedZoneId.value = viagensStore.zones[0].id
  } else {
    selectedZoneId.value = 1
  }

  checkoutOpen.value = true
  loadCheckoutPrice()
}

watch(checkoutOpen, (open) => { if (open) loadCheckoutPrice() })
watch(selectedZoneId, (zoneId) => { if (checkoutOpen.value && zoneId != null) loadCheckoutPrice() })

function increaseQty() { quantity.value++ }
function decreaseQty() { if (quantity.value > 1) quantity.value-- }

async function processPayment() {
  submitting.value = true
  errorMessage.value = ''
  try {
    const isTicket = currentProduct.value.type === 'ticket'
    const checkoutRequest = {
      metodoPagamento: 'CARTAO',
      tipoProduto: isTicket ? 'BILHETE' : 'PASSE',
      zonaId: selectedZoneId.value
    }

    if (isTicket) {
      checkoutRequest.quantidade = currentProduct.value.ticketsQty * quantity.value
    } else {
      checkoutRequest.modalidade = currentProduct.value.modalidade
      checkoutRequest.mesInicio = selectedMonth.value
      checkoutRequest.anoInicio = selectedYear.value
    }

    const result = await ticketsStore.checkout(checkoutRequest)
    checkoutOpen.value = false

    if (result.estado === 'CONCLUIDO') {
      await ticketsStore.fetchMyTickets()
      await ticketsStore.fetchMyPasses()
      successOpen.value = true
    } else if (result.redirectUrl) {
      window.location.href = result.redirectUrl
    }
  } catch (err) {
    errorMessage.value = err.message || 'Erro ao efetuar o pagamento.'
  } finally {
    submitting.value = false
  }
}

async function pollStripeResult(token) {
  try {
    const status = await ticketsStore.checkPaymentStatus(token)
    if (status.estado === 'CONCLUIDO') {
      await ticketsStore.fetchMyTickets()
      await ticketsStore.fetchMyPasses()
      successOpen.value = true
    }
  } catch (e) {
    console.error('Stripe result check erro:', e)
  }
}

function closeSuccess() {
  successOpen.value = false
  errorMessage.value = ''
}
</script>

<style scoped>
.tickets-page {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: calc(var(--header-h, 42px) + 24px) var(--page-pad, 20px) calc(var(--tabbar-h, 78px) + 16px);
  background: #f8f9fa;
}

.tickets-container {
  width: 100%;
}

.page-header {
  text-align: left;
}

.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 24px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0;
  line-height: 1.2;
}

.page-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #6c757d;
  margin: 4px 0 0 0;
}

.section-title {
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #0b1a16;
  margin: 0 0 12px 0;
}

.grid-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.offer-card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
  transition: transform 0.2s, box-shadow 0.2s;
}

.offer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.05);
}

.offer-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bg-blue   { background: #ebf3ff; }
.bg-orange { background: #fff5eb; }

.offer-info  { flex-grow: 1; }

.offer-name {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0b1a16;
  margin: 0 0 2px 0;
}

.offer-desc {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #6c757d;
  margin: 0;
  line-height: 1.3;
}

.offer-price-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.btn-add {
  height: 28px;
  padding: 0 12px;
  border-radius: 6px;
  border: none;
  background: #028e5c;
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
}

.btn-add:hover   { background: #017a4e; }
.btn-add:active  { transform: scale(0.96); }

/* ---- Seletor de mês ---- */
.month-section { }

.month-year-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.month-pill {
  padding: 7px 4px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #495057;
  text-align: center;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
}

.month-pill:hover:not(.month-pill--occupied):not(.month-pill--past) {
  border-color: #028e5c;
  color: #028e5c;
}

.month-pill--selected {
  background: #e6f7f0;
  border-color: #028e5c;
  color: #028e5c;
  font-weight: 700;
}

.month-pill--occupied {
  background: #fff5f5;
  border-color: #ffc9c9;
  color: #c92a2a;
  cursor: not-allowed;
  text-decoration: line-through;
}

.month-pill--past {
  background: #f8f9fa;
  color: #ced4da;
  cursor: not-allowed;
}

.year-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.year-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #dee2e6;
  background: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #495057;
  transition: border-color 0.15s;
}

.year-btn:hover:not(:disabled) { border-color: #028e5c; color: #028e5c; }
.year-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.year-value {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0b1a16;
  min-width: 40px;
  text-align: center;
}

.month-conflict-msg {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #c92a2a;
  margin: 6px 0 0 0;
  font-weight: 600;
}

.month-selected-info {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #028e5c;
  margin: 6px 0 0 0;
  font-weight: 600;
  background: #e6f7f0;
  padding: 6px 10px;
  border-radius: 6px;
}

/* ---- resto do checkout (igual ao original) ---- */
.checkout-sheet {
  background: #fff;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  width: 100%;
  max-width: 100vw;
  padding: 12px 20px 34px;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);
}

.drag-handle {
  width: 60px;
  height: 4px;
  background: #dee2e6;
  border-radius: 100px;
  margin: 0 auto 16px;
}

.checkout-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px 0;
}

.checkout-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #6c757d;
  margin: 0 0 16px 0;
}

.checkout-summary-card {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-details { display: flex; flex-direction: column; }

.product-name {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0b1a16;
}

.product-price-base {
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  color: #6c757d;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 20px;
  padding: 2px 6px;
}

.quantity-value {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 700;
  min-width: 20px;
  text-align: center;
}

.section-label {
  display: block;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 700;
  color: #0b1a16;
  margin-bottom: 8px;
}

.zones-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.zone-checkbox-pill {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #dee2e6;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}

.zone-checkbox-pill--selected {
  background: #e6f7f0;
  border-color: #028e5c;
  color: #028e5c;
  font-weight: 700;
}

.zone-hint {
  font-family: 'Inter', sans-serif;
  font-size: 10px;
  color: #868e96;
  margin: 6px 0 0 0;
  line-height: 1.4;
}

.zone-info {
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  color: #028e5c;
  margin: 4px 0 0 0;
  font-weight: 600;
  background: #e6f7f0;
  padding: 6px 10px;
  border-radius: 6px;
}

.total-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f3f5;
  padding-top: 14px;
}

.total-label {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.total-value {
  font-family: 'Inter', sans-serif;
  font-size: 20px;
  font-weight: 900;
  color: #028e5c;
}

.btn-pay {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #028e5c 0%, #01bc74 100%);
  color: #fff;
  font-family: 'Inter', sans-serif;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(2, 142, 92, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-pay:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(2, 142, 92, 0.3);
}

.btn-pay:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.error-msg {
  color: #d32f2f;
  font-size: 12px;
  text-align: center;
}

.stripe-hint {
  font-family: 'Inter', sans-serif;
  font-size: 10px;
  color: #adb5bd;
  text-align: center;
  margin: 8px 0 0 0;
}

.success-card {
  width: 320px;
  border-radius: 16px;
}

.success-icon-container {
  width: 72px;
  height: 72px;
  background: #e6f7f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 8px 0;
}

.success-desc {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  line-height: 1.4;
}
</style>
