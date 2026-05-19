<template>
  <q-page class="oauth-page">
    <div class="oauth-content">
      <q-spinner size="48px" color="primary" />
      <p class="oauth-text">A autenticar...</p>
      <p v-if="error" class="oauth-error">{{ error }}</p>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const error = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')

  if (token) {
    try {
      authStore.setToken(token)
      await authStore.fetchUser()
      router.replace('/home')
    } catch {
      error.value = 'Erro ao autenticar. Tente novamente.'
      authStore.logout()
      setTimeout(() => router.replace('/signin'), 2000)
    }
  } else {
    error.value = 'Token não encontrado. Tente novamente.'
    setTimeout(() => router.replace('/signin'), 2000)
  }
})
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
</style>
