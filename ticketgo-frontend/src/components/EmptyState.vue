<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  icon: { type: String, default: '🎫' },
  actionText: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<template>
  <div class="empty-state">
    <div class="es-icon">{{ icon }}</div>
    <div class="es-desc">{{ description }}</div>
    <button v-if="actionText" class="btn btn-ghost btn-sm mt-16" @click="$emit('action')">
      {{ actionText }}
    </button>
    <slot />
  </div>
</template>

<style scoped>
.empty-state {
  padding: 54px 20px;
  text-align: center;
  color: var(--text-3);
  background: #fff;
  border: 1px dashed #e8d9c5;
  border-radius: var(--radius);
}
.es-icon {
  font-size: 44px;
  margin-bottom: 12px;
  opacity: 0.75;
}
.es-desc {
  font-size: 14px;
}
</style>
