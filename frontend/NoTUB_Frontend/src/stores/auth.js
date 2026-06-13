import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!token.value)

  async function parseErrorResponse(response, defaultMessage) {
    try {
      const data = await response.json()
      return data.mensagem || data.message || data.error || defaultMessage
    } catch {
      const text = await response.text().catch(() => '')
      return text || defaultMessage
    }
  }

  async function login(email, password) {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    })
    if (!response.ok) {
      const message = await parseErrorResponse(response, 'Credenciais inválidas.')
      throw new Error(message)
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
      const message = await parseErrorResponse(response, 'O registo falhou.')
      throw new Error(message)
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

  async function fetchUser() {
    if (!token.value) return
    try {
      const response = await fetch('/api/auth/me', {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      if (response.ok) {
        user.value = await response.json()
      } else {
        logout()
      }
    } catch {
      logout()
    }
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
      const message = await parseErrorResponse(response, 'Erro ao atualizar perfil')
      throw new Error(message)
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
      const message = await parseErrorResponse(response, 'Erro ao solicitar recuperação')
      throw new Error(message)
    }
  }

  async function resetPassword(token, novaPassword) {
    const response = await fetch('/api/auth/redefinir-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, novaPassword })
    })
    if (!response.ok) {
      const message = await parseErrorResponse(response, 'Erro ao redefinir palavra-passe')
      throw new Error(message)
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