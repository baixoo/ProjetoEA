<template>
  <div class="brand-header">
    <img src="/assets/logo.png" alt="EA Movi" class="brand-logo" />
  </div>
</template>

<script setup>
</script>

<style scoped>
.brand-header {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 375px;
  max-width: 100%;
  height: 42px;
  background: #fff;
  border-bottom: 0.5px solid #e6e6e6;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo {
  height: 31px;
  width: 120px;
  object-fit: contain;
}
</style>
