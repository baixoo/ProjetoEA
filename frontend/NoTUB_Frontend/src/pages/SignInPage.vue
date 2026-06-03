<template>
  <q-page class="auth-page">
    <div class="auth-content">
      <div class="auth-copy">
        <h1 class="auth-title">Iniciar sessao</h1>
        <p class="auth-subtitle">Insira os dados de autenticacao</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <div class="field">
          <input
            v-model="email"
            type="email"
            placeholder="Email"
            class="field__input"
            autocomplete="email"
            required
          />
        </div>
        <div class="field">
          <input
            v-model="password"
            type="password"
            placeholder="Palavra-passe"
            class="field__input"
            autocomplete="current-password"
            required
          />
        </div>
        <div class="forgot-row">
          <a href="#" class="forgot-link" @click.prevent="$router.push('/forgot-password')">Esqueceu a palavra-passe?</a>
        </div>
        <p v-if="error" class="error-message">{{ error }}</p>
        <button type="submit" class="btn btn--primary" :disabled="loading">
          {{ loading ? 'A entrar...' : 'Continuar' }}
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
        Nao tem uma conta? <router-link to="/signup" class="auth-link">Registe-se</router-link>.
      </p>
      <p class="auth-terms">
        Ao clicar em continuar, voce concorda com os nossos
        <a href="#" class="terms-link" @click.prevent="showTerms">Termos de Servico</a> e com a
        <a href="#" class="terms-link" @click.prevent="showPrivacy">Politica de Privacidade</a>
      </p>
    </div>

    <TermsModal v-model="termsOpen" :section="termsSection" :title="termsTitle" />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'
import TermsModal from 'src/components/TermsModal.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const termsOpen = ref(false)
const termsSection = ref('terms')
const termsTitle = ref('Termos e Condicoes')

onMounted(() => {
  if (route.query.oauth_error) {
    error.value = 'Autenticacao com Google falhou. Tente novamente.'
  }
})

function showTerms() {
  termsSection.value = 'terms'
  termsTitle.value = 'Termos e Condicoes'
  termsOpen.value = true
}

function showPrivacy() {
  termsSection.value = 'privacy'
  termsTitle.value = 'Politica de Privacidade'
  termsOpen.value = true
}

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(email.value, password.value)
    router.push('/home')
  } catch (e) {
    error.value = e.message || 'Erro ao iniciar sessao'
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

.forgot-row {
  text-align: right;
  margin-top: -8px;
}

.forgot-link {
  font-size: 12px;
  color: var(--color-primary, #028e5c);
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
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

.btn--google {
  background: #eee;
  color: #000;
  width: 100%;
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
  width: 100%;
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
  color: var(--color-primary, #028e5c);
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

.terms-link {
  color: var(--color-primary, #028e5c);
  text-decoration: none;
  font-weight: 500;
}

.terms-link:hover {
  text-decoration: underline;
}
</style>
