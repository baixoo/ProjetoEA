import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './auth'

export const useTicketsStore = defineStore('tickets', () => {
  const authStore = useAuthStore()
  const tickets = ref([])
  const passes = ref([])        // todos os passes do utilizador
  const passeAtivo = ref(null)  // passe ativo neste momento (ou null)
  const loading = ref(false)
  const error = ref(null)

  // Meses já ocupados por algum passe: Set de strings "YYYY-MM"
  const mesesOcupados = computed(() => {
    const ocupados = new Set()
    for (const passe of passes.value) {
      const inicio = new Date(passe.inicio)
      const fim = new Date(passe.fim)
      const cur = new Date(inicio.getFullYear(), inicio.getMonth(), 1)
      const end = new Date(fim.getFullYear(), fim.getMonth(), 1)
      while (cur <= end) {
        const key = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}`
        ocupados.add(key)
        cur.setMonth(cur.getMonth() + 1)
      }
    }
    return ocupados
  })

  // Passes futuros (inicio > agora)
  const passesFuturos = computed(() => {
    const now = new Date()
    return passes.value.filter(p => new Date(p.inicio) > now)
  })

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

  async function fetchMyPasses() {
    if (!authStore.token) return
    loading.value = true
    error.value = null
    try {
      const [passesRes, ativoRes] = await Promise.all([
        fetch('/api/tickets/meus-passes', {
          headers: { Authorization: `Bearer ${authStore.token}` }
        }),
        fetch('/api/tickets/meu-passe/ativo', {
          headers: { Authorization: `Bearer ${authStore.token}` }
        })
      ])
      if (!passesRes.ok) throw new Error('Falha ao obter passes')
      passes.value = await passesRes.json()
      passeAtivo.value = ativoRes.status === 204 ? null : await ativoRes.json()
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

  async function checkPaymentStatus(token) {
    if (!authStore.token) throw new Error('Não autenticado')
    const response = await fetch(`/api/pagamento/estado?t=${encodeURIComponent(token)}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (!response.ok) throw new Error('Falha ao verificar estado')
    return await response.json()
  }

  async function fetchPrice(tipoProduto, nrZonas, modalidade) {
    if (!authStore.token) return null
    const params = new URLSearchParams({
      tipoProduto,
      nrZonas: String(nrZonas)
    })
    if (modalidade) params.set('modalidade', modalidade)
    const tipoUtilizador = authStore.tipoUtilizador || authStore.user?.tipoUtilizador
    if (tipoUtilizador) params.set('tipoUtilizador', tipoUtilizador)
    const response = await fetch(`/api/tarifas/calculadora?${params}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    })
    if (!response.ok) return null
    const data = await response.json()
    return data.valor ?? null
  }

  async function buyTickets(quantidade, zonaId) {
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
        body: JSON.stringify({ quantidade, zonaId })
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
      throw e
    } finally {
      loading.value = false
    }
  }

  async function buyPass(modalidade, zonaId, mesInicio, anoInicio) {
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
        body: JSON.stringify({ modalidade, zonaId, mesInicio, anoInicio })
      })
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Falha ao comprar passe')
      }
      const data = await response.json()
      await fetchMyPasses()
      return data
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    tickets,
    passes,
    passeAtivo,
    mesesOcupados,
    passesFuturos,
    loading,
    error,
    fetchMyTickets,
    fetchMyPasses,
    checkout,
    checkPaymentStatus,
    fetchPrice,
    buyTickets,
    buyPass
  }
})
