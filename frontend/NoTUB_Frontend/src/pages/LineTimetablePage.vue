<template>
  <q-page class="timetable-page">
    <div class="timetable-content">
      <div class="page-header">
        <q-btn flat round dense icon="arrow_back" @click="router.push('/routes')" class="back-btn" />
        <h2 class="page-title">Horarios</h2>
        <p class="page-subtitle">Consulte os horarios de cada linha</p>
      </div>

      <div class="linha-selector">
        <q-select
          v-model="selectedLinhaId"
          :options="linhaOptions"
          label="Linha"
          outlined
          dense
          emit-value
          map-options
          use-input
          input-debounce="200"
          @filter="filterLinhas"
          @update:model-value="loadHorarios"
        >
          <template v-slot:prepend>
            <q-icon name="directions_bus" color="primary" />
          </template>
        </q-select>
      </div>

      <div class="filters-row">
        <div class="service-toggle">
          <button
            v-for="svc in serviceOptions"
            :key="svc.value"
            class="svc-btn"
            :class="{ 'svc-btn--active': selectedService === svc.value }"
            @click="selectedService = svc.value; loadHorarios()"
          >{{ svc.label }}</button>
        </div>
        <div class="direction-toggle" v-if="horarios.length > 1">
          <button
            v-for="dir in availableDirections"
            :key="dir.idx"
            class="dir-btn"
            :class="{ 'dir-btn--active': selectedDirection === dir.idx }"
            @click="selectedDirection = dir.idx"
          >{{ dir.label }}</button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <q-spinner size="32px" color="primary" />
        <span>A carregar horarios...</span>
      </div>

      <div v-if="!loading && currentTrajeto" class="table-container">
        <div class="table-header-custom">
          <div class="linha-title-row">
            <q-icon name="directions_bus" color="primary" size="20px" />
            <span class="linha-name">{{ headerName }}</span>
          </div>
        </div>

        <div class="table-scroll">
          <table class="tt-table">
            <thead>
              <tr>
                <th class="th-stop">Paragem</th>
                <th class="th-horarios">Horarios</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(paragem, pIdx) in currentTrajeto.paragens" :key="pIdx">
                <td class="td-stop">
                  <span class="stop-dot" :class="{ 'dot-first': pIdx === 0, 'dot-last': pIdx === currentTrajeto.paragens.length - 1 }"></span>
                  <span class="stop-text" :class="{ 'text-first': pIdx === 0, 'text-last': pIdx === currentTrajeto.paragens.length - 1 }">{{ paragem.nome }}</span>
                </td>
                <td class="td-horarios">
                  <div class="times-grid" v-if="formatTimes(paragem.horarios).length > 0">
                    <span
                      v-for="(time, tIdx) in formatTimes(paragem.horarios)"
                      :key="tIdx"
                      class="time-val"
                    >{{ time }}</span>
                  </div>
                  <span v-else class="no-times">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="!loading && selectedLinhaId && horarios.length === 0" class="no-horarios">
        <q-icon name="schedule" size="48px" color="grey-5" />
        <p>Sem horarios disponiveis para esta selecao.</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const selectedLinhaId = ref(null)
const selectedService = ref('UTEIS')
const selectedDirection = ref(0)
const linhaOptions = ref([])
const horarios = ref([])
const loading = ref(false)

const serviceOptions = [
  { label: 'Dia util', value: 'UTEIS' },
  { label: 'Sabado', value: 'SAB' },
  { label: 'Domingo', value: 'DOM' }
]

const availableDirections = computed(() => {
  return horarios.value.map((h, idx) => ({
    idx,
    label: h.direcao === 'IDA' ? 'Ida' : 'Volta'
  }))
})

const currentTrajeto = computed(() => {
  if (horarios.value.length === 0) return null
  const idx = Math.min(selectedDirection.value, horarios.value.length - 1)
  return horarios.value[idx]
})

const headerName = computed(() => {
  if (!currentTrajeto.value) return ''
  const nome = currentTrajeto.value.linhaNome || ''
  const parts = nome.split(' - ')
  const code = parts[0] || ''
  const paragens = currentTrajeto.value.paragens || []
  const destStop = paragens.length > 0 ? paragens[paragens.length - 1]?.nome || '' : ''
  return `${code} - ${destStop}`
})

function formatTimes(horariosArr) {
  if (!horariosArr || !Array.isArray(horariosArr) || horariosArr.length === 0) return []
  return horariosArr
    .map(h => {
      const raw = h.hora || ''
      // hora comes as "HH:mm:ss" or "HH:mm" from LocalTime
      const parts = raw.split(':')
      const hr = parseInt(parts[0] || '0', 10)
      const min = parseInt(parts[1] || '0', 10)
      return { hr, min }
    })
    .sort((a, b) => a.hr - b.hr || a.min - b.min)
    .map(t => String(t.hr).padStart(2, '0') + ':' + String(t.min).padStart(2, '0'))
}

let allLinhas = []

onMounted(async () => {
  try {
    const response = await fetch('/api/network/linhas/resumo', {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      allLinhas = await response.json()
      linhaOptions.value = allLinhas.map(l => ({ label: l.nome, value: l.id }))
    }
  } catch (e) {
    console.error(e)
  }
})

function filterLinhas(val, update) {
  update(() => {
    if (!val) {
      linhaOptions.value = allLinhas.map(l => ({ label: l.nome, value: l.id }))
    } else {
      const q = val.toLowerCase()
      linhaOptions.value = allLinhas
        .filter(l => l.nome.toLowerCase().includes(q))
        .map(l => ({ label: l.nome, value: l.id }))
    }
  })
}

async function loadHorarios() {
  if (!selectedLinhaId.value) return
  loading.value = true
  horarios.value = []
  selectedDirection.value = 0

  try {
    const response = await fetch(`/api/network/linhas/${selectedLinhaId.value}/horarios-paragens?serviceId=${selectedService.value}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (response.ok) {
      horarios.value = await response.json()
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.timetable-page {
  background: #f1f5f9;
  min-height: 100vh;
}

.timetable-content {
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

.linha-selector {
  margin-bottom: 12px;
}

.filters-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.service-toggle {
  display: flex;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.svc-btn {
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

.svc-btn:last-child {
  border-right: none;
}

.svc-btn--active {
  background: #028e5c;
  color: #fff;
}

.direction-toggle {
  display: flex;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
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

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 32px;
  color: #94a3b8;
  font-size: 14px;
}

.table-container {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.table-header-custom {
  padding: 12px 16px;
  background: #f0f9ff;
  border-bottom: 1px solid #e2e8f0;
}

.linha-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.linha-name {
  font-size: 16px;
  font-weight: 800;
  color: #0369a1;
}

.table-scroll {
  overflow-y: auto;
  max-height: 60vh;
  -webkit-overflow-scrolling: touch;
}

.tt-table {
  border-collapse: collapse;
  width: 100%;
}

.tt-table thead {
  position: sticky;
  top: 0;
  z-index: 2;
}

.tt-table th {
  background: #f8fafc;
  padding: 8px 12px;
  font-weight: 700;
  color: #475569;
  text-align: left;
  border-bottom: 2px solid #e2e8f0;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.th-stop {
  min-width: 120px;
}

.th-horarios {
  min-width: 200px;
}

.tt-table tbody tr {
  border-bottom: 1px solid #f1f5f9;
}

.tt-table tbody tr:hover {
  background: #f8fafc;
}

.td-stop {
  padding: 10px 12px;
  vertical-align: top;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.stop-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.dot-first {
  background: #10b981;
  width: 10px;
  height: 10px;
}

.dot-last {
  background: #ef4444;
  width: 10px;
  height: 10px;
}

.stop-text {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.text-first {
  color: #10b981;
  font-weight: 700;
}

.text-last {
  color: #ef4444;
  font-weight: 700;
}

.td-horarios {
  padding: 10px 12px;
  vertical-align: top;
}

.times-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.time-val {
  font-size: 12px;
  font-weight: 600;
  color: #0369a1;
  background: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.no-times {
  font-size: 12px;
  color: #cbd5e1;
}

.no-horarios {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
}

.no-horarios p {
  margin: 8px 0 0;
  font-size: 14px;
}
</style>
