<template>
  <q-page class="admin-page" padding>
    <div class="page-header">
      <div class="header-title-container">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="$router.push('/admin')" class="q-mr-xs" />
        <h2 class="page-title">Veículos</h2>
      </div>
      <q-btn icon="add" label="Novo Veiculo" color="primary" @click="openCreate" />
    </div>

    <q-table :rows="adminStore.vehicles" :columns="columns" row-key="id" flat bordered>
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
          <div class="text-h6">{{ editing ? 'Editar Veiculo' : 'Novo Veiculo' }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-input v-model="form.matricula" label="Matricula" outlined class="q-mb-sm" />
          <q-input v-model.number="form.nLugares" label="Lugares" type="number" outlined />
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

const form = reactive({ matricula: '', nLugares: 40 })

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'matricula', label: 'Matricula', field: 'matricula', align: 'left' },
  { name: 'nLugares', label: 'Lugares', field: 'nLugares', align: 'left' },
  { name: 'lotacaoAtual', label: 'Lotacao', field: 'lotacaoAtual', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

onMounted(() => adminStore.fetchVehicles())

function openCreate() {
  editing.value = false
  editId.value = null
  form.matricula = ''
  form.nLugares = 40
  formOpen.value = true
}

function openEdit(row) {
  editing.value = true
  editId.value = row.id
  form.matricula = row.matricula
  form.nLugares = row.nLugares
  formOpen.value = true
}

async function handleSave() {
  try {
    if (editing.value) {
      await adminStore.updateVehicle(editId.value, { matricula: form.matricula, nLugares: form.nLugares })
    } else {
      await adminStore.createVehicle({ matricula: form.matricula, nLugares: form.nLugares })
    }
    formOpen.value = false
    await adminStore.fetchVehicles()
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(row) {
  if (!confirm(`Eliminar veiculo ${row.matricula}?`)) return
  try {
    await adminStore.deleteVehicle(row.id)
    await adminStore.fetchVehicles()
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
