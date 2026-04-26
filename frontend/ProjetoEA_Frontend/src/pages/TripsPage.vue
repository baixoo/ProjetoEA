<template>
  <q-page class="placeholder-page">
    <div class="placeholder-content">
      <q-icon name="directions_bus" size="64px" color="grey-5" />
      <h1 class="placeholder-title">Viagens</h1>
      <p class="placeholder-text">Em breve poderá consultar as suas viagens aqui.</p>
    </div>
  </q-page>
</template>

<script setup>
</script>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding-top: 42px;
  padding-bottom: 78px;
  background: #fff;
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.placeholder-title {
  font-size: 24px;
  font-weight: 600;
  color: #000;
  margin: 0;
}

.placeholder-text {
  font-size: 14px;
  color: #828282;
  margin: 0;
  text-align: center;
}
</style>
