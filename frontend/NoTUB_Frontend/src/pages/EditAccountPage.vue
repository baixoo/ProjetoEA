<template>
  <q-page class="account-page">
    <div class="account-content">
      <!-- Profile Header Section -->
      <div class="profile-header">
        <div class="profile-avatar">
          {{ initials }}
        </div>
        <h2 class="profile-name">{{ userFullName || 'Utilizador' }}</h2>
        <p class="profile-email">{{ form.email }}</p>
      </div>

      <!-- Points Dashboard -->
      <div class="points-dashboard">
        <div class="points-summary">
          <div class="points-label-container">
            <q-icon name="stars" size="24px" class="points-star-icon" />
            <span class="points-title">Os Seus Pontos</span>
          </div>
          <div class="points-value">{{ points }} <span class="points-unit">Pts</span></div>
        </div>

        <div class="tier-status">
          <div class="tier-badge-container">
            <span class="tier-badge" :class="currentTier.class">
              {{ currentTier.name }}
            </span>
          </div>
          <div v-if="currentTier.nextMilestone" class="tier-next-info">
            Faltam <strong>{{ pointsToNextMilestone }}</strong> pontos para <strong>{{ currentTier.nextName }}</strong>
          </div>
          <div v-else class="tier-next-info">
            Nível máximo atingido! Parabéns!
          </div>
        </div>

        <div class="progress-container">
          <q-linear-progress :value="tierProgress" class="tier-progress-bar" />
        </div>
      </div>

      <!-- Reward Redemption -->
      <div class="reward-section">
        <h3 class="section-title">Resgatar Prémio</h3>
        <div class="reward-card">
          <div class="reward-info">
            <div class="reward-icon-container">
              <q-icon name="confirmation_number" size="24px" color="primary" />
            </div>
            <div class="reward-text">
              <span class="reward-name">Bilhete Simples Gratuito</span>
              <span class="reward-cost">Custo: 100 Pontos</span>
            </div>
          </div>
          <button 
            class="btn-redeem" 
            :disabled="points < 100 || loadingRedeem" 
            @click="handleRedeem"
          >
            <q-spinner v-if="loadingRedeem" size="16px" class="q-mr-xs" />
            <span>{{ loadingRedeem ? 'A Resgatar...' : 'Resgatar' }}</span>
          </button>
        </div>
        <p v-if="redeemSuccess" class="success-message text-center q-mt-xs">{{ redeemSuccess }}</p>
        <p v-if="redeemError" class="error-message text-center q-mt-xs">{{ redeemError }}</p>
      </div>

      <!-- Points Transaction History -->
      <div class="history-section">
        <h3 class="section-title">Histórico de Pontos</h3>
        
        <div v-if="loadingHistory" class="history-loading text-center q-py-md">
          <q-spinner color="primary" size="24px" />
        </div>
        
        <div v-else-if="historico.length === 0" class="history-empty text-center q-py-md">
          <q-icon name="history_toggle_off" size="32px" color="grey-5" />
          <p class="history-empty-text">Ainda não tem transações de pontos.</p>
        </div>
        
        <div v-else class="history-list">
          <div 
            v-for="item in historico" 
            :key="item.id" 
            class="history-item"
          >
            <div class="history-left">
              <div 
                class="history-badge" 
                :class="item.pontos >= 0 ? 'history-badge--plus' : 'history-badge--minus'"
              >
                {{ item.pontos >= 0 ? '+' : '' }}{{ item.pontos }}
              </div>
              <div class="history-details">
                <span class="history-desc">{{ item.descricao }}</span>
                <span class="history-date">{{ formatDate(item.dataHora) }}</span>
              </div>
            </div>
            <div class="history-right">
              <span class="history-type">{{ item.tipo }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Account Edit Form Section -->
      <div class="account-edit-section">
        <div class="section-header-row">
          <h3 class="section-title" style="margin-bottom:0">Dados Pessoais</h3>
          <button class="btn-toggle-form" @click="showForm = !showForm">
            <q-icon :name="showForm ? 'expand_less' : 'expand_more'" size="20px" />
          </button>
        </div>
        <form v-if="showForm" class="account-form" @submit.prevent="handleSave">
          <div class="field">
            <input v-model="form.email" type="email" placeholder="Email" class="field__input" required />
          </div>
          <div class="field">
            <input v-model="form.nome" type="text" placeholder="Nome" class="field__input" required />
          </div>
          <div class="field">
            <input v-model="form.nif" type="text" placeholder="NIF" class="field__input" maxlength="9" />
          </div>
          <div class="field">
            <input v-model="form.dataNascimento" type="date" placeholder="Data de nascimento" class="field__input" />
          </div>
          <div v-if="authStore.user?.authMethod === 'CREDENTIALS'" class="field">
            <input v-model="form.password" type="password" placeholder="Palavra-passe" class="field__input" />
          </div>
          <p v-if="error" class="error-message">{{ error }}</p>
          <p v-if="success" class="success-message">{{ success }}</p>
          <button type="submit" class="btn btn--primary" :disabled="loading">
            {{ loading ? 'A guardar...' : 'Guardar Alteracoes' }}
          </button>
        </form>
      </div>

      <!-- Bottom Actions -->
      <div class="bottom-actions">
        <router-link to="/terms" class="link-terms">Termos e Condicoes</router-link>
        <router-link v-if="authStore.user?.role === 'ADMINISTRADOR'" to="/admin" class="link-admin">
          <q-icon name="admin_panel_settings" size="18px" />
          <span>Painel de Administracao</span>
        </router-link>
        <router-link v-else-if="authStore.user?.role === 'MOTORISTA'" to="/driver" class="link-driver">
          <q-icon name="directions_bus" size="18px" />
          <span>Painel de Motorista</span>
        </router-link>
        <button class="btn btn--logout" @click="handleLogout">
          <q-icon name="logout" size="18px" class="q-mr-xs" />
          <span>Sair da Conta</span>
        </button>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'
import { useTicketsStore } from 'src/stores/tickets'
import { useViagensStore } from 'src/stores/viagens'

const router = useRouter()
const authStore = useAuthStore()
const ticketsStore = useTicketsStore()
const viagensStore = useViagensStore()

const form = reactive({
  email: '',
  nome: '',
  nif: '',
  dataNascimento: '',
  password: ''
})

const error = ref('')
const success = ref('')
const loading = ref(false)
const showForm = ref(false)

// Points & history state
const points = computed(() => authStore.user?.nrPontos ?? 0)
const historico = ref([])
const loadingHistory = ref(false)
const loadingRedeem = ref(false)
const redeemSuccess = ref('')
const redeemError = ref('')

// Computed properties for User Initials
const initials = computed(() => {
  if (!authStore.user) return 'U'
  const first = authStore.user.primeiroNome ? authStore.user.primeiroNome.charAt(0).toUpperCase() : ''
  const last = authStore.user.ultimoNome ? authStore.user.ultimoNome.charAt(0).toUpperCase() : ''
  return first + last || 'U'
})

const userFullName = computed(() => {
  if (!authStore.user) return ''
  return [authStore.user.primeiroNome, authStore.user.ultimoNome].filter(Boolean).join(' ')
})

// Tier Calculations
const currentTier = computed(() => {
  const pts = points.value
  if (pts < 100) {
    return { name: 'Bronze', class: 'tier--bronze', nextMilestone: 100, nextName: 'Explorador Prata' }
  }
  if (pts < 300) {
    return { name: 'Explorador Prata', class: 'tier--silver', nextMilestone: 300, nextName: 'Navegador Ouro' }
  }
  if (pts < 500) {
    return { name: 'Navegador Ouro', class: 'tier--gold', nextMilestone: 500, nextName: 'Lenda Platina' }
  }
  return { name: 'Lenda Platina', class: 'tier--platinum', nextMilestone: null, nextName: null }
})

const tierProgress = computed(() => {
  const pts = points.value
  if (pts < 100) return pts / 100
  if (pts < 300) return (pts - 100) / 200
  if (pts < 500) return (pts - 300) / 200
  return 1.0
})

const pointsToNextMilestone = computed(() => {
  const tier = currentTier.value
  if (!tier.nextMilestone) return 0
  return tier.nextMilestone - points.value
})

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchUser()
  }
  if (authStore.user) {
    const u = authStore.user
    form.email = u.email || ''
    form.nome = [u.primeiroNome, u.ultimoNome].filter(Boolean).join(' ')
    form.nif = u.nif || ''
    form.dataNascimento = u.dataNascimento || ''
    form.password = u.authMethod === 'CREDENTIALS' ? '*****' : ''
  }
  await Promise.all([
    fetchHistorico(),
    viagensStore.fetchZones()
  ])
})

async function fetchHistorico() {
  if (!authStore.token) return
  loadingHistory.value = true
  try {
    const response = await fetch('/api/pontos/historico', {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      historico.value = await response.json()
    }
  } catch (e) {
    console.error('Erro ao buscar histórico de pontos:', e)
  } finally {
    loadingHistory.value = false
  }
}

async function handleRedeem() {
  if (points.value < 100) return
  redeemError.value = ''
  redeemSuccess.value = ''
  loadingRedeem.value = true
  try {
    // 1. Deduct points via API
    const response = await fetch('/api/pontos/utilizar', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        pontos: 100,
        descricao: 'Resgate de Bilhete Gratuito'
      })
    })

    if (!response.ok) {
      const data = await response.json().catch(() => ({}))
      throw new Error(data.mensagem || 'Falha ao utilizar pontos')
    }

    // 2. Buy the free ticket (using first zone id or fallback 1)
    const zoneId = viagensStore.zones?.[0]?.id || 1
    await ticketsStore.buyTickets(1, zoneId)

    // 3. Refresh user profile (for points balance) and history
    await authStore.fetchUser()
    await fetchHistorico()

    redeemSuccess.value = 'Bilhete resgatado e adicionado à sua carteira!'
  } catch (e) {
    redeemError.value = e.message || 'Erro ao resgatar bilhete'
  } finally {
    loadingRedeem.value = false
  }
}

async function handleSave() {
  error.value = ''
  success.value = ''
  loading.value = true

  const nameParts = form.nome.trim().split(/\s+/)
  const primeiroNome = nameParts[0] || ''
  const ultimoNome = nameParts.slice(1).join(' ') || ''

  try {
    await authStore.updateProfile({
      email: form.email,
      primeiroNome,
      ultimoNome,
      nif: form.nif || null,
      dataNascimento: form.dataNascimento || null
    })
    success.value = 'Perfil atualizado com sucesso!'
  } catch (e) {
    error.value = e.message || 'Erro ao atualizar perfil'
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/signin')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleString('pt-PT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}
</script>

<style scoped>
.account-page {
  position: relative;
  min-height: 100vh;
  padding-top: calc(var(--header-h, 42px) + 16px);
  padding-bottom: calc(var(--tabbar-h, 78px) + 16px);
  background: #f7f9fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
}

.account-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 0 var(--page-pad, 20px);
  width: 100%;
}

/* Profile Header Styles */
.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-align: center;
  margin-top: 10px;
}

.profile-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1876d2 0%, #00d4ff 100%);
  color: #ffffff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(24, 118, 210, 0.2);
  letter-spacing: 0.5px;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: #121212;
  margin: 8px 0 0 0;
  line-height: 1.2;
}

.profile-email {
  font-size: 13px;
  color: #757575;
  margin: 0;
}

/* Points Dashboard Card */
.points-dashboard {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  padding: 16px;
  width: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.points-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.points-label-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.points-star-icon {
  color: #ffb300;
}

.points-title {
  font-size: 14px;
  font-weight: 600;
  color: #121212;
}

.points-value {
  font-size: 26px;
  font-weight: 700;
  color: #121212;
}

.points-unit {
  font-size: 13px;
  font-weight: 500;
  color: #757575;
}

.tier-status {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.tier-badge-container {
  display: flex;
}

.tier-badge {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 12px;
  letter-spacing: 0.5px;
}

.tier--bronze {
  background: #efebe9;
  color: #8d6e63;
}

.tier--silver {
  background: #eceff1;
  color: #78909c;
}

.tier--gold {
  background: #fff8e1;
  color: #ffb300;
}

.tier--platinum {
  background: #e0f7fa;
  color: #00acc1;
}

.tier-next-info {
  font-size: 12px;
  color: #616161;
  line-height: 1.4;
}

.progress-container {
  margin-top: 10px;
}

.tier-progress-bar {
  height: 8px;
  border-radius: 4px;
  background: #f5f5f5;
}

/* Sections Common Styles */
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 10px 0;
  width: 100%;
}

.reward-section,
.history-section,
.account-edit-section {
  width: 100%;
  display: flex;
  flex-direction: column;
}

/* Reward Redemption Section */
.reward-card {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.reward-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reward-icon-container {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(24, 118, 210, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.reward-text {
  display: flex;
  flex-direction: column;
}

.reward-name {
  font-size: 14px;
  font-weight: 600;
  color: #121212;
}

.reward-cost {
  font-size: 12px;
  color: #1876d2;
  font-weight: 500;
}

.btn-redeem {
  background: #1876d2;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.btn-redeem:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-redeem:disabled {
  background: #e0e0e0;
  color: #9e9e9e;
  cursor: not-allowed;
}

/* Points History Section */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 4px;
}

.history-item {
  background: #ffffff;
  border: 1px solid #eef2f5;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.01);
}

.history-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-badge {
  min-width: 46px;
  height: 26px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.history-badge--plus {
  background: rgba(46, 125, 50, 0.08);
  color: #2e7d32;
}

.history-badge--minus {
  background: rgba(198, 40, 40, 0.08);
  color: #c62828;
}

.history-details {
  display: flex;
  flex-direction: column;
}

.history-desc {
  font-size: 13px;
  font-weight: 500;
  color: #121212;
  line-height: 1.3;
}

.history-date {
  font-size: 11px;
  color: #757575;
  margin-top: 2px;
}

.history-right {
  display: flex;
  align-items: center;
}

.history-type {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 2px 6px;
  background: #eceff1;
  color: #546e7a;
  border-radius: 4px;
}

.history-empty-text {
  font-size: 13px;
  color: #757575;
  margin-top: 4px;
}

/* Edit Account Form Styles */
.account-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.field__input {
  width: 100%;
  height: 40px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 400;
  color: #121212;
  outline: none;
  transition: border-color 0.2s;
}

.field__input::placeholder {
  color: #9e9e9e;
}

.field__input:focus {
  border-color: #1876d2;
}

.error-message {
  color: #d32f2f;
  font-size: 13px;
  margin: -8px 0 0 0;
}

.success-message {
  color: #2e7d32;
  font-size: 13px;
  margin: -8px 0 0 0;
}

.btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn--primary {
  background: #121212;
  color: #ffffff;
}

.btn--primary:hover:not(:disabled) {
  opacity: 0.9;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.btn-toggle-form {
  background: none;
  border: none;
  cursor: pointer;
  color: #757575;
  padding: 4px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-toggle-form:hover {
  background: #f0f0f0;
}

.bottom-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 8px;
}

.link-admin {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #028e5c;
  text-decoration: none;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #028e5c;
  border-radius: 10px;
  width: 100%;
  transition: all 0.2s;
}

.link-admin:hover {
  background: #e6f7f0;
}

.link-driver {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: #1876d2;
  text-decoration: none;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #1876d2;
  border-radius: 10px;
  width: 100%;
  transition: all 0.2s;
}

.link-driver:hover {
  background: #e3f2fd;
}

.link-terms {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #028e5c;
  text-decoration: none;
  font-weight: 600;
  transition: opacity 0.2s;
}

.link-terms:hover {
  opacity: 0.8;
}

.btn--logout {
  background: #ffffff;
  color: #d32f2f;
  border: 1px solid #ffcdd2;
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn--logout:hover {
  background: #fff5f5;
  border-color: #ef9a9a;
}
</style>
