import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!token.value)

  async function login(email, password) {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    })
    if (!response.ok) {
      const data = await response.json().catch(() => ({ message: 'Credenciais inválidas' }))
      throw new Error(data.message || 'O login falhou')
    }
    const data = await response.json()
    token.value = data.token
    localStorage.setItem('token', data.token)
    await fetchUser()
  }

  async function register(userData) {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userData)
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || 'O registo falhou')
    }
    const data = await response.json()
    // Se o backend retornar um token após registro
    if (data.token) {
      token.value = data.token
      localStorage.setItem('token', data.token)
      await fetchUser()
    }
  }

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  let fetchUserPromise = null

  async function fetchUser() {
    if (!token.value) return
    if (fetchUserPromise) return fetchUserPromise
    fetchUserPromise = (async () => {
      try {
        const response = await fetch('/api/auth/me', {
          headers: { Authorization: `Bearer ${token.value}` }
        })
        if (response.ok) {
          user.value = await response.json()
        } else if (response.status === 401) {
          logout()
        }
      } catch {
        // network error — keep token, retry on next navigation
      } finally {
        fetchUserPromise = null
      }
    })()
    return fetchUserPromise
  }

  async function updateProfile(profileData) {
    const response = await fetch('/api/utilizadores/perfil', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token.value}`
      },
      body: JSON.stringify(profileData)
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || 'Erro ao atualizar perfil')
    }
    user.value = await response.json()
  }

  async function forgotPassword(email) {
    const response = await fetch('/api/auth/esqueceu-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || 'Erro ao solicitar recuperação')
    }
  }

  async function resetPassword(token, novaPassword) {
    const response = await fetch('/api/auth/redefinir-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, novaPassword })
    })
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || 'Erro ao redefinir palavra-passe')
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    register,
    setToken,
    fetchUser,
    updateProfile,
    forgotPassword,
    resetPassword,
    logout
  }
})
