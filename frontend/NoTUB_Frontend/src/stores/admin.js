import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

export const useAdminStore = defineStore('admin', () => {
  const authStore = useAuthStore()

  function headers() {
    return { Authorization: `Bearer ${authStore.token}` }
  }

  const stats = ref(null)
  const users = ref([])
  const vehicles = ref([])
  const tarifas = ref([])
  const zonas = ref([])
  const viagens = ref([])
  const linhas = ref([])
  const trajetos = ref([])
  const paragens = ref([])

  async function fetchStats() {
    const r = await fetch('/api/admin/stats', { headers: headers() })
    if (r.ok) stats.value = await r.json()
  }

  async function fetchUsers() {
    const r = await fetch('/api/admin/utilizadores', { headers: headers() })
    if (r.ok) users.value = await r.json()
  }

  async function updateUserRole(id, role) {
    const r = await fetch(`/api/admin/utilizadores/${id}/role`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ role })
    })
    if (!r.ok) throw new Error('Erro ao alterar role')
    return await r.json()
  }

  async function deleteUser(id) {
    const r = await fetch(`/api/admin/utilizadores/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar utilizador')
    return await r.text()
  }

  async function fetchVehicles() {
    const r = await fetch('/api/veiculos', { headers: headers() })
    if (r.ok) vehicles.value = await r.json()
  }

  async function createVehicle(data) {
    const r = await fetch('/api/admin/veiculos', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar veiculo')
    return await r.json()
  }

  async function updateVehicle(id, data) {
    const r = await fetch(`/api/admin/veiculos/${id}`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao atualizar veiculo')
    return await r.json()
  }

  async function deleteVehicle(id) {
    const r = await fetch(`/api/admin/veiculos/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar veiculo')
    return await r.text()
  }

  async function fetchTarifas() {
    const r = await fetch('/api/tarifas', { headers: headers() })
    if (r.ok) tarifas.value = await r.json()
  }

  async function createTarifa(data) {
    const r = await fetch('/api/admin/tarifas', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar tarifa')
    return await r.json()
  }

  async function updateTarifa(id, data) {
    const r = await fetch(`/api/admin/tarifas/${id}`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao atualizar tarifa')
    return await r.json()
  }

  async function deleteTarifa(id) {
    const r = await fetch(`/api/admin/tarifas/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar tarifa')
    return await r.text()
  }

  async function fetchZonas() {
    const r = await fetch('/api/zonas', { headers: headers() })
    if (r.ok) zonas.value = await r.json()
  }

  async function createZona(data) {
    const r = await fetch('/api/admin/zonas', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar zona')
    return await r.json()
  }

  async function updateZona(id, data) {
    const r = await fetch(`/api/admin/zonas/${id}`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao atualizar zona')
    return await r.json()
  }

  async function deleteZona(id) {
    const r = await fetch(`/api/admin/zonas/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar zona')
    return await r.text()
  }

  async function fetchViagens() {
    const r = await fetch('/api/admin/viagens/veiculo', { headers: headers() })
    if (!r.ok) {
      const r2 = await fetch('/api/viagens/veiculo', { headers: headers() })
      if (r2.ok) viagens.value = await r2.json()
      return
    }
    viagens.value = await r.json()
  }

  async function createViagem(data) {
    const r = await fetch('/api/admin/viagens/veiculo', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar viagem')
    return await r.json()
  }

  async function deleteViagem(id) {
    const r = await fetch(`/api/admin/viagens/veiculo/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar viagem')
    return await r.text()
  }

  async function fetchLinhas() {
    const r = await fetch('/api/network/linhas', { headers: headers() })
    if (r.ok) linhas.value = await r.json()
  }

  async function fetchTrajetos() {
    const r = await fetch('/api/network/trajetos', { headers: headers() })
    if (r.ok) trajetos.value = await r.json()
  }

  async function fetchParagens() {
    const r = await fetch('/api/network/paragens', { headers: headers() })
    if (r.ok) paragens.value = await r.json()
  }

  async function createLinha(data) {
    const r = await fetch('/api/admin/network/linhas', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar linha')
    return await r.json()
  }

  async function updateLinha(id, data) {
    const r = await fetch(`/api/admin/network/linhas/${id}`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao atualizar linha')
    return await r.json()
  }

  async function deleteLinha(id) {
    const r = await fetch(`/api/admin/network/linhas/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar linha')
    return await r.text()
  }

  async function createTrajeto(data) {
    const r = await fetch('/api/admin/network/trajetos', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar trajeto')
    return await r.json()
  }

  async function deleteTrajeto(id) {
    const r = await fetch(`/api/admin/network/trajetos/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar trajeto')
    return await r.text()
  }

  async function createParagem(data) {
    const r = await fetch('/api/admin/network/paragens', {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao criar paragem')
    return await r.json()
  }

  async function updateParagem(id, data) {
    const r = await fetch(`/api/admin/network/paragens/${id}`, {
      method: 'PUT',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!r.ok) throw new Error('Erro ao atualizar paragem')
    return await r.json()
  }

  async function deleteParagem(id) {
    const r = await fetch(`/api/admin/network/paragens/${id}`, {
      method: 'DELETE',
      headers: headers()
    })
    if (!r.ok) throw new Error('Erro ao eliminar paragem')
    return await r.text()
  }

  return {
    stats, users, vehicles, tarifas, zonas, viagens, linhas, trajetos, paragens,
    fetchStats, fetchUsers, updateUserRole, deleteUser,
    fetchVehicles, createVehicle, updateVehicle, deleteVehicle,
    fetchTarifas, createTarifa, updateTarifa, deleteTarifa,
    fetchZonas, createZona, updateZona, deleteZona,
    fetchViagens, createViagem, deleteViagem,
    fetchLinhas, fetchTrajetos, fetchParagens,
    createLinha, updateLinha, deleteLinha,
    createTrajeto, deleteTrajeto,
    createParagem, updateParagem, deleteParagem
  }
})
