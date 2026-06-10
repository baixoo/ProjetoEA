<template>
  <q-page class="stop-schedule-page">
    <div class="stop-schedule-content">
      <div class="page-header">
        <q-btn flat round dense icon="arrow_back" @click="router.push('/routes')" class="back-btn" />
        <h2 class="page-title">Proximo Autocarro</h2>
        <p class="page-subtitle">Consulte as proximas passagens numa paragem</p>
      </div>

      <div class="stop-field">
        <label class="stop-label">
          <q-icon name="directions_bus" size="16px" color="primary" />
          <span>Paragem</span>
        </label>
        <div class="stop-input-wrapper">
          <input
            type="text"
            class="stop-input"
            placeholder="Pesquisar paragem..."
            v-model="stopQuery"
            @focus="stopFocused = true"
            @blur="handleBlur"
            @input="onInput"
            autocomplete="off"
          />
          <button v-if="selectedStopId" class="clear-btn" @click="clearStop">&times;</button>
        </div>
        <div v-if="stopFocused && !selectedStopId" class="dropdown">
          <div
            v-for="opt in filteredOptions"
            :key="opt.value"
            class="dropdown-item"
            @mousedown.prevent="selectStop(opt)"
          >
            <span class="dropdown-name">{{ opt.label }}</span>
          </div>
          <div v-if="filteredOptions.length === 0" class="dropdown-empty">
            Nenhuma paragem encontrada
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <q-spinner size="32px" color="primary" />
        <span>A carregar proximas passagens...</span>
      </div>

      <div v-if="!loading && data" class="results-container">
        <div class="stop-name-header">
          <q-icon name="place" size="20px" color="primary" />
          <span>{{ data.paragemNome }}</span>
        </div>

        <div v-if="data.linhas.length === 0" class="no-results">
          <q-icon name="directions_bus" size="48px" color="grey-5" />
          <p>Nenhuma linha passa nesta paragem.</p>
        </div>

        <template v-if="data.linhas.length > 0">
          <div class="direction-toggle">
            <button
              class="dir-btn"
              :class="{ 'dir-btn--active': selectedDirection === 'IDA' }"
              @click="selectedDirection = 'IDA'"
            >Ida</button>
            <button
              class="dir-btn"
              :class="{ 'dir-btn--active': selectedDirection === 'VOLTA' }"
              @click="selectedDirection = 'VOLTA'"
            >Volta</button>
          </div>

          <div v-if="filteredLinhas.length === 0" class="no-results">
            <p>Sem trajetos disponiveis para {{ selectedDirection === 'IDA' ? 'Ida' : 'Volta' }}.</p>
          </div>

          <div v-for="linha in filteredLinhas" :key="linha.linhaId" class="linha-card">
            <div class="linha-card-header" @click="toggleLinha(linha.linhaId)">
              <div class="linha-info">
                <q-icon name="directions_bus" size="18px" color="primary" />
                <span class="linha-nome">{{ linha.linhaNome }}</span>
                <span class="linha-destino">- {{ linha.trajetos[0].destinoFinal }}</span>
              </div>
              <q-icon
                :name="expandedLinhas[linha.linhaId] ? 'expand_less' : 'expand_more'"
                size="20px"
                color="grey-6"
              />
            </div>

            <div v-if="expandedLinhas[linha.linhaId]" class="linha-card-body">
              <div v-if="linha.trajetos[0].proximosPasses.length === 0" class="no-passes">
                Sem proximas passagens disponiveis
              </div>
              <div
                v-for="(passe, pIdx) in linha.trajetos[0].proximosPasses"
                :key="pIdx"
                class="passe-item"
              >
                <span class="passe-time">{{ passe.hora }}</span>
                <span class="passe-wait">{{ passe.esperaMinutos }} min</span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div v-if="!loading && searched && !data" class="no-results">
        <q-icon name="search_off" size="48px" color="grey-5" />
        <p>Paragem nao encontrada ou sem dados.</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const stopQuery = ref('')
const selectedStopId = ref(null)
const stopFocused = ref(false)
const loading = ref(false)
const searched = ref(false)
const data = ref(null)
const selectedDirection = ref('IDA')
const expandedLinhas = reactive({})

let allStops = []
const filteredOptions = ref([])

const filteredLinhas = computed(() => {
  if (!data.value) return []
  return data.value.linhas.filter(linha => {
    const trajeto = linha.trajetos.find(t => t.direcao === selectedDirection.value)
    return !!trajeto
  }).map(linha => {
    const trajeto = linha.trajetos.find(t => t.direcao === selectedDirection.value)
    return { ...linha, trajetos: [trajeto] }
  })
})

onMounted(async () => {
  try {
    const response = await fetch('/api/network/paragens', {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      allStops = await response.json()
      filteredOptions.value = buildStopOptions('')
    }
  } catch (e) {
    console.error(e)
  }
})

function onInput() {
  if (selectedStopId.value) {
    selectedStopId.value = null
    data.value = null
    searched.value = false
  }
  filterOptions()
}

function filterOptions() {
  filteredOptions.value = buildStopOptions(stopQuery.value)
}

function buildStopOptions(query) {
  const q = (query || '').trim().toLowerCase()
  return allStops
    .filter(s => !q || s.nome.toLowerCase().includes(q))
    .map(s => ({ label: s.nome, value: s.id }))
    .sort((a, b) => a.label.localeCompare(b.label))
    .slice(0, 80)
}

function selectStop(opt) {
  stopQuery.value = opt.label
  selectedStopId.value = opt.value
  stopFocused.value = false
  loadProximosPasses(opt.value)
}

function clearStop() {
  stopQuery.value = ''
  selectedStopId.value = null
  data.value = null
  searched.value = false
  filterOptions()
}

function handleBlur() {
  setTimeout(() => { stopFocused.value = false }, 150)
}

function toggleLinha(linhaId) {
  expandedLinhas[linhaId] = !expandedLinhas[linhaId]
}

async function loadProximosPasses(stopId) {
  loading.value = true
  searched.value = true

  try {
    const now = new Date()
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
    const day = days[now.getDay() === 0 ? 6 : now.getDay() - 1]

    const response = await fetch(`/api/network/paragens/${stopId}/proximas-passagens?time=${time}&day=${day}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      data.value = await response.json()
      if (data.value) {
        const hasIda = data.value.linhas.some(l => l.trajetos.some(t => t.direcao === 'IDA'))
        selectedDirection.value = hasIda ? 'IDA' : 'VOLTA'
        for (const linha of data.value.linhas) {
          expandedLinhas[linha.linhaId] = true
        }
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.stop-schedule-page {
  background: #f1f5f9;
  min-height: 100vh;
}

.stop-schedule-content {
  padding: calc(var(--header-h, 42px) + 16px) 16px calc(var(--tabbar-h, 78px) + 16px);
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
  color: #028e5c;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.stop-field {
  margin-bottom: 16px;
  position: relative;
}

.stop-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.stop-input-wrapper {
  position: relative;
}

.stop-input {
  width: 100%;
  padding: 12px 36px 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.stop-input:focus {
  border-color: #028e5c;
}

.clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 0 0 10px 10px;
  border-top: none;
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.dropdown-item {
  padding: 10px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  transition: background 0.1s;
}

.dropdown-item:hover {
  background: #f0fdf4;
}

.dropdown-name {
  font-weight: 500;
}

.dropdown-empty {
  padding: 12px 14px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 32px;
  color: #94a3b8;
  font-size: 14px;
}

.results-container {
  margin-top: 8px;
}

.stop-name-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.direction-toggle {
  display: flex;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  margin-bottom: 12px;
}

.dir-btn {
  flex: 1;
  padding: 10px;
  border: none;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  border-right: 1px solid #e2e8f0;
  transition: all 0.15s;
}

.dir-btn:last-child {
  border-right: none;
}

.dir-btn--active {
  background: #0369a1;
  color: #fff;
}

.linha-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-bottom: 10px;
  overflow: hidden;
}

.linha-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.linha-card-header:hover {
  background: #f8fafc;
}

.linha-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.linha-nome {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
}

.linha-destino {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.linha-card-body {
  border-top: 1px solid #f1f5f9;
  padding: 12px 16px 16px;
}

.passes-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.passe-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 8px;
}

.passe-time {
  font-size: 16px;
  font-weight: 700;
  color: #0369a1;
}

.passe-wait {
  font-size: 13px;
  font-weight: 600;
  color: #028e5c;
  background: #f0fdf4;
  padding: 4px 10px;
  border-radius: 12px;
}

.no-passes {
  text-align: center;
  padding: 16px;
  color: #94a3b8;
  font-size: 13px;
}

.no-results {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
}

.no-results p {
  margin: 8px 0 0;
  font-size: 14px;
}
</style>
