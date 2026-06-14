<template>
  <q-page class="admin-page" padding>
    <div class="page-header page-header--main">
      <div class="header-title-container">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="$router.push('/admin')" class="q-mr-xs" />
        <h2 class="page-title">Rede de Transporte</h2>
      </div>
    </div>

    <q-tabs v-model="activeTab" class="text-primary q-mb-md">
      <q-tab name="linhas" label="Linhas" icon="route" />
      <q-tab name="trajetos" label="Trajetos" icon="alt_route" />
      <q-tab name="paragens" label="Paragens" icon="place" />
    </q-tabs>

    <!-- LINHAS -->
    <div v-if="activeTab === 'linhas'">
      <div class="page-header">
        <span></span>
        <q-btn icon="add" label="Nova Linha" color="primary" size="sm" @click="openLinhaCreate" />
      </div>
      <q-table :rows="adminStore.linhas" :columns="linhaCols" row-key="id" flat bordered>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn dense flat color="primary" icon="edit" @click="openLinhaEdit(props.row)" />
            <q-btn dense flat color="red" icon="delete" @click="handleDeleteLinha(props.row)" />
          </q-td>
        </template>
      </q-table>
      <q-dialog v-model="linhaFormOpen">
        <q-card style="min-width: 360px">
          <q-card-section><div class="text-h6">{{ linhaEditing ? 'Editar' : 'Nova' }} Linha</div></q-card-section>
          <q-card-section class="q-pt-none column q-gutter-sm">
            <q-input v-model="linhaForm.nome" label="Nome" outlined />
            <q-input v-model="linhaForm.identificadorServico" label="Identificador" outlined />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn color="primary" label="Guardar" @click="handleSaveLinha" />
          </q-card-actions>
        </q-card>
      </q-dialog>
    </div>

    <!-- TRAJETOS -->
    <div v-if="activeTab === 'trajetos'">
      <div class="page-header">
        <span></span>
        <q-btn icon="add" label="Novo Trajeto" color="primary" size="sm" @click="openTrajetoCreate" />
      </div>
      <q-table :rows="adminStore.trajetos" :columns="trajetoCols" row-key="id" flat bordered>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn dense flat color="red" icon="delete" @click="handleDeleteTrajeto(props.row)" />
          </q-td>
        </template>
      </q-table>
      <q-dialog v-model="trajetoFormOpen">
        <q-card style="min-width: 360px">
          <q-card-section><div class="text-h6">Novo Trajeto</div></q-card-section>
          <q-card-section class="q-pt-none column q-gutter-sm">
            <q-select v-model="trajetoForm.direcao" label="Direcao" outlined :options="['IDA','VOLTA']" />
            <q-select v-model="trajetoForm.linhaId" label="Linha" outlined
              :options="linhaOptions" emit-value map-options />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn color="primary" label="Criar" @click="handleSaveTrajeto" />
          </q-card-actions>
        </q-card>
      </q-dialog>
    </div>

    <!-- PARAGENS -->
    <div v-if="activeTab === 'paragens'">
      <div class="page-header">
        <span></span>
        <q-btn icon="add" label="Nova Paragem" color="primary" size="sm" @click="openParagemCreate" />
      </div>
      <q-table :rows="adminStore.paragens" :columns="paragemCols" row-key="id" flat bordered>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn dense flat color="primary" icon="edit" @click="openParagemEdit(props.row)" />
            <q-btn dense flat color="red" icon="delete" @click="handleDeleteParagem(props.row)" />
          </q-td>
        </template>
      </q-table>
      <q-dialog v-model="paragemFormOpen">
        <q-card style="min-width: 360px">
          <q-card-section><div class="text-h6">{{ paragemEditing ? 'Editar' : 'Nova' }} Paragem</div></q-card-section>
          <q-card-section class="q-pt-none column q-gutter-sm">
            <q-input v-model="paragemForm.nome" label="Nome" outlined />
            <q-input v-model.number="paragemForm.latitude" label="Latitude" type="number" step="0.0001" outlined />
            <q-input v-model.number="paragemForm.longitude" label="Longitude" type="number" step="0.0001" outlined />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn color="primary" label="Guardar" @click="handleSaveParagem" />
          </q-card-actions>
        </q-card>
      </q-dialog>
    </div>
  </q-page>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useAdminStore } from 'src/stores/admin'

const adminStore = useAdminStore()
const activeTab = ref('linhas')

const linhaOptions = computed(() => adminStore.linhas.map(l => ({ label: l.nome, value: l.id })))

const linhaCols = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'nome', label: 'Nome', field: 'nome', align: 'left' },
  { name: 'identificadorServico', label: 'Identificador', field: 'identificadorServico', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

const trajetoCols = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'direcao', label: 'Direcao', field: 'direcao', align: 'left' },
  { name: 'linha', label: 'Linha', field: t => t.linha?.nome || '-', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

const paragemCols = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'nome', label: 'Nome', field: 'nome', align: 'left' },
  { name: 'latitude', label: 'Lat', field: p => p.localizacao?.latitude ?? '-', align: 'left' },
  { name: 'longitude', label: 'Lng', field: p => p.localizacao?.longitude ?? '-', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

onMounted(() => Promise.all([
  adminStore.fetchLinhas(), adminStore.fetchTrajetos(), adminStore.fetchParagens()
]))

// LINHAS
const linhaFormOpen = ref(false)
const linhaEditing = ref(false)
const linhaEditId = ref(null)
const linhaForm = reactive({ nome: '', identificadorServico: '' })

function openLinhaCreate() {
  linhaEditing.value = false; linhaEditId.value = null
  linhaForm.nome = ''; linhaForm.identificadorServico = ''
  linhaFormOpen.value = true
}
function openLinhaEdit(row) {
  linhaEditing.value = true; linhaEditId.value = row.id
  linhaForm.nome = row.nome; linhaForm.identificadorServico = row.identificadorServico
  linhaFormOpen.value = true
}
async function handleSaveLinha() {
  try {
    if (linhaEditing.value) await adminStore.updateLinha(linhaEditId.value, { ...linhaForm })
    else await adminStore.createLinha({ ...linhaForm })
    linhaFormOpen.value = false; await adminStore.fetchLinhas()
  } catch (e) { console.error(e) }
}
async function handleDeleteLinha(row) {
  if (!confirm('Eliminar linha?')) return
  try { await adminStore.deleteLinha(row.id); await adminStore.fetchLinhas() } catch (e) { console.error(e) }
}

// TRAJETOS
const trajetoFormOpen = ref(false)
const trajetoForm = reactive({ direcao: 'IDA', linhaId: null })

function openTrajetoCreate() {
  trajetoForm.direcao = 'IDA'; trajetoForm.linhaId = null
  trajetoFormOpen.value = true
}
async function handleSaveTrajeto() {
  try {
    await adminStore.createTrajeto({
      direcao: trajetoForm.direcao,
      linha: { id: trajetoForm.linhaId }
    })
    trajetoFormOpen.value = false; await adminStore.fetchTrajetos()
  } catch (e) { console.error(e) }
}
async function handleDeleteTrajeto(row) {
  if (!confirm('Eliminar trajeto?')) return
  try { await adminStore.deleteTrajeto(row.id); await adminStore.fetchTrajetos() } catch (e) { console.error(e) }
}

// PARAGENS
const paragemFormOpen = ref(false)
const paragemEditing = ref(false)
const paragemEditId = ref(null)
const paragemForm = reactive({ nome: '', latitude: 41.55, longitude: -8.42 })

function openParagemCreate() {
  paragemEditing.value = false; paragemEditId.value = null
  paragemForm.nome = ''; paragemForm.latitude = 41.55; paragemForm.longitude = -8.42
  paragemFormOpen.value = true
}
function openParagemEdit(row) {
  paragemEditing.value = true; paragemEditId.value = row.id
  paragemForm.nome = row.nome
  paragemForm.latitude = row.localizacao?.latitude ?? 41.55
  paragemForm.longitude = row.localizacao?.longitude ?? -8.42
  paragemFormOpen.value = true
}
async function handleSaveParagem() {
  try {
    const data = { nome: paragemForm.nome, localizacao: { latitude: paragemForm.latitude, longitude: paragemForm.longitude } }
    if (paragemEditing.value) await adminStore.updateParagem(paragemEditId.value, data)
    else await adminStore.createParagem(data)
    paragemFormOpen.value = false; await adminStore.fetchParagens()
  } catch (e) { console.error(e) }
}
async function handleDeleteParagem(row) {
  if (!confirm('Eliminar paragem?')) return
  try { await adminStore.deleteParagem(row.id); await adminStore.fetchParagens() } catch (e) { console.error(e) }
}
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header--main { margin-bottom: 8px; }
.header-title-container { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 22px; font-weight: 700; color: #0e2d24; margin: 0; }
</style>
