<template>
  <q-page class="admin-page" padding>
    <div class="page-header">
      <div class="header-title-container">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="$router.push('/admin')" class="q-mr-xs" />
        <h2 class="page-title">Utilizadores</h2>
      </div>
    </div>

    <q-table
      :rows="adminStore.users"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :rows-per-page-options="[10, 25, 50]"
    >
      <template v-slot:body-cell-role="props">
        <q-td :props="props">
          <q-badge :color="props.row.role === 'ADMINISTRADOR' ? 'red' : 'grey'">
            {{ props.row.role }}
          </q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-authMethod="props">
        <q-td :props="props">
          <q-badge :color="props.row.authMethod === 'GOOGLE' ? 'blue' : 'green'">
            {{ props.row.authMethod || 'N/A' }}
          </q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn
            v-if="props.row.role !== 'ADMINISTRADOR'"
            dense flat color="red" icon="admin_panel_settings"
            @click="handlePromote(props.row)"
          >
            <q-tooltip>Tornar Admin</q-tooltip>
          </q-btn>
          <q-btn
            v-if="props.row.role === 'ADMINISTRADOR'"
            dense flat color="grey" icon="person"
            @click="handleDemote(props.row)"
          >
            <q-tooltip>Remover Admin</q-tooltip>
          </q-btn>
          <q-btn dense flat color="red" icon="delete" @click="handleDelete(props.row)">
            <q-tooltip>Eliminar</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="confirmOpen">
      <q-card>
        <q-card-section>
          <p>{{ confirmMessage }}</p>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn color="red" label="Confirmar" @click="confirmAction" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAdminStore } from 'src/stores/admin'

const adminStore = useAdminStore()
const confirmOpen = ref(false)
const confirmMessage = ref('')
const pendingAction = ref(null)

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'primeiroNome', label: 'Nome', field: 'primeiroNome', align: 'left' },
  { name: 'email', label: 'Email', field: 'email', align: 'left' },
  { name: 'nif', label: 'NIF', field: 'nif', align: 'left' },
  { name: 'role', label: 'Role', field: 'role', align: 'left', sortable: true },
  { name: 'authMethod', label: 'Auth', field: 'authMethod', align: 'left' },
  { name: 'actions', label: 'Acoes', field: 'actions', align: 'center' }
]

onMounted(() => adminStore.fetchUsers())

function handlePromote(user) {
  confirmMessage.value = `Tornar ${user.primeiroNome} administrador?`
  pendingAction.value = async () => {
    await adminStore.updateUserRole(user.id, 'ADMINISTRADOR')
    await adminStore.fetchUsers()
  }
  confirmOpen.value = true
}

function handleDemote(user) {
  confirmMessage.value = `Remover ${user.primeiroNome} de administrador?`
  pendingAction.value = async () => {
    await adminStore.updateUserRole(user.id, 'UTILIZADOR')
    await adminStore.fetchUsers()
  }
  confirmOpen.value = true
}

function handleDelete(user) {
  confirmMessage.value = `Eliminar utilizador ${user.primeiroNome}? Esta acao e irreversivel.`
  pendingAction.value = async () => {
    await adminStore.deleteUser(user.id)
    await adminStore.fetchUsers()
  }
  confirmOpen.value = true
}

async function confirmAction() {
  confirmOpen.value = false
  if (pendingAction.value) {
    try {
      await pendingAction.value()
    } catch (e) {
      console.error(e)
    }
    pendingAction.value = null
  }
}
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-title-container { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 22px; font-weight: 700; color: #0e2d24; margin: 0; }
</style>
