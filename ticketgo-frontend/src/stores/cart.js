import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as cartApi from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)
  const selectedIds = ref([])

  const selectedItems = computed(() =>
    items.value.filter((it) => selectedIds.value.includes(it.id))
  )
  const totalAmount = computed(() =>
    selectedItems.value.reduce((sum, it) => {
      const price = Number(it.price || it.unit_price || it.amount || 0)
      const qty = Number(it.quantity || 1)
      return sum + price * qty
    }, 0)
  )

  async function load() {
    loading.value = true
    try {
      const data = await cartApi.getCart()
      items.value = Array.isArray(data) ? data : (data && data.items) || []
    } finally {
      loading.value = false
    }
    return items.value
  }

  async function remove(id) {
    await cartApi.removeCartItem(id)
    items.value = items.value.filter((it) => it.id !== id)
    selectedIds.value = selectedIds.value.filter((sid) => sid !== id)
  }

  function toggleSelect(id) {
    const i = selectedIds.value.indexOf(id)
    if (i >= 0) selectedIds.value.splice(i, 1)
    else selectedIds.value.push(id)
  }

  function selectAll() {
    selectedIds.value = items.value.map((it) => it.id)
  }

  function clearSelection() {
    selectedIds.value = []
  }

  return {
    items,
    loading,
    selectedIds,
    selectedItems,
    totalAmount,
    load,
    remove,
    toggleSelect,
    selectAll,
    clearSelection
  }
})
