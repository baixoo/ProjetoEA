<template>
  <q-page class="admin-page" padding>
    <div class="page-header">
      <div class="header-title-container">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="$router.push('/admin')" class="q-mr-xs" />
        <h2 class="page-title">Viagens de Veiculo</h2>
      </div>
      <q-btn icon="add" label="Nova Viagem" color="primary" @click="openCreate" />
    </div>

    <q-table :rows="adminStore.viagens" :columns="columns" row-key="id" flat bordered>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn dense flat color="red" icon="delete" @click="handleDelete(props.row)">
            <q-tooltip>Eliminar</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="formOpen">
      <q-card style="min-width: 360px">
        <q-card-section>
          <div class="text-h6">Nova Viagem</div>
        </q-card-section>
        <q-card-section class="q-pt-none column q-gutter-sm">
          <q-input v-model="form.tripId" label="Trip ID" outlined />
          <q-select v-model="form.veiculoId" label="Veiculo" outlined
            :options="vehicleOptions" emit-value map-options />
          <q-select v-model="form.trajetoId" label="Trajeto" outlined
            :options="trajetoOptions" emit-value map-options />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn color="primary" label="Criar" @click="handleSave" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useAdminStore } from 'src/stores/admin'

const adminStore = useAdminStore()
const formOpen = ref(false)
const form = reactive({ tripId: '', veiculoId: null, trajetoId: null })

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left' },
  { name: 'tripId', label: 'Trip ID', field: 'tripId', align: 'left' },
  { name: 'veiculo', label: 'Veiculo', field: v => v.veiculo?.matricula || v.veiculoId, align: 'left' },
  { name: 'trajeto', label: 'Trajeto', field: v => v.trajeto?.id || v.trajetoId, align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

const vehicleOptions = computed(() => adminStore.vehicles.map(v => ({ label: v.matricula, value: v.id })))
const trajetoOptions = computed(() => adminStore.trajetos.map(t => ({ label: `Trajeto ${t.id}`, value: t.id })))

onMounted(async () => {
  await Promise.all([
    adminStore.fetchViagens(),
    adminStore.fetchVehicles(),
    adminStore.fetchTrajetos()
  ])
})

function openCreate() {
  form.tripId = ''; form.veiculoId = null; form.trajetoId = null
  formOpen.value = true
}

async function handleSave() {
  try {
    await adminStore.createViagem({
      tripId: form.tripId,
      veiculo: { id: form.veiculoId },
      trajeto: { id: form.trajetoId }
    })
    formOpen.value = false
    await adminStore.fetchViagens()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  if (!confirm('Eliminar viagem?')) return
  try { await adminStore.deleteViagem(row.id); await adminStore.fetchViagens() } catch (e) { console.error(e) }
}
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-title-container { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 22px; font-weight: 700; color: #0e2d24; margin: 0; }
</style>
