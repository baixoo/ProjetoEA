import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from './auth'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

export const useDriverStore = defineStore('driver', () => {
  const $q = useQuasar()
  const authStore = useAuthStore()
  const vehicles = ref([])
  const activeTrips = ref([])
  const trajetos = ref([])
  const activeViagemVeiculo = ref(null)
  const notifications = ref([])
  const selectedVehicleId = ref(null)
  const connected = ref(false)
  const loading = ref(false)
  const error = ref(null)
  let stompClient = null

  async function fetchVehicles() {
    if (!authStore.token) return
    loading.value = true
    try {
      const response = await fetch('/api/driver/veiculos', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter veiculos')
      vehicles.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchActiveTrips(veiculoId) {
    if (!authStore.token) return
    try {
      const response = await fetch(`/api/driver/veiculos/${veiculoId}/viagens`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter viagens')
      activeTrips.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    }
  }

  function connectWebSocket(veiculoId) {
    disconnectWebSocket()
    selectedVehicleId.value = veiculoId

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        stompClient.subscribe(`/topic/bus.${veiculoId}`, (message) => {
          const notificacao = JSON.parse(message.body)
          notifications.value.unshift({
            ...notificacao,
            id: Date.now()
          })
          if (notifications.value.length > 50) {
            notifications.value = notifications.value.slice(0, 50)
          }
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        connected.value = false
      }
    })

    stompClient.activate()
  }

  function disconnectWebSocket() {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
    connected.value = false
    selectedVehicleId.value = null
  }

  function clearNotifications() {
    notifications.value = []
  }

  function disconnect() {
    disconnectWebSocket()
    notifications.value = []
    activeTrips.value = []
    activeViagemVeiculo.value = null
  }

  async function fetchTrajetos() {
    if (!authStore.token) return
    try {
      const response = await fetch('/api/driver/trajetos', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter trajetos')
      trajetos.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    }
  }

  async function startViagem(veiculoId, trajetoId) {
    if (!authStore.token) return
    loading.value = true
    try {
      const response = await fetch('/api/driver/viagens/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ veiculoId, trajetoId })
      })
      if (!response.ok) throw new Error('Falha ao iniciar viagem')
      activeViagemVeiculo.value = await response.json()
      $q.notify({ type: 'positive', message: 'Viagem iniciada!', position: 'top', timeout: 2000 })
    } catch (e) {
      error.value = e.message
      $q.notify({ type: 'negative', message: e.message, position: 'top', timeout: 3000 })
    } finally {
      loading.value = false
    }
  }

  async function endViagem() {
    if (!authStore.token || !activeViagemVeiculo.value) return
    loading.value = true
    try {
      const response = await fetch(`/api/driver/viagens/${activeViagemVeiculo.value.id}/end`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao terminar viagem')
      activeViagemVeiculo.value = null
      notifications.value = []
      $q.notify({ type: 'info', message: 'Viagem terminada', position: 'top', timeout: 2000 })
    } catch (e) {
      error.value = e.message
      $q.notify({ type: 'negative', message: e.message, position: 'top', timeout: 3000 })
    } finally {
      loading.value = false
    }
  }

  return {
    vehicles,
    activeTrips,
    trajetos,
    activeViagemVeiculo,
    notifications,
    selectedVehicleId,
    connected,
    loading,
    error,
    fetchVehicles,
    fetchActiveTrips,
    fetchTrajetos,
    startViagem,
    endViagem,
    connectWebSocket,
    disconnectWebSocket,
    clearNotifications,
    disconnect
  }
})
