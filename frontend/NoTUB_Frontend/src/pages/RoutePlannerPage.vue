<template>
  <q-page class="route-page">
    <div class="route-content">
      <div class="route-header">
        <q-btn flat round dense icon="arrow_back" @click="router.push('/')" class="back-btn" />
        <h2 class="route-title">Planeamento de Rota</h2>
        <p class="route-subtitle">Encontre a melhor ligacao entre paragens</p>
      </div>

      <div class="route-form">
        <div class="stop-field">
          <label class="stop-label">
            <q-icon name="trip_origin" size="16px" color="positive" />
            <span>Origem</span>
          </label>
          <div class="stop-input-wrapper">
            <input
              type="text"
              class="stop-input"
              placeholder="Pesquisar paragem de origem..."
              v-model="origemQuery"
              @focus="origemFocused = true"
              @blur="handleBlur('origem')"
              @input="onOrigemInput"
              autocomplete="off"
            />
            <button v-if="origemId" class="clear-btn" @click="clearOrigem">&times;</button>
          </div>
          <div v-if="origemFocused && !origemId" class="dropdown">
            <div
              v-for="(opt, idx) in filteredOrigemOptions"
              :key="idx"
              class="dropdown-item"
              @mousedown.prevent="selectOrigem(opt)"
            >
              <span class="dropdown-name">{{ opt.label }}</span>
            </div>
            <div v-if="filteredOrigemOptions.length === 0" class="dropdown-empty">
              Nenhuma paragem encontrada
            </div>
          </div>
        </div>

        <div class="swap-btn-container">
          <q-btn flat round dense icon="swap_vert" color="primary" @click="swapStops" />
        </div>

        <div class="stop-field">
          <label class="stop-label">
            <q-icon name="place" size="16px" color="negative" />
            <span>Destino</span>
          </label>
          <div class="stop-input-wrapper">
            <input
              type="text"
              class="stop-input"
              placeholder="Pesquisar paragem de destino..."
              v-model="destinoQuery"
              @focus="destinoFocused = true"
              @blur="handleBlur('destino')"
              @input="onDestinoInput"
              autocomplete="off"
            />
            <button v-if="destinoId" class="clear-btn" @click="clearDestino">&times;</button>
          </div>
          <div v-if="destinoFocused && !destinoId" class="dropdown">
            <div
              v-for="(opt, idx) in filteredDestinoOptions"
              :key="idx"
              class="dropdown-item"
              @mousedown.prevent="selectDestino(opt)"
            >
              <span class="dropdown-name">{{ opt.label }}</span>
            </div>
            <div v-if="filteredDestinoOptions.length === 0" class="dropdown-empty">
              Nenhuma paragem encontrada
            </div>
          </div>
        </div>

        <button class="btn-search" @click="searchRoute" :disabled="loading || !origemId || !destinoId">
          <q-spinner v-if="loading" size="20px" class="q-mr-sm" />
          <q-icon v-else name="route" size="20px" class="q-mr-sm" />
          <span>Procurar Rota</span>
        </button>

        <button class="btn-timetables" @click="router.push('/routes/timetables')">
          <q-icon name="schedule" size="18px" class="q-mr-sm" />
          <span>Horarios por Linha</span>
        </button>

        <button class="btn-timetables btn-stop-schedule" @click="router.push('/routes/stop-schedule')">
          <q-icon name="directions_bus" size="18px" class="q-mr-sm" />
          <span>Proximo Autocarro</span>
        </button>
      </div>

      <div v-if="error" class="error-msg q-mt-md">{{ error }}</div>

      <div v-if="rotas.length > 0" class="route-results q-mt-md">
        <div
          v-for="(rota, rIdx) in rotas"
          :key="rIdx"
          class="route-option"
          :class="{ 'route-option--selected': selectedRoute === rIdx }"
          @click="selectedRoute = rIdx"
        >
          <div class="route-option-header">
            <div class="route-option-badges">
              <span v-if="rota.caminho" class="badge badge--walk">
                <q-icon name="directions_walk" size="12px" class="q-mr-xs" /> A pe
              </span>
              <span v-else-if="rota.direta" class="badge badge--direct">Direta</span>
              <span v-else class="badge badge--transfer">{{ rota.trocas }} troca{{ rota.trocas !== 1 ? 's' : '' }}</span>
            </div>
            <div class="route-option-times">
              <span class="time-departure">{{ rota.horaPartida }}</span>
              <q-icon name="arrow_forward" size="14px" class="time-arrow" />
              <span class="time-arrival">{{ rota.horaChegada }}</span>
            </div>
            <div class="route-option-meta">
              <span>{{ formatMinutes(rota.totalMinutos) }}</span>
              <span v-if="rota.totalCaminhadaMinutos > 0" class="meta-walk">
                <q-icon name="directions_walk" size="14px" />
                ~{{ formatMinutesCompact(rota.totalCaminhadaMinutos) }}
              </span>
              <span v-if="rota.nrZonas > 0 && formatZones(rota.zonas)" class="meta-zones">
                <q-icon name="map" size="14px" />
                Zonas: {{ formatZones(rota.zonas) }} ({{ rota.nrZonas }})
              </span>
            </div>
          </div>
        </div>

        <div class="selected-route-detail">
          <div class="segments-list">
            <template v-for="(seg, idx) in rotas[selectedRoute]?.segmentos || []" :key="idx">
              <div class="segment-card">
                <div class="segment-header" @click="toggleSegment(idx)">
                  <div class="segment-badges">
                    <div class="line-badge">
                      <q-icon :name="seg.linhaNome === 'A pe' ? 'directions_walk' : 'directions_bus'" size="14px" />
                      <span>{{ seg.linhaNome }}</span>
                    </div>
                    <div v-if="seg.nrZonas > 0 && formatZones(seg.zonas)" class="segment-zones">
                      <q-icon name="map" size="12px" />
                      <span>{{ formatZones(seg.zonas) }}</span>
                    </div>
                    <div v-if="seg.destinoFinal" class="direction-dest">
                      <q-icon name="arrow_forward" size="12px" />
                      <span>{{ seg.destinoFinal }}</span>
                    </div>
                  </div>
                  <div class="segment-time-info">
                    <span class="seg-departure">{{ seg.horaPartida }}</span>
                    <span class="seg-duration">{{ formatMinutes(seg.duracaoMinutos) }}</span>
                    <span class="seg-arrival">{{ seg.horaChegada }}</span>
                    <q-icon
                      :name="expandedSegments[idx] ? 'expand_less' : 'expand_more'"
                      size="18px"
                      color="grey-6"
                      class="expand-icon"
                    />
                  </div>
                </div>

                <div v-if="seg.esperaMinutos > 0" class="wait-info">
                  <q-icon name="schedule" size="14px" />
                  <span>Proximo autocarro em {{ formatMinutes(seg.esperaMinutos) }}</span>
                </div>

                <div v-if="expandedSegments[idx]" class="segment-stops">
                  <div v-for="(p, pIdx) in seg.paragens" :key="pIdx" class="stop-point">
                    <div class="stop-line-container">
                      <div class="stop-dot" :class="{ 'first': pIdx === 0, 'last': pIdx === seg.paragens.length - 1 }"></div>
                      <div v-if="pIdx < seg.paragens.length - 1" class="stop-connector"></div>
                    </div>
                    <span class="stop-name" :class="{ 'bold': pIdx === 0 || pIdx === seg.paragens.length - 1 }">{{ p.nome }}</span>
                  </div>
                </div>

                <div v-else class="segment-collapsed" @click="seg.linhaNome !== 'A pe' && toggleSegment(idx)">
                  <div class="collapsed-stops">
                    <span class="collapsed-origin">{{ seg.paragens?.[0]?.nome }}</span>
                    <q-icon name="arrow_forward" size="14px" color="grey-5" />
                    <span class="collapsed-dest">{{ seg.paragens?.[seg.paragens.length - 1]?.nome }}</span>
                    <span v-if="seg.linhaNome !== 'A pe'" class="collapsed-count">{{ seg.paragens?.length }} paragens</span>
                  </div>
                </div>

                <button v-if="seg.trajetoId && seg.linhaNome !== 'A pe'" class="btn-proximos" @click.stop="loadProximosPasses(seg, idx)">
                  <q-icon name="schedule" size="14px" />
                  <span>Proximos autocarros</span>
                  <q-icon v-if="loadingProximos[idx]" name="hourglass_top" size="14px" class="q-ml-sm" />
                </button>

                <div v-if="proximosPasses[idx]?.length" class="proximos-list">
                  <div v-for="(pp, ppIdx) in proximosPasses[idx]" :key="ppIdx" class="proximo-item">
                    <span class="proximo-hora">{{ pp.hora }}</span>
                    <span class="proximo-espera">{{ pp.esperaMinutos === 0 ? 'agora' : `em ${formatMinutes(pp.esperaMinutos)}` }}</span>
                  </div>
                </div>
              </div>

              <div v-if="idx < (rotas[selectedRoute]?.segmentos || []).length - 1" class="transfer-indicator">
                <template v-if="rotas[selectedRoute].segmentos[idx + 1]?.linhaNome === 'A pe'">
                  <q-icon name="directions_walk" size="16px" />
                  <span>A pe ~{{ formatMinutesCompact(rotas[selectedRoute].segmentos[idx + 1].duracaoMinutos) }}</span>
                </template>
                <template v-else-if="seg.tempoCaminhadaMinutos > 0">
                  <q-icon name="directions_walk" size="16px" />
                  <div class="transfer-indicator-text">
                    <span>Troca (~{{ formatMinutesCompact(seg.tempoCaminhadaMinutos) }} a pe)</span>
                    <span v-if="rotas[selectedRoute].segmentos[idx + 1]?.esperaMinutos > 0" class="transfer-wait">
                      Espere {{ formatMinutes(rotas[selectedRoute].segmentos[idx + 1].esperaMinutos) }} pelo autocarro
                    </span>
                  </div>
                </template>
                <template v-else>
                  <q-icon name="swap_horiz" size="16px" />
                  <div class="transfer-indicator-text">
                    <span>Troca de linha</span>
                    <span v-if="rotas[selectedRoute].segmentos[idx + 1]?.esperaMinutos > 0" class="transfer-wait">
                      Espere {{ formatMinutes(rotas[selectedRoute].segmentos[idx + 1].esperaMinutos) }} pelo autocarro
                    </span>
                  </div>
                </template>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div v-if="noRoute" class="no-route q-mt-md">
        <q-icon name="bus_alert" size="48px" color="grey-5" />
        <p>Nao foi encontrada rota entre estas paragens.</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useViagensStore } from 'src/stores/viagens'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const viagensStore = useViagensStore()

const origemId = ref(null)
const destinoId = ref(null)
const origemQuery = ref('')
const destinoQuery = ref('')
const origemFocused = ref(false)
const destinoFocused = ref(false)
const rotas = ref([])
const selectedRoute = ref(0)
const loading = ref(false)
const error = ref(null)
const noRoute = ref(false)
const expandedSegments = reactive({})
const proximosPasses = reactive({})
const loadingProximos = reactive({})

let allStopOptions = []

function toggleSegment(idx) {
  expandedSegments[idx] = !expandedSegments[idx]
}

function formatMinutesCompact(minutes) {
  if (!Number.isFinite(minutes) || minutes <= 0) return '0 min'
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${hours}h${String(mins).padStart(2, '0')}m`
}

function formatMinutes(minutes) {
  return formatMinutesCompact(minutes)
}

function zoneSignature(zonas) {
  if (!Array.isArray(zonas)) return ''
  return zonas
    .filter(zona => Number.isFinite(Number(zona?.num)))
    .map(zona => `${zona.num}:${zona.nome || ''}`)
    .join(',')
}

function formatZones(zonas) {
  if (!Array.isArray(zonas)) return ''
  return zonas
    .filter(zona => Number.isFinite(Number(zona?.num)))
    .map(zona => `Z${zona.num}`)
    .join(', ')
}

function buildVisibleRouteKey(rota) {
  const segmentos = (rota.segmentos || [])
    .map(seg => [
      seg.linhaNome || '',
      seg.direcao || '',
      seg.destinoFinal || '',
      seg.origem?.nome || '',
      seg.destino?.nome || '',
      seg.horaPartida || '',
      seg.horaChegada || '',
      seg.nrZonas ?? 0,
      zoneSignature(seg.zonas)
    ].join('|'))
    .join('||')

  return [
    rota.caminho ? 'walk' : 'transit',
    rota.direta ? 'direct' : 'transfer',
    rota.trocas ?? 0,
    rota.nrZonas ?? 0,
    zoneSignature(rota.zonas),
    rota.horaPartida || '',
    rota.horaChegada || '',
    segmentos
  ].join('###')
}

function dedupeRoutes(routes) {
  const bestByKey = new Map()

  for (const rota of routes || []) {
    const key = buildVisibleRouteKey(rota)
    const current = bestByKey.get(key)
    if (!current || (rota.totalMinutos ?? Number.MAX_SAFE_INTEGER) < (current.totalMinutos ?? Number.MAX_SAFE_INTEGER)) {
      bestByKey.set(key, rota)
    }
  }

  return [...bestByKey.values()]
}

function getCurrentTimeParam() {
  const now = new Date()
  return String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0')
}

function getCurrentDayParam() {
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  return days[new Date().getDay() === 0 ? 6 : new Date().getDay() - 1]
}

onMounted(async () => {
  if (!viagensStore.stops || viagensStore.stops.length === 0) {
    await viagensStore.fetchStops()
  }
  buildOptions()
})

function buildOptions() {
  const stops = viagensStore.stops || []
  const groups = new Map()
  for (const s of stops) {
    if (!groups.has(s.nome)) groups.set(s.nome, [])
    groups.get(s.nome).push(s)
  }
  allStopOptions = []
  for (const [nome, group] of groups) {
    const zoneNames = [...new Set(group.map(s => s.zonaNome).filter(Boolean))]
    const zoneSuffix = zoneNames.length > 0 ? ` (${zoneNames.join(', ')})` : ''
    allStopOptions.push({ label: `${nome}${zoneSuffix}`, value: group[0].id, nome })
  }
  allStopOptions.sort((a, b) => a.label.localeCompare(b.label))
}

function filterOptions(query) {
  if (!query) return allStopOptions.slice(0, 50)
  const q = query.toLowerCase()
  return allStopOptions.filter(o => o.label.toLowerCase().includes(q)).slice(0, 50)
}

const filteredOrigemOptions = computed(() => filterOptions(origemQuery.value))
const filteredDestinoOptions = computed(() => filterOptions(destinoQuery.value))

function onOrigemInput() {
  origemId.value = null
  origemFocused.value = true
}
function onDestinoInput() {
  destinoId.value = null
  destinoFocused.value = true
}

function selectOrigem(opt) {
  origemId.value = opt.value
  origemQuery.value = opt.label
  origemFocused.value = false
}
function selectDestino(opt) {
  destinoId.value = opt.value
  destinoQuery.value = opt.label
  destinoFocused.value = false
}

function clearOrigem() {
  origemId.value = null
  origemQuery.value = ''
}
function clearDestino() {
  destinoId.value = null
  destinoQuery.value = ''
}

function swapStops() {
  const tmpId = origemId.value
  const tmpQ = origemQuery.value
  origemId.value = destinoId.value
  origemQuery.value = destinoQuery.value
  destinoId.value = tmpId
  destinoQuery.value = tmpQ
}

function handleBlur(which) {
  setTimeout(() => {
    if (which === 'origem') origemFocused.value = false
    else destinoFocused.value = false
  }, 200)
}

async function searchRoute() {
  if (!origemId.value || !destinoId.value) return
  loading.value = true
  error.value = null
  rotas.value = []
  noRoute.value = false
  selectedRoute.value = 0
  Object.keys(expandedSegments).forEach(k => delete expandedSegments[k])
  Object.keys(proximosPasses).forEach(k => delete proximosPasses[k])

  try {
    const time = getCurrentTimeParam()
    const day = getCurrentDayParam()
    const response = await fetch(`/api/network/route?from=${origemId.value}&to=${destinoId.value}&time=${time}&day=${day}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.status === 404) {
      noRoute.value = true
      return
    }
    if (!response.ok) throw new Error('Erro ao procurar rota')
    rotas.value = dedupeRoutes(await response.json())
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadProximosPasses(seg, idx) {
  if (proximosPasses[idx]?.length) {
    delete proximosPasses[idx]
    return
  }
  const tId = seg.trajetoId
  if (!tId) return
  loadingProximos[idx] = true
  try {
    const time = getCurrentTimeParam()
    const day = getCurrentDayParam()
    const paragemId = seg.origem?.id
    const response = await fetch(`/api/network/trajetos/${tId}/proximas-passagens?paragemId=${paragemId}&time=${time}&day=${day}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      proximosPasses[idx] = await response.json()
    }
  } catch (e) {
    console.error(e)
  } finally {
    delete loadingProximos[idx]
  }
}
</script>

<style scoped>
.route-page {
  background: #f1f5f9;
  min-height: 100vh;
}

.route-content {
  padding: calc(var(--header-h, 42px) + 16px) 16px calc(var(--tabbar-h, 78px) + 16px);
  max-width: 500px;
  margin: 0 auto;
}

.route-header {
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

.route-title {
  font-size: 22px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0 0 4px;
}

.route-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.route-form {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stop-field {
  position: relative;
}

.stop-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 4px;
}

.stop-input-wrapper {
  position: relative;
}

.stop-input {
  width: 100%;
  padding: 10px 32px 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.stop-input:focus {
  border-color: #028e5c;
}

.stop-input::placeholder {
  color: #94a3b8;
}

.clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.clear-btn:hover {
  color: #64748b;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-top: 4px;
  max-height: 240px;
  overflow-y: auto;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.dropdown-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.1s;
}

.dropdown-item:hover {
  background: #f0fdf4;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-name {
  font-size: 13px;
  color: #1e293b;
}

.dropdown-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.swap-btn-container {
  text-align: center;
}

.btn-search {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 46px;
  background: #028e5c;
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.2s;
}

.btn-search:hover {
  background: #03694a;
  transform: translateY(-1px);
}

.btn-search:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-timetables {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 40px;
  background: transparent;
  border: 1px solid #028e5c;
  border-radius: 10px;
  color: #028e5c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 6px;
  transition: all 0.2s;
}

.btn-timetables:hover {
  background: #f0fdf4;
}

.error-msg {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  color: #dc2626;
  font-size: 13px;
}

.route-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.route-option {
  background: #fff;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.route-option:hover {
  border-color: #028e5c;
}

.route-option--selected {
  border-color: #028e5c;
  background: #f0fdf4;
}

.route-option-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.route-option-badges {
  display: flex;
  gap: 6px;
}

.badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 20px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
}

.badge--direct {
  background: #dcfce7;
  color: #166534;
}

.badge--walk {
  background: #e0f2fe;
  color: #0369a1;
}

.badge--transfer {
  background: #fef3c7;
  color: #92400e;
}

.route-option-times {
  display: flex;
  align-items: center;
  gap: 6px;
}

.time-departure, .time-arrival {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.time-arrow {
  color: #94a3b8;
}

.route-option-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.meta-walk {
  display: flex;
  align-items: center;
  gap: 3px;
  color: #028e5c;
}

.meta-zones {
  display: flex;
  align-items: center;
  gap: 3px;
  color: #475569;
}

.selected-route-detail {
  margin-top: 4px;
}

.segments-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.segment-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 6px;
  cursor: pointer;
}

.segment-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.line-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #e0f2fe;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  color: #0369a1;
}

.segment-zones {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 7px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.direction-dest {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segment-time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.seg-departure {
  font-weight: 700;
  color: #028e5c;
}

.seg-duration {
  color: #94a3b8;
  font-weight: 600;
}

.seg-arrival {
  font-weight: 700;
  color: #dc2626;
}

.expand-icon {
  cursor: pointer;
}

.wait-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #0369a1;
  font-weight: 600;
  margin-bottom: 8px;
  background: #f0f9ff;
  padding: 4px 8px;
  border-radius: 6px;
}

.segment-stops {
  display: flex;
  flex-direction: column;
  margin-top: 4px;
}

.segment-collapsed {
  margin-top: 4px;
  cursor: pointer;
}

.collapsed-stops {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.collapsed-origin {
  font-weight: 700;
  color: #028e5c;
}

.collapsed-dest {
  font-weight: 700;
  color: #dc2626;
}

.collapsed-count {
  font-size: 11px;
  color: #94a3b8;
  margin-left: auto;
}

.stop-point {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 30px;
}

.stop-line-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
  flex-shrink: 0;
}

.stop-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.stop-dot.first {
  background: #10b981;
  width: 14px;
  height: 14px;
}

.stop-dot.last {
  background: #ef4444;
  width: 14px;
  height: 14px;
}

.stop-connector {
  width: 2px;
  height: 20px;
  background: #cbd5e1;
}

.stop-name {
  font-size: 13px;
  color: #475569;
}

.stop-name.bold {
  font-weight: 700;
  color: #1e293b;
}

.btn-proximos {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 4px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #0369a1;
  cursor: pointer;
  width: 100%;
  justify-content: center;
  transition: all 0.15s;
}

.btn-proximos:hover {
  background: #e0f2fe;
}

.proximos-list {
  margin-top: 8px;
  background: #f0f9ff;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.proximo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  font-size: 13px;
}

.proximo-hora {
  font-weight: 700;
  color: #0369a1;
}

.proximo-espera {
  color: #64748b;
  font-weight: 600;
}

.transfer-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  padding: 8px;
  margin: 2px 0;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

.transfer-indicator-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.transfer-wait {
  font-size: 11px;
  color: #0369a1;
}

.no-route {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
}

.no-route p {
  margin: 8px 0 0;
  font-size: 14px;
}
</style>
