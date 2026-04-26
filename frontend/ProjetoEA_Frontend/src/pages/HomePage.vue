<template>
  <q-page class="home-page">
    <div class="home-content">
      <h1 class="home-title">Bem-vindo!</h1>

      <button class="start-button" @click="startTrip">
        <span class="start-button__text">Iniciar</span>
      </button>

      <div class="home-info">
        <p class="home-info__bold">Clique no botão para sair do autocarro.</p>
        <p class="home-info__medium">Lotação do autocarro: 23 pessoas</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function startTrip() {
  router.push('/scan')
}
</script>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 93px 18px 92px;
  background: #fff;
}

.home-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: #fff;
  border-radius: 30px;
  padding: 10px;
  width: 100%;
  max-width: 339px;
  min-height: 500px;
}

.home-title {
  font-size: 48px;
  font-weight: 700;
  color: #000;
  margin: 0;
  line-height: normal;
}

.start-button {
  width: 261px;
  height: 261px;
  border-radius: 50%;
  border: none;
  background: #1876d2;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(24, 118, 210, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
}

.start-button:hover {
  transform: scale(1.03);
  box-shadow: 0 6px 16px rgba(24, 118, 210, 0.5);
}

.start-button:active {
  transform: scale(0.98);
}

.start-button__text {
  font-size: 64px;
  font-weight: 700;
  color: #fff;
}

.home-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  height: 54px;
}

.home-info__bold {
  font-size: 16px;
  font-weight: 700;
  color: #000;
  margin: 0;
  line-height: normal;
  white-space: nowrap;
}

.home-info__medium {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  margin: 0;
  line-height: normal;
  width: 261px;
  text-align: center;
}
</style>
