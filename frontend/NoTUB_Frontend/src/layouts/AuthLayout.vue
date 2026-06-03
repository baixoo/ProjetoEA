<template>
  <q-layout view="hHh lpR fFf">
    <BrandHeader />
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import BrandHeader from 'components/BrandHeader.vue'
</script>
