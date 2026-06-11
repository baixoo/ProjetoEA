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
  const scheduleOptions = ref([])
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
      const list = await response.json()
      activeTrips.value = list
      activeViagemVeiculo.value = list.find(t => t.startTime && !t.finishTime) || null
    } catch (e) {
      error.value = e.message
      console.error(e)
    }
  }

  async function fetchTrajetos(linhaId) {
    if (!authStore.token) return
    try {
      const url = linhaId
        ? `/api/driver/trajetos?linhaId=${linhaId}`
        : '/api/driver/trajetos'
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter trajetos')
      trajetos.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    }
  }

  async function fetchScheduleOptions(trajetoId) {
    if (!authStore.token) return
    loading.value = true
    try {
      const response = await fetch(`/api/driver/trajetos/${trajetoId}/horarios-disponiveis`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter horários')
      scheduleOptions.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    } finally {
      loading.value = false
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
    trajetos.value = []
    scheduleOptions.value = []
  }

  async function startScheduledViagem(veiculoId, trajetoId, serviceId, gtfsTripId) {
    if (!authStore.token) return
    loading.value = true
    try {
      const response = await fetch('/api/driver/viagens/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ veiculoId, trajetoId, serviceId, gtfsTripId })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao iniciar viagem')
      }
      activeViagemVeiculo.value = await response.json()
      $q.notify({ type: 'positive', message: 'Viagem iniciada!', position: 'top', timeout: 2000 })
    } catch (e) {
      error.value = e.message
      $q.notify({ type: 'negative', message: e.message, position: 'top', timeout: 3000 })
      throw e
    } finally {
      loading.value = false
    }
  }

  async function advanceStop() {
    if (!authStore.token || !activeViagemVeiculo.value) return
    loading.value = true
    try {
      const response = await fetch(`/api/driver/viagens/${activeViagemVeiculo.value.id}/avancar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao avançar paragem')
      }
      const data = await response.json()
      
      // Update local state
      activeViagemVeiculo.value.pontoAtualId = data.pontoAtualId
      if (activeViagemVeiculo.value.veiculo) {
        activeViagemVeiculo.value.veiculo.tempoAtraso = data.tempoAtraso
      }
      
      const v = vehicles.value.find(vh => vh.id === selectedVehicleId.value)
      if (v) {
        v.tempoAtraso = data.tempoAtraso
      }
      
      return data
    } catch (e) {
      error.value = e.message
      $q.notify({ type: 'negative', message: e.message, position: 'top', timeout: 3000 })
      throw e
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
      
      const v = vehicles.value.find(vh => vh.id === selectedVehicleId.value)
      if (v) {
        v.tempoAtraso = 0
        v.lotacaoAtual = 0
      }

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

  async function refreshVehicleMetrics() {
    if (!authStore.token || !selectedVehicleId.value) return
    try {
      const response = await fetch(`/api/driver/veiculos/${selectedVehicleId.value}`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (response.ok) {
        const v = await response.json()
        const idx = vehicles.value.findIndex(vh => vh.id === v.id)
        if (idx !== -1) {
          vehicles.value[idx] = v
        } else {
          vehicles.value.push(v)
        }
      }
    } catch (e) {
      console.error('Error refreshing metrics:', e)
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
    scheduleOptions,
    fetchVehicles,
    fetchActiveTrips,
    fetchTrajetos,
    fetchScheduleOptions,
    startScheduledViagem,
    advanceStop,
    endViagem,
    connectWebSocket,
    disconnectWebSocket,
    clearNotifications,
    disconnect,
    refreshVehicleMetrics
  }
})
