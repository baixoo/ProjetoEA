<template>
  <q-page class="auth-page">
    <div class="auth-content">
      <div class="auth-copy">
        <h1 class="auth-title">Criar uma conta</h1>
        <p class="auth-subtitle">Preencha com os seus dados</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
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
          <input v-model="form.password" type="password" placeholder="Palavra-passe" class="field__input" required />
        </div>
        <p v-if="error" class="error-message">{{ error }}</p>
        <p v-if="success" class="success-message">{{ success }}</p>
        <button type="submit" class="btn btn--primary" :disabled="loading">
          {{ loading ? 'A criar conta...' : 'Continuar' }}
        </button>
      </form>

      <div class="divider">
        <div class="divider__line"></div>
        <span class="divider__text">ou</span>
        <div class="divider__line"></div>
      </div>

      <button class="btn btn--google" @click="googleLogin">
        <img src="/assets/google-logo.svg" alt="Google" class="google-icon" />
        <span>Continuar com o Google</span>
      </button>

      <p class="auth-link-text">
        Já tem uma conta? <router-link to="/signin" class="auth-link">Iniciar sessão</router-link>.
      </p>
      <p class="auth-terms">
        Ao clicar em continuar, você concorda com os nossos <strong>Termos de Serviço</strong> e com a <strong>Política de Privacidade</strong>
      </p>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
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

async function handleRegister() {
  error.value = ''
  success.value = ''
  loading.value = true

  const nameParts = form.nome.trim().split(/\s+/)
  const primeiroNome = nameParts[0] || ''
  const ultimoNome = nameParts.slice(1).join(' ') || ''

  try {
    await authStore.register({
      email: form.email,
      primeiroNome,
      ultimoNome,
      nif: form.nif,
      dataNascimento: form.dataNascimento || null,
      password: form.password
    })
    success.value = 'Conta criada com sucesso!'
    setTimeout(() => router.push('/signin'), 1500)
  } catch (e) {
    error.value = e.message || 'Erro ao criar conta'
  } finally {
    loading.value = false
  }
}

function googleLogin() {
  window.location.href = '/oauth2/authorization/google'
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding-top: 42px;
  background: #fff;
}

.auth-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 0 24px;
  width: 100%;
  max-width: 375px;
}

.auth-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.auth-title {
  font-size: 16px;
  font-weight: 600;
  color: #000;
  margin: 0;
  line-height: 1.5;
}

.auth-subtitle {
  font-size: 14px;
  font-weight: 400;
  color: #000;
  margin: 0;
  line-height: 1.5;
}

.auth-form {
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
  gap: 8px;
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

.btn--google {
  background: #eee;
  color: #000;
  width: 327px;
  max-width: 100%;
}

.btn--google:hover {
  background: #e0e0e0;
}

.google-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  object-fit: contain;
}

.divider {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 327px;
  max-width: 100%;
}

.divider__line {
  flex: 1;
  height: 1px;
  background: #e6e6e6;
}

.divider__text {
  font-size: 14px;
  color: #828282;
  white-space: nowrap;
}

.auth-link-text {
  font-size: 12px;
  color: #828282;
  text-align: center;
  margin: 0;
  line-height: 1.5;
}

.auth-link {
  color: #2196f3;
  text-decoration: none;
}

.auth-link:hover {
  text-decoration: underline;
}

.auth-terms {
  font-size: 12px;
  color: #828282;
  text-align: center;
  margin: 0;
  line-height: 1.5;
}

.auth-terms strong {
  color: #000;
  font-weight: 400;
}
</style>
