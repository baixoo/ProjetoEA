<template>
  <q-page class="auth-page">
    <div class="auth-content">
      <div class="auth-copy">
        <h1 class="auth-title">Criar uma conta</h1>
        <p class="auth-subtitle">Preencha com os seus dados</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="field">
          <input v-model="form.email" type="email" placeholder="Email" class="field__input" autocomplete="email" required />
        </div>
        <div class="field">
          <input v-model="form.nome" type="text" placeholder="Nome" class="field__input" autocomplete="name" required @input="validarNome"/>
        </div>
        <div class="field">
          <input v-model="form.nif" type="text" placeholder="NIF" class="field__input" maxlength="9" autocomplete="off" @input="validarNif" />
        </div>
        <div class="field">
          <input v-model="form.dataNascimento" type="date" placeholder="Data de nascimento" class="field__input" autocomplete="bday" />
        </div>
        <div class="field">
          <input v-model="form.password" type="password" placeholder="Palavra-passe" class="field__input" autocomplete="new-password" required />
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
        Ao clicar em continuar, você concorda com os nossos
        <a href="#" class="terms-link" @click.prevent="showTerms">Termos de Servico</a> e com a
        <a href="#" class="terms-link" @click.prevent="showPrivacy">Política de Privacidade</a>
      </p>
    </div>

    <TermsModal v-model="termsOpen" :section="termsSection" :title="termsTitle" />
  </q-page>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'
import TermsModal from 'src/components/TermsModal.vue'

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
const termsOpen = ref(false)
const termsSection = ref('terms')
const termsTitle = ref('Termos e Condições')

function showTerms() {
  termsSection.value = 'terms'
  termsTitle.value = 'Termos e Condições'
  termsOpen.value = true
}

function showPrivacy() {
  termsSection.value = 'privacy'
  termsTitle.value = 'Política de Privacidade'
  termsOpen.value = true
}

function validarNif(event) {
  let valor = event.target.value;
  
  valor = valor.replace(/\D/g, '');
  
  form.nif = valor;
}

function validarNome(event) {
  let valor = event.target.value;
  valor = valor.replace(/[^a-zA-Z\s]/g, '');

  if (valor.startsWith(' ')) {
    valor = valor.trimStart();
  }

  form.nome = valor;
}

async function handleRegister() {
  error.value = ''
  success.value = ''

  const nameParts = form.nome.trim().split(/\s+/)
  const primeiroNome = nameParts[0] || ''
  const ultimoNome = nameParts.slice(1).join(' ') || ''

  if (form.nif && form.nif.length !== 9) {
    error.value = 'O NIF deve conter exatamente 9 dígitos.'
    return
  }

  loading.value = true

  try {
    await authStore.register({
      email: form.email,
      primeiroNome,
      ultimoNome,
      nif: form.nif.trim() || null,
      dataNascimento: form.dataNascimento || null,
      password: form.password
    })
    success.value = 'Conta criada com sucesso!'
    // Redirecionar para a página inicial imediatamente
    router.push({ name: 'home' })
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

.success-message {
  color: #2e7d32;
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
