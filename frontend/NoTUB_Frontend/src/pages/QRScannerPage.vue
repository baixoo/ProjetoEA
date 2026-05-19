<template>
  <q-page class="scanner-page">
    <div class="scanner-viewport">
      <img src="/assets/scanner-bg.png" alt="" class="scanner-bg-img" />
      <img src="/assets/scanner-subtract.svg" alt="" class="scanner-subtract" />
      <div class="scanner-frame">
        <img src="/assets/corner-tl.svg" alt="" class="corner corner--tl" />
        <img src="/assets/corner-tr.svg" alt="" class="corner corner--tr" />
        <img src="/assets/corner-bl.svg" alt="" class="corner corner--bl" />
        <img src="/assets/corner-br.svg" alt="" class="corner corner--br" />
      </div>
    </div>
    <p class="scanner-instructions">
      Aponte com a c&acirc;mara do telem&oacute;vel para o c&oacute;digo QR do autocarro e aguarde.
    </p>
  </q-page>
</template>

<script setup>
</script>

<style scoped>
.scanner-page {
  position: relative;
  min-height: 100vh;
  padding-top: 86px;
  padding-bottom: 78px;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
}

.scanner-viewport {
  position: relative;
  width: 375px;
  max-width: 100%;
  height: calc(100vh - 86px - 78px - 80px);
  overflow: hidden;
}

.scanner-bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}

.scanner-subtract {
  position: absolute;
  top: 0;
  left: 0;
  width: 375px;
  height: 648px;
  pointer-events: none;
}

.scanner-frame {
  position: absolute;
  top: 108px;
  left: 50%;
  transform: translateX(-50%);
  width: 261px;
  height: 261px;
}

.corner {
  position: absolute;
  width: 60px;
  height: 60px;
}

.corner--tl {
  top: 0;
  left: 0;
}

.corner--tr {
  top: 0;
  right: 0;
}

.corner--bl {
  bottom: 0;
  left: 0;
}

.corner--br {
  bottom: 0;
  right: 0;
}

.scanner-instructions {
  margin: 24px 0;
  padding: 0 42px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  text-align: center;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}
</style>
