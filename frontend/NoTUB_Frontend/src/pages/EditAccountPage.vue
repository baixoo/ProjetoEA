<template>
  <q-page class="account-page">
    <div class="account-content">
      <div class="account-copy">
        <h1 class="account-title">Editar conta</h1>
        <p class="account-subtitle">Editar os dados da sua conta</p>
      </div>

      <form class="account-form" @submit.prevent="handleSave">
        <div class="field">
          <input v-model="form.email" type="email" placeholder="Email" class="field__input" required />
        </div>
        <div class="field">
          <input v-model="form.nome" type="text" placeholder="Nome" class="field__input" required />
        </div>
        <div class="field">
          <input v-model="form.nif" type="text" placeholder="NIF" class="field__input" maxlength="9" />
        </div>
        <div class="field">
          <input v-model="form.dataNascimento" type="date" placeholder="Data de nascimento" class="field__input" />
        </div>
        <div class="field">
          <input v-model="form.password" type="password" placeholder="Palavra-passe" class="field__input" />
        </div>
        <p v-if="error" class="error-message">{{ error }}</p>
        <p v-if="success" class="success-message">{{ success }}</p>
        <button type="submit" class="btn btn--primary" :disabled="loading">
          {{ loading ? 'A guardar...' : 'Editar' }}
        </button>
      </form>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from 'src/stores/auth'

const authStore = useAuthStore()

const form = reactive({
  email: '',
  nome: '',
  nif: '',
  dataNascimento: '',
  password: ''
})

const error = ref('')
const success = ref('')
const loading = ref(false)

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchUser()
  }
  if (authStore.user) {
    const u = authStore.user
    form.email = u.email || ''
    form.nome = [u.primeiroNome, u.ultimoNome].filter(Boolean).join(' ')
    form.nif = u.nif || ''
    form.dataNascimento = u.dataNascimento || ''
    form.password = '*****'
  }
})

async function handleSave() {
  error.value = ''
  success.value = ''
  loading.value = true

  const nameParts = form.nome.trim().split(/\s+/)
  const primeiroNome = nameParts[0] || ''
  const ultimoNome = nameParts.slice(1).join(' ') || ''

  try {
    await authStore.updateProfile({
      email: form.email,
      primeiroNome,
      ultimoNome,
      nif: form.nif,
      dataNascimento: form.dataNascimento || null
    })
    success.value = 'Perfil atualizado com sucesso!'
  } catch (e) {
    error.value = e.message || 'Erro ao atualizar perfil'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.account-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding-top: 42px;
  padding-bottom: 78px;
  background: #fff;
}

.account-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 0 24px;
  width: 100%;
  max-width: 375px;
}

.account-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.account-title {
  font-size: 16px;
  font-weight: 600;
  color: #000;
  margin: 0;
  line-height: 1.5;
}

.account-subtitle {
  font-size: 14px;
  font-weight: 400;
  color: #000;
  margin: 0;
  line-height: 1.5;
}

.account-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 327px;
  max-width: 100%;
}

.field__input {
  width: 100%;
  height: 40px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 400;
  color: #000;
  outline: none;
  transition: border-color 0.2s;
}

.field__input::placeholder {
  color: #828282;
}

.field__input:focus {
  border-color: #1876d2;
}

.error-message {
  color: #d32f2f;
  font-size: 13px;
  margin: -8px 0;
}

.success-message {
  color: #2e7d32;
  font-size: 13px;
  margin: -8px 0;
}

.btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn--primary {
  background: #000;
  color: #fff;
}

.btn--primary:hover:not(:disabled) {
  opacity: 0.85;
}
</style>
