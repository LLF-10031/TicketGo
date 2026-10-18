<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { serverNow } from '@/utils/time'
import { toDate } from '@/utils/format'

const props = defineProps({
  target: { type: [String, Number, Date], default: null },
  prefix: { type: String, default: '' },
  finishedText: { type: String, default: '00:00:00' },
  showDays: { type: Boolean, default: false }
})
const emit = defineEmits(['finish', 'tick'])

const remain = ref(0)
let timer = null
let finished = false

const targetMs = computed(() => {
  const d = toDate(props.target)
  return d ? d.getTime() : 0
})

function compute() {
  if (!targetMs.value) {
    remain.value = 0
    return
  }
  remain.value = Math.max(0, targetMs.value - serverNow())
}

function render() {
  const total = Math.floor(remain.value / 1000)
  const days = Math.floor(total / 86400)
  const h = Math.floor((total % 86400) / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const p = (n) => String(n).padStart(2, '0')
  if (props.showDays && days > 0) return `${days}天 ${p(h)}:${p(m)}:${p(s)}`
  return `${p(h)}:${p(m)}:${p(s)}`
}

const text = computed(() => (remain.value > 0 ? render() : props.finishedText))

function tick() {
  compute()
  emit('tick', remain.value)
  if (remain.value <= 0) {
    stop()
    if (!finished) {
      finished = true
      emit('finish')
    }
  }
}

function start() {
  stop()
  finished = false
  compute()
  tick()
  if (remain.value > 0) timer = setInterval(tick, 1000)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(start)
onBeforeUnmount(stop)

defineExpose({ start, stop, remain })
</script>

<template>
  <span class="countdown">
    <slot :text="text" :remain="remain">{{ prefix }}{{ text }}</slot>
  </span>
</template>
