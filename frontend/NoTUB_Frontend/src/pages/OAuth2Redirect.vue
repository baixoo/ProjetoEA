<template>
  <div class="oauth-page">
    <div class="oauth-content">
      <q-spinner size="48px" color="primary" />
      <p class="oauth-text">A autenticar...</p>
      <p v-if="error" class="oauth-error">{{ error }}</p>
    </div>

    <q-dialog v-model="showProfileDialog" persistent :no-backdrop-dismiss="true" :no-route-dismiss="true">
      <q-card class="profile-dialog-card">
        <q-card-section>
          <div class="text-h6 dialog-title">Complete o seu perfil</div>
          <p class="dialog-subtitle">Para continuar, preencha os campos obrigatorios.</p>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <div class="dialog-field">
            <q-input
              v-model="profileForm.nif"
              label="NIF *"
              outlined
              maxlength="9"
              :error="!!profileErrors.nif"
              :error-message="profileErrors.nif"
            />
          </div>
          <div class="dialog-field">
            <q-input
              v-model="profileForm.dataNascimento"
              label="Data de Nascimento *"
              type="date"
              outlined
              :error="!!profileErrors.dataNascimento"
              :error-message="profileErrors.dataNascimento"
            />
          </div>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn
            label="Guardar"
            color="primary"
            :loading="saving"
            @click="handleSaveProfile"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const error = ref('')
const showProfileDialog = ref(false)
const saving = ref(false)

const profileForm = reactive({
  nif: '',
  dataNascimento: ''
})

const profileErrors = reactive({
  nif: '',
  dataNascimento: ''
})

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')

  if (token) {
    try {
      authStore.setToken(token)
      await authStore.fetchUser()
      const user = authStore.user
      if (user && (!user.nif || !user.dataNascimento)) {
        profileForm.nif = user.nif || ''
        profileForm.dataNascimento = user.dataNascimento || ''
        showProfileDialog.value = true
      } else {
        router.replace('/home')
      }
    } catch {
      error.value = 'Erro ao autenticar. Tente novamente.'
      authStore.logout()
      setTimeout(() => router.replace('/signin'), 2000)
    }
  } else {
    error.value = 'Token nao encontrado. Tente novamente.'
    setTimeout(() => router.replace('/signin'), 2000)
  }
})

async function handleSaveProfile() {
  profileErrors.nif = ''
  profileErrors.dataNascimento = ''

  if (!profileForm.nif || profileForm.nif.trim().length < 9) {
    profileErrors.nif = 'NIF deve ter 9 digitos'
    return
  }
  if (!profileForm.dataNascimento) {
    profileErrors.dataNascimento = 'Data de nascimento obrigatoria'
    return
  }

  saving.value = true
  try {
    const user = authStore.user
    await authStore.updateProfile({
      email: user.email,
      primeiroNome: user.primeiroNome,
      ultimoNome: user.ultimoNome,
      nif: profileForm.nif.trim(),
      dataNascimento: profileForm.dataNascimento
    })
    showProfileDialog.value = false
    router.replace('/home')
  } catch {
    profileErrors.nif = 'Erro ao guardar. Tente novamente.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.oauth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #fff;
}

.oauth-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.oauth-text {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  margin: 0;
}

.oauth-error {
  font-size: 14px;
  color: #d32f2f;
  margin: 0;
}

.profile-dialog-card {
  min-width: 320px;
  max-width: 400px;
  border-radius: 16px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
}

.dialog-subtitle {
  font-size: 13px;
  color: #757575;
  margin: 4px 0 0 0;
}

.dialog-field {
  margin-bottom: 12px;
}
</style>
