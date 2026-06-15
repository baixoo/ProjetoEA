<template>
  <q-page class="admin-page" padding>
    <div class="page-header">
      <div class="header-title-container">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="$router.push('/admin')" class="q-mr-xs" />
        <h2 class="page-title">Tarifas</h2>
      </div>
      <q-btn icon="add" label="Nova Tarifa" color="primary" @click="openCreate" />
    </div>

    <q-table :rows="adminStore.tarifas" :columns="columns" row-key="id" flat bordered>
      <template v-slot:body-cell-valor="props">
        <q-td :props="props">{{ props.row.valor?.toFixed(2) }} EUR</q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn dense flat color="primary" icon="edit" @click="openEdit(props.row)">
            <q-tooltip>Editar</q-tooltip>
          </q-btn>
          <q-btn dense flat color="red" icon="delete" @click="handleDelete(props.row)">
            <q-tooltip>Eliminar</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="formOpen">
      <q-card style="min-width: 360px">
        <q-card-section>
          <div class="text-h6">{{ editing ? 'Editar Tarifa' : 'Nova Tarifa' }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none column q-gutter-sm">
          <q-input v-model.number="form.valor" label="Valor (EUR)" type="number" step="0.01" outlined />
          <q-select v-model="form.tipoUtilizador" label="Tipo Utilizador" outlined
            :options="['CRIANCA','ESTUDANTE','ADULTO','SENIOR']" />
          <q-select v-model="form.modalidade" label="Modalidade" outlined
            :options="['MENSAL','ANUAL','SEMANAL','H24','H48','H72']" clearable />
          <q-input v-model.number="form.nrZonas" label="Nr Zonas" type="number" min="1" max="3" step="1" outlined />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn color="primary" label="Guardar" @click="handleSave" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useAdminStore } from 'src/stores/admin'

const adminStore = useAdminStore()
const formOpen = ref(false)
const editing = ref(false)
const editId = ref(null)

const form = reactive({ valor: 0, tipoUtilizador: 'ADULTO', modalidade: null, nrZonas: 1 })

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'tipoUtilizador', label: 'Tipo', field: 'tipoUtilizador', align: 'left', sortable: true },
  { name: 'modalidade', label: 'Modalidade', field: 'modalidade', align: 'left' },
  { name: 'nrZonas', label: 'Zonas', field: 'nrZonas', align: 'left' },
  { name: 'valor', label: 'Valor', field: 'valor', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

onMounted(() => adminStore.fetchTarifas())

function openCreate() {
  editing.value = false
  editId.value = null
  form.valor = 0; form.tipoUtilizador = 'ADULTO'; form.modalidade = null; form.nrZonas = 1
  formOpen.value = true
}

function openEdit(row) {
  editing.value = true
  editId.value = row.id
  form.valor = row.valor; form.tipoUtilizador = row.tipoUtilizador
  form.modalidade = row.modalidade; form.nrZonas = row.nrZonas
  formOpen.value = true
}

async function handleSave() {
  try {
    if (editing.value) {
      await adminStore.updateTarifa(editId.value, { ...form })
    } else {
      await adminStore.createTarifa({ ...form })
    }
    formOpen.value = false
    await adminStore.fetchTarifas()
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(row) {
  if (!confirm('Eliminar tarifa?')) return
  try {
    await adminStore.deleteTarifa(row.id)
    await adminStore.fetchTarifas()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-title-container { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 22px; font-weight: 700; color: #0e2d24; margin: 0; }
</style>
