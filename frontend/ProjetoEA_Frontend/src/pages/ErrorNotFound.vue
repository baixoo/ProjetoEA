<template>
  <q-page class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-text">Página não encontrada</p>
      <router-link to="/home" class="error-link">Voltar ao início</router-link>
    </div>
  </q-page>
</template>

<script setup>
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #fff;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.error-code {
  font-size: 72px;
  font-weight: 700;
  color: #1876d2;
  margin: 0;
}

.error-text {
  font-size: 16px;
  color: #828282;
  margin: 0;
}

.error-link {
  font-size: 14px;
  font-weight: 500;
  color: #2196f3;
  text-decoration: none;
  margin-top: 8px;
}

.error-link:hover {
  text-decoration: underline;
}
</style>
