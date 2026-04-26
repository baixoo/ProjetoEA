<template>
  <q-page class="traveling-page">
    <div class="traveling-content">
      <h1 class="traveling-title">A viajar...</h1>

      <div class="traveling-buttons">
        <button class="btn-validate" @click="$router.push('/scan')">
          <span>Validar Viagem</span>
          <img src="/assets/icon-qr2.svg" alt="" class="btn-validate__icon" />
        </button>

        <div class="bus-illustration">
          <div class="bus-body">
            <img src="/assets/union1.svg" alt="" class="bus-union bus-union--1" />
            <img src="/assets/union2.svg" alt="" class="bus-union bus-union--2" />
            <img src="/assets/union3.svg" alt="" class="bus-union bus-union--3" />
          </div>
          <img src="/assets/mask-group.svg" alt="" class="bus-mask" />
        </div>
      </div>

      <button class="btn-exit" @click="$router.push('/home')">
        <span>Sair</span>
        <img src="/assets/icon-exit.svg" alt="" class="btn-exit__icon" />
      </button>

      <div class="traveling-text">
        <p class="traveling-text__main">Clique no botão para sair do autocarro.</p>
        <p class="traveling-text__occupancy">Lotação do autocarro: 23 pessoas</p>
      </div>
    </div>
  </q-page>
</template>

<script setup>
</script>

<style scoped>
.traveling-page {
  position: relative;
  min-height: 100vh;
  background: #fff;
}

.traveling-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  min-height: calc(100vh - 42px - 78px);
  padding: 10px;
  padding-top: 86px;
  padding-bottom: 92px;
  background: #d2dff2;
}

.traveling-title {
  font-size: 48px;
  font-weight: 700;
  color: #000;
  margin: 0;
  line-height: normal;
  white-space: nowrap;
}

.traveling-buttons {
  display: flex;
  flex-direction: column;
  gap: 48px;
  align-items: center;
  padding: 0 55px;
}

.btn-validate {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 217px;
  height: 60px;
  background: #fff;
  border: none;
  border-radius: 5px;
  box-shadow: 0 4px 4px 0 rgba(0, 0, 0, 0.2);
  cursor: pointer;
  font-size: 20px;
  font-weight: 700;
  color: #000;
  transition: transform 0.2s;
}

.btn-validate:hover {
  transform: scale(1.03);
}

.btn-validate__icon {
  width: 38px;
  height: 38px;
}

.bus-illustration {
  position: relative;
  width: 376px;
  max-width: 100%;
  height: 190px;
}

.bus-body {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, #66c4ff, #fff4cd);
  border-radius: 20px 20px 0 0;
  box-shadow: inset 0 6px 8px 3px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.bus-union {
  position: absolute;
}

.bus-union--1 {
  top: 20%;
  left: 11.5%;
  width: 77%;
  height: 23%;
}

.bus-union--2 {
  top: 13%;
  left: 43%;
  width: 14%;
  height: 13%;
}

.bus-union--3 {
  top: 33%;
  left: 37.5%;
  width: 25%;
  height: 19%;
}

.bus-mask {
  position: absolute;
  top: -10px;
  left: 0;
  width: 376px;
  height: 190px;
  pointer-events: none;
}

.btn-exit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 237px;
  height: 66px;
  background: #2a85b2;
  border: none;
  border-radius: 5px;
  box-shadow: 0 4px 4px 0 rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-exit:hover {
  opacity: 0.9;
}

.btn-exit span {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
}

.btn-exit__icon {
  width: 60px;
  height: 60px;
}

.traveling-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 5px;
}

.traveling-text__main {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  margin: 0;
  text-align: center;
}

.traveling-text__occupancy {
  font-size: 20px;
  font-weight: 700;
  color: #000;
  margin: 0;
  white-space: nowrap;
}
</style>
