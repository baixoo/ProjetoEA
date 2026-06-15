<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated class="admin-header">
      <q-toolbar>
        <q-toolbar-title class="admin-toolbar-title">
          <img src="/assets/logo.png" alt="NoTUB" class="admin-logo" />
          <span class="admin-title-text">Painel de Administracao</span>
        </q-toolbar-title>
        <q-btn flat dense icon="logout" label="Sair" @click="handleLogout" />
      </q-toolbar>
    </q-header>

    <q-drawer v-model="drawerOpen" show-if-above bordered class="admin-sidebar">
      <q-list>
        <q-item-label header class="sidebar-label">Menu</q-item-label>

        <q-item
          v-for="item in menuItems"
          :key="item.route"
          :to="item.route"
          clickable
          :active="$route.path === item.route"
          active-class="sidebar-item--active"
        >
          <q-item-section avatar>
            <q-icon :name="item.icon" />
          </q-item-section>
          <q-item-section>{{ item.label }}</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const drawerOpen = ref(true)

const menuItems = [
  { route: '/admin', icon: 'dashboard', label: 'Dashboard' },
  { route: '/admin/users', icon: 'people', label: 'Utilizadores' },
  { route: '/admin/vehicles', icon: 'directions_bus', label: 'Veiculos' },
  { route: '/admin/tariffs', icon: 'payments', label: 'Tarifas' },
  { route: '/admin/zones', icon: 'map', label: 'Zonas' },
  { route: '/admin/trips', icon: 'schedule', label: 'Viagens' },
  { route: '/admin/network', icon: 'alt_route', label: 'Rede' }
]

function handleLogout() {
  authStore.logout()
  router.push('/signin')
}
</script>

<style scoped>
.admin-header {
  background: #0e2d24;
}

.admin-toolbar-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-logo {
  height: 28px;
  object-fit: contain;
}

.admin-title-text {
  font-size: 15px;
  font-weight: 600;
}

.admin-sidebar {
  background: #fafafa;
}

.sidebar-label {
  font-size: 11px;
  font-weight: 700;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 16px 16px 8px;
}

.sidebar-item--active {
  background: #e6f7f0;
  color: #028e5c;
  font-weight: 600;
}
</style>
