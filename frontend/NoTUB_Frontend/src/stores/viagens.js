import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'

export const useViagensStore = defineStore('viagens', () => {
  const authStore = useAuthStore()
  const activeTrip = ref(null)
  const stops = ref([])
  const vehicleTrips = ref([])
  const zones = ref([])
  const loading = ref(false)
  const error = ref(null)

  const passengerStompClient = ref(null)
  const passengerConnected = ref(false)

  async function fetchActiveTrip() {
    if (!authStore.token) return null
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/viagens/utilizador', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter viagens')
      const list = await response.json()
      const active = list.find(t => t.estado === 'ATIVA')
      activeTrip.value = active || null
      return active || null
    } catch (e) {
      error.value = e.message
      console.error(e)
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchStops() {
    try {
      const response = await fetch('/api/network/paragens')
      if (!response.ok) throw new Error('Falha ao obter paragens')
      stops.value = await response.json()
    } catch (e) {
      console.error(e)
    }
  }

  async function fetchVehicleTrips() {
    if (!authStore.token) return
    try {
      const response = await fetch('/api/viagens/veiculo', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter viagens de veículos')
      vehicleTrips.value = await response.json()
    } catch (e) {
      console.error(e)
    }
  }

  async function fetchZones() {
    try {
      const response = await fetch('/api/zonas')
      if (!response.ok) throw new Error('Falha ao obter zonas')
      zones.value = await response.json()
    } catch (e) {
      console.error(e)
    }
  }

  async function startTrip({ tipoTitulo, quantidade, paragemEntradaId, viagemVeiculoId }) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/viagens/utilizador/iniciar', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ tipoTitulo, quantidade, paragemEntradaId, viagemVeiculoId })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao iniciar viagem')
      }
      const trip = await response.json()
      activeTrip.value = trip
      return trip
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function endTrip(tripId, pontoPassagemSaidaId) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`/api/viagens/utilizador/${tripId}/terminar`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ pontoPassagemSaidaId })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao terminar viagem')
      }
      const trip = await response.json()
      return trip
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function validateTicket(tituloId) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`/api/validacao/${tituloId}/usar`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authStore.token}`
        }
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao validar título')
      }
      return await response.json()
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchZonasVeiculo(viagemVeiculoId, paragemId) {

    const authStore_local = useAuthStore()
    const tokenAtual = authStore_local.token

    if (!tokenAtual) throw new Error('Não autenticado')
    try {
      const response = await fetch(`/api/viagens/veiculo/${viagemVeiculoId}/${paragemId}/zona_min_max`, {
        headers: { Authorization: `Bearer ${tokenAtual}` }
      })
      if (!response.ok) throw new Error('Falha ao obter zonas do trajeto')
      return await response.json() 
    } catch (e) {
      console.error(e)
      throw e
    }
  }

  function connectPassengerWebSocket() { 
    disconnectPassengerWebSocket()

    const viagemVeiculoId = activeTrip.value?.viagemVeiculo?.id 
    
    if (!viagemVeiculoId) {
      console.warn('Não foi possível conectar ao WebSocket: Nenhuma viagem ativa encontrada na store.')
      return
    }

    const socket = new SockJS('/ws') 
    
    passengerStompClient.value = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => console.log(str),
      onConnect: (frame) => {
        passengerConnected.value = true
        console.log(`Conectado ao WebSocket do passageiro para a viagem ${viagemVeiculoId}:`, frame)

        passengerStompClient.value.subscribe(`/topic/bus.${viagemVeiculoId}.route`, (message) => {
          const dados = JSON.parse(message.body)
          
          if (dados.pontoAtualId !== undefined) {
            if (activeTrip.value && activeTrip.value.viagemVeiculo) {
              activeTrip.value.viagemVeiculo.pontoAtualId = Number(dados.pontoAtualId)
            }
          } 
          
          else if (dados.viagemStatus === 'END') {
            console.log('[WebSocket] O motorista terminou o circuito. A encerrar viagem do passageiro...')
            
            activeTrip.value.estado = 'END'
            console.log('Estado da viagem atualizado para : ', activeTrip.value.estado)
            disconnectPassengerWebSocket()
          }
        })
      },
      onDisconnect: () => {
        passengerConnected.value = false 
      },
      onStompError: (frame) => {
        console.error('Erro STOMP no passageiro:', frame)
        passengerConnected.value = false 
      }
    })

    passengerStompClient.value.activate()
  }

  function disconnectPassengerWebSocket() {
    if (passengerStompClient.value) {
      passengerStompClient.value.deactivate()
      passengerStompClient.value = null 
      passengerConnected.value = false
      console.log('WebSocket desativado.')
    }
  }

  return {
    activeTrip,
    stops,
    vehicleTrips,
    zones,
    loading,
    error,
    fetchActiveTrip,
    fetchStops,
    fetchVehicleTrips,
    fetchZones,
    startTrip,
    endTrip,
    validateTicket,
    fetchZonasVeiculo,
    connectPassengerWebSocket,
    disconnectPassengerWebSocket
  }
})
