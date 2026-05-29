import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

export const useDriverStore = defineStore('driver', () => {
  const authStore = useAuthStore()
  const vehicles = ref([])
  const activeTrips = ref([])
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
  }

  return {
    vehicles,
    activeTrips,
    notifications,
    selectedVehicleId,
    connected,
    loading,
    error,
    fetchVehicles,
    fetchActiveTrips,
    connectWebSocket,
    disconnectWebSocket,
    clearNotifications,
    disconnect
  }
})
