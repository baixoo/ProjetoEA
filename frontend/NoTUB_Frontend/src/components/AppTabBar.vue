<template>
  <div class="tab-bar">
    <div class="tab-bar__inner">
      <router-link
        v-for="tab in tabs"
        :key="tab.name"
        :to="tab.route"
        class="tab-bar__item"
        :class="{ 'tab-bar__item--active': activeTab === tab.name }"
      >
        <img :src="tab.icon" alt="" class="tab-icon" />
      </router-link>
    </div>
    <div class="home-indicator">
      <div class="home-indicator__bar"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  activeTab: {
    type: String,
    required: true
  }
})

const tabs = [
  { name: 'home', icon: '/assets/icon-home.svg', route: '/home' },
  { name: 'trips', icon: '/assets/icon-bus.svg', route: '/trips' },
  { name: 'scan', icon: '/assets/icon-qr.svg', route: '/scan' },
  { name: 'tickets', icon: '/assets/icon-cart.svg', route: '/tickets' },
  { name: 'account', icon: '/assets/icon-person.svg', route: '/account' }
]
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 375px;
  max-width: 100%;
  height: 78px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 0.5px solid #e6e6e6;
  box-shadow: 0 -0.5px 0 0 rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.tab-bar__inner {
  display: flex;
  align-items: flex-start;
  justify-content: space-around;
  height: 44px;
  padding: 12px 26px 8px;
}

.tab-bar__item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 24px;
  opacity: 0.3;
  text-decoration: none;
  transition: opacity 0.2s;
}

.tab-bar__item--active {
  opacity: 1;
}

.tab-icon {
  width: 24px;
  height: 24px;
}

.home-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 375px;
  height: 34px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.home-indicator__bar {
  width: 134px;
  height: 5px;
  background: #000;
  border-radius: 100px;
  margin-bottom: 8px;
}
</style>
