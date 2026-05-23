<template>
  <q-page class="auth-page">
    <div class="auth-content">
      <div class="auth-copy">
        <h1 class="auth-title">Recuperar palavra-passe</h1>
        <p class="auth-subtitle">Insira o seu email para receber um link de recuperacao</p>
      </div>

      <form v-if="!sent" class="auth-form" @submit.prevent="handleSubmit">
        <div class="field">
          <input
            v-model="email"
            type="email"
            placeholder="Email"
            class="field__input"
            required
          />
        </div>
        <p v-if="error" class="error-message">{{ error }}</p>
        <button type="submit" class="btn btn--primary" :disabled="loading">
          {{ loading ? 'A enviar...' : 'Enviar link de recuperacao' }}
        </button>
      </form>

      <div v-else class="success-card">
        <q-icon name="mark_email_read" size="48px" color="positive" />
        <p class="success-title">Email enviado</p>
        <p class="success-desc">
          Se existir uma conta associada a <strong>{{ email }}</strong>, recebera um email com instrucoes para redefinir a sua palavra-passe.
        </p>
      </div>

      <p class="auth-link-text">
        <router-link to="/signin" class="auth-link">Voltar ao inicio de sessao</router-link>
      </p>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from 'src/stores/auth'

const authStore = useAuthStore()

const email = ref('')
const error = ref('')
const loading = ref(false)
const sent = ref(false)

async function handleSubmit() {
  error.value = ''
  loading.value = true
  try {
    await authStore.forgotPassword(email.value)
    sent.value = true
  } catch (e) {
    error.value = e.message || 'Erro ao enviar email de recuperacao'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: calc(var(--header-h, 42px) + 32px) var(--page-pad, 20px) 32px;
  background: #fff;
}

.auth-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  width: 100%;
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
  width: 100%;
}

.field__input {
  width: 100%;
  height: 44px;
  background: #fff;
  border: 1.5px solid var(--color-border, #d7d6d6);
  border-radius: var(--radius-input, 8px);
  padding: 0 16px;
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
  border-color: var(--color-primary, #028e5c);
}

.error-message {
  color: #d32f2f;
  font-size: 13px;
  margin: -8px 0;
}

.btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-input, 8px);
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

.success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px 24px;
  background: #f8f9fa;
  border-radius: 12px;
  width: 327px;
  max-width: 100%;
}

.success-title {
  font-size: 18px;
  font-weight: 700;
  color: #000;
  margin: 0;
}

.success-desc {
  font-size: 13px;
  color: #495057;
  text-align: center;
  margin: 0;
  line-height: 1.5;
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
</style>
