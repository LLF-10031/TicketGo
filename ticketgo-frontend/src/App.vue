<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

const route = useRoute()
const blank = computed(() => route.meta.blank === true)
</script>

<template>
  <template v-if="blank">
    <router-view />
  </template>
  <template v-else>
    <AppHeader />
    <router-view />
    <AppFooter />
  </template>
</template>
