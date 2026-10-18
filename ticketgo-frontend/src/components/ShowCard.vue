<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { toDate } from '@/utils/format'

const props = defineProps({
  product: { type: Object, required: true }
})
const router = useRouter()

const saleInFuture = computed(() => {
  const d = toDate(props.product.saleStart)
  return d ? d.getTime() > Date.now() : false
})

function open() {
  if (props.product.id === undefined || props.product.id === null) return
  router.push(`/product/${props.product.id}`)
}
</script>

<template>
  <div class="card" @click="open">
    <div class="cover" :class="product.coverClass">
      <img v-if="product.cover" :src="product.cover" :alt="product.title" @error="($event.target.style.display = 'none')" />
      <span v-if="product.soldOut" class="badge soldout">售罄</span>
      <span v-else-if="product.hot" class="badge hot">热卖</span>
      <span v-else-if="saleInFuture" class="badge countdown">即将开票</span>
      <span v-else-if="product.category" class="badge">{{ product.category }}</span>
    </div>
    <div class="card-body">
      <div class="title">{{ product.title }}</div>
      <div class="venue">
        <template v-if="product.city">{{ product.city }} · </template>{{ product.venue || '场馆待定' }}
      </div>
      <div class="price">¥{{ product.price }} <small>起</small></div>
    </div>
  </div>
</template>
