<template>
  <q-page class="admin-page" padding>
    <div class="page-header">
      <h2 class="page-title">Zonas</h2>
      <q-btn icon="add" label="Nova Zona" color="primary" @click="openCreate" />
    </div>

    <q-table :rows="adminStore.zonas" :columns="columns" row-key="id" flat bordered>
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
      <q-card style="min-width: 320px">
        <q-card-section>
          <div class="text-h6">{{ editing ? 'Editar Zona' : 'Nova Zona' }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none column q-gutter-sm">
          <q-input v-model.number="form.num" label="Numero" type="number" outlined />
          <q-input v-model="form.nome" label="Nome" outlined />
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
const form = reactive({ num: 1, nome: '' })

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'num', label: 'Numero', field: 'num', align: 'left', sortable: true },
  { name: 'nome', label: 'Nome', field: 'nome', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

onMounted(() => adminStore.fetchZonas())

function openCreate() {
  editing.value = false; editId.value = null; form.num = 1; form.nome = ''
  formOpen.value = true
}

function openEdit(row) {
  editing.value = true; editId.value = row.id; form.num = row.num; form.nome = row.nome
  formOpen.value = true
}

async function handleSave() {
  try {
    if (editing.value) await adminStore.updateZona(editId.value, { ...form })
    else await adminStore.createZona({ ...form })
    formOpen.value = false
    await adminStore.fetchZonas()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  if (!confirm('Eliminar zona?')) return
  try { await adminStore.deleteZona(row.id); await adminStore.fetchZonas() } catch (e) { console.error(e) }
}
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 22px; font-weight: 700; color: #0e2d24; margin: 0; }
</style>
