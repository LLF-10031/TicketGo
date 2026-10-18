<script setup>
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <div class="container page">
    <EmptyState description="页面不存在或已下线" icon="🧭" action-text="返回首页" @action="router.push('/')" />
  </div>
</template>
