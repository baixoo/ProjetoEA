<template>
  <q-layout view="hHh lpR fFf">
    <BrandHeader />
    <q-page-container>
      <router-view />
    </q-page-container>
    <AppTabBar :active-tab="activeTab" />
  </q-layout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BrandHeader from 'components/BrandHeader.vue'
import AppTabBar from 'components/AppTabBar.vue'

const route = useRoute()
const activeTab = computed(() => route.meta.activeTab || 'home')
</script>
