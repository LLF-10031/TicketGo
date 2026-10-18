<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  bizType: { type: String, default: 'avatar' },
  shape: { type: String, default: 'circle' },
  size: { type: Number, default: 96 }
})
const emit = defineEmits(['update:modelValue', 'uploaded'])

const uploading = ref(false)
const inputRef = ref(null)

function trigger() {
  if (uploading.value) return
  inputRef.value && inputRef.value.click()
}

async function onChange(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const data = await uploadFile(file, props.bizType)
    const url =
      (data && (data.display_url || data.displayUrl || data.original_url || data.url)) || ''
    if (!url) throw new Error('upload response missing url')
    emit('update:modelValue', url)
    emit('uploaded', url, data)
    ElMessage.success('上传成功')
  } catch (err) {
    // 后端未就绪时本地预览兜底
    const localUrl = URL.createObjectURL(file)
    emit('update:modelValue', localUrl)
    emit('uploaded', localUrl, null)
    ElMessage.warning('上传接口不可用，已使用本地预览')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="image-upload" :style="{ width: size + 'px', height: size + 'px' }" @click="trigger">
    <img v-if="modelValue" :src="modelValue" alt="preview" />
    <div v-else class="iu-placeholder">
      <span>{{ uploading ? '上传中…' : '＋' }}</span>
      <small>上传图片</small>
    </div>
    <div class="iu-mask">{{ uploading ? '上传中…' : '更换' }}</div>
    <input ref="inputRef" type="file" accept="image/*" style="display: none" @change="onChange" />
  </div>
</template>

<style scoped>
.image-upload {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: var(--warm-1);
  border: 1px dashed #e3d2bc;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-upload img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.iu-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #b09a83;
  font-size: 20px;
}
.iu-placeholder small {
  font-size: 12px;
}
.iu-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  opacity: 0;
  transition: 0.18s;
}
.image-upload:hover .iu-mask {
  opacity: 1;
}
</style>
