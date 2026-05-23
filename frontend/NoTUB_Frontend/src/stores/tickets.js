import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

export const useTicketsStore = defineStore('tickets', () => {
  const authStore = useAuthStore()
  const tickets = ref([])
  const activePass = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchMyTickets() {
    if (!authStore.token) return
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/tickets/meus-bilhetes', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter bilhetes')
      tickets.value = await response.json()
    } catch (e) {
      error.value = e.message
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchMyPass() {
    if (!authStore.token) return
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/tickets/meu-passe', {
        headers: { Authorization: `Bearer ${authStore.token}` }
      })
      if (!response.ok) throw new Error('Falha ao obter passe')
      const data = await response.json().catch(() => null)
      activePass.value = data
    } catch (e) {
      error.value = e.message
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  async function checkout(checkoutRequest) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/pagamento/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify(checkoutRequest)
      })
      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.erro || 'Falha ao iniciar pagamento')
      }
      return await response.json()
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function checkPaymentStatus(transacaoId) {
    if (!authStore.token) throw new Error('Não autenticado')
    const response = await fetch(`/api/pagamento/${transacaoId}/estado`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (!response.ok) throw new Error('Falha ao verificar estado')
    return await response.json()
  }

  async function buyTickets(quantidade, zonaIds) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/tickets/comprar', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ quantidade, zonaIds })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao comprar bilhetes')
      }
      const data = await response.json()
      await fetchMyTickets()
      return data
    } catch (e) {
      error.value = e.message
      throw e;
    } finally {
      loading.value = false
    }
  }

  async function buyPass(modalidade, zonaIds) {
    if (!authStore.token) throw new Error('Não autenticado')
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/tickets/passe/comprar', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authStore.token}`
        },
        body: JSON.stringify({ modalidade, zonaIds })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao comprar passe')
      }
      const data = await response.json()
      await fetchMyPass()
      return data
    } catch (e) {
      error.value = e.message
      throw e;
    } finally {
      loading.value = false
    }
  }

  return {
    tickets,
    activePass,
    loading,
    error,
    fetchMyTickets,
    fetchMyPass,
    checkout,
    checkPaymentStatus,
    buyTickets,
    buyPass
  }
})
