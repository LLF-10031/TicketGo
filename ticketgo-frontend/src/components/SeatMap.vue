<script setup>
import { computed } from 'vue'
import { seatStatusOf } from '@/utils/normalize'

const props = defineProps({
  seats: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] },
  maxSelect: { type: Number, default: 6 }
})
const emit = defineEmits(['update:modelValue', 'conflict'])

const CELL = 26
const GAP = 6
const PAD = 24
const STAGE_H = 46

const TIER_COLORS = ['#ffb199', '#99c2ff', '#a8e6a8', '#ffcc80', '#d5b3ff', '#8fd9d9']
const tierColorMap = computed(() => {
  const map = {}
  let i = 0
  props.seats.forEach((s) => {
    const key = s.tierId ?? s.area ?? 'default'
    if (!(key in map)) {
      map[key] = TIER_COLORS[i % TIER_COLORS.length]
      i += 1
    }
  })
  return map
})

const rows = computed(() => {
  const set = []
  props.seats.forEach((s) => {
    const r = String(s.row)
    if (!set.includes(r)) set.push(r)
  })
  return set.sort((a, b) => {
    const na = parseFloat(a)
    const nb = parseFloat(b)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
    return a.localeCompare(b)
  })
})

const cols = computed(() => {
  const set = []
  props.seats.forEach((s) => {
    const c = String(s.col)
    if (!set.includes(c)) set.push(c)
  })
  return set.sort((a, b) => {
    const na = parseFloat(a)
    const nb = parseFloat(b)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
    return a.localeCompare(b)
  })
})

const geometry = computed(() => {
  const rowIndex = {}
  rows.value.forEach((r, i) => (rowIndex[r] = i))
  const colIndex = {}
  cols.value.forEach((c, i) => (colIndex[c] = i))
  const width = PAD * 2 + Math.max(cols.value.length, 1) * (CELL + GAP)
  const height = PAD * 2 + STAGE_H + Math.max(rows.value.length, 1) * (CELL + GAP)
  const mapped = props.seats.map((s) => {
    const r = rowIndex[String(s.row)] || 0
    const c = colIndex[String(s.col)] || 0
    return {
      ...s,
      px: PAD + c * (CELL + GAP),
      py: PAD + STAGE_H + r * (CELL + GAP)
    }
  })
  return { width, height, mapped }
})

const selectedSet = computed(() => new Set(props.modelValue))

function onSeatClick(seat) {
  const st = seatStatusOf(seat)
  if (st !== 'available') {
    emit('conflict', seat, st)
    return
  }
  const next = [...props.modelValue]
  const idx = next.indexOf(seat.id)
  if (idx >= 0) {
    next.splice(idx, 1)
  } else {
    if (next.length >= props.maxSelect) {
      emit('conflict', seat, 'max')
      return
    }
    next.push(seat.id)
  }
  emit('update:modelValue', next)
}

function fillOf(seat) {
  const st = seatStatusOf(seat)
  if (st === 'sold') return '#d9d9d9'
  if (st === 'locked') return '#c9c4bd'
  if (selectedSet.value.has(seat.id)) return '#ff4b2b'
  return tierColorMap.value[seat.tierId ?? seat.area ?? 'default'] || '#ffb199'
}

function strokeOf(seat) {
  const st = seatStatusOf(seat)
  if (st === 'sold') return '#c4c4c4'
  if (st === 'locked') return '#b3aca3'
  if (selectedSet.value.has(seat.id)) return '#e83f22'
  return 'rgba(0,0,0,.08)'
}
</script>

<template>
  <div class="seat-map">
    <div v-if="!seats.length" class="muted center" style="padding: 40px 0">暂无座位数据</div>
    <template v-else>
      <div class="seat-scroll">
        <svg :viewBox="`0 0 ${geometry.width} ${geometry.height}`" :width="geometry.width" :height="geometry.height">
          <rect
            :x="PAD"
            y="10"
            :width="Math.max(geometry.width - PAD * 2, 40)"
            :height="STAGE_H - 18"
            rx="8"
            fill="url(#stageGrad)"
          />
          <text
            :x="geometry.width / 2"
            :y="10 + (STAGE_H - 18) / 2 + 6"
            text-anchor="middle"
            fill="#fff"
            font-size="14"
            font-weight="700"
          >
            舞台 / STAGE
          </text>
          <defs>
            <linearGradient id="stageGrad" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#ff4b2b" />
              <stop offset="100%" stop-color="#ff8a4b" />
            </linearGradient>
          </defs>
          <g v-for="seat in geometry.mapped" :key="seat.id">
            <rect
              :x="seat.px"
              :y="seat.py"
              :width="CELL"
              :height="CELL"
              rx="6"
              :fill="fillOf(seat)"
              :stroke="strokeOf(seat)"
              stroke-width="1"
              class="seat-rect"
              @click="onSeatClick(seat)"
            />
            <title>{{ seat.area }} {{ seat.row }}排{{ seat.col }}座 · ¥{{ seat.price }}</title>
          </g>
        </svg>
      </div>
      <div class="seat-legend mt-16">
        <span class="lg"><i class="dot" style="background: #ffb199"></i>可售</span>
        <span class="lg"><i class="dot" style="background: #ff4b2b; border-color: #e83f22"></i>已选</span>
        <span class="lg"><i class="dot" style="background: #d9d9d9"></i>已售</span>
        <span class="lg"><i class="dot" style="background: #c9c4bd"></i>锁定中</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.seat-map {
  background: linear-gradient(180deg, #fffdfa, #fff);
  border: 1px solid #e8d9c5;
  border-radius: var(--radius);
  padding: 16px;
}
.seat-scroll {
  overflow: auto;
  max-width: 100%;
}
.seat-rect {
  cursor: pointer;
  transition: 0.12s;
}
.seat-rect:hover {
  opacity: 0.82;
}
</style>
