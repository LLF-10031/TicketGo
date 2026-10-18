<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, sendSms } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('password') // password | code
const isRegister = ref(false)
const loading = ref(false)

const smsCountdown = ref(0)
let smsTimer = null

const captcha = reactive({ id: '', image: '' })

const form = reactive({
  phone: '',
  password: '',
  code: '',
  captchaInput: '',
  nickName: ''
})

const needCaptcha = computed(() => mode.value === 'code' || isRegister.value)
const submitText = computed(() => {
  if (loading.value) return '处理中…'
  if (isRegister.value) return '注 册'
  return '登 录'
})

function validPhone() {
  return /^1[3-9]\d{9}$/.test(form.phone)
}

async function loadCaptcha() {
  captcha.image = ''
  try {
    const data = await getCaptcha()
    if (data) {
      captcha.id = data.captcha_id || data.captchaId || ''
      const img = data.image_base64 || data.imageBase64 || data.image || ''
      captcha.image = img ? (img.startsWith('data:') ? img : `data:image/png;base64,${img}`) : ''
    }
  } catch (e) {
    captcha.id = ''
    captcha.image = ''
  }
}

async function onSendSms() {
  if (smsCountdown.value > 0) return
  if (!validPhone()) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (needCaptcha.value && !form.captchaInput) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  try {
    await sendSms({
      phone: form.phone,
      captcha_id: captcha.id,
      captcha_input: form.captchaInput
    })
    ElMessage.success('验证码已发送（Mock 环境请查看后端日志）')
    startSmsCountdown()
  } catch (e) {
    // 后端未就绪时允许演示：给出明确提示
    ElMessage.error('短信服务暂不可用，请确认后端已启动')
    loadCaptcha()
  }
}

function startSmsCountdown() {
  smsCountdown.value = 60
  if (smsTimer) clearInterval(smsTimer)
  smsTimer = setInterval(() => {
    smsCountdown.value -= 1
    if (smsCountdown.value <= 0) {
      clearInterval(smsTimer)
      smsTimer = null
    }
  }, 1000)
}

function switchMode(next) {
  if (mode.value === next) return
  mode.value = next
  isRegister.value = false
  form.password = ''
  form.code = ''
  if (next === 'code') loadCaptcha()
}

function toggleRegister() {
  isRegister.value = !isRegister.value
  form.code = ''
  form.captchaInput = ''
  if (isRegister.value) loadCaptcha()
}

async function submit() {
  if (!validPhone()) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      if (!form.password || form.password.length < 6) {
        ElMessage.warning('密码至少 6 位')
        return
      }
      await userStore.register({
        phone: form.phone,
        password: form.password,
        nick_name: form.nickName || undefined
      })
      ElMessage.success('注册成功，正在为你登录…')
      await userStore.loginByPassword(form.phone, form.password)
    } else if (mode.value === 'password') {
      if (!form.password) {
        ElMessage.warning('请输入密码')
        return
      }
      await userStore.loginByPassword(form.phone, form.password)
    } else {
      if (!form.code) {
        ElMessage.warning('请输入短信验证码')
        return
      }
      await userStore.loginByCode(form.phone, form.code)
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : '/')
  } catch (e) {
    const msg = (e && e.message) || '登录失败，请稍后重试'
    ElMessage.error(msg)
    if (needCaptcha.value) loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <div class="ink">
    <div class="mountain"></div>
    <div class="poem">此曲只应天上有，人间能得几回闻</div>
    <div class="seal">票韵</div>
    <div class="petal" style="left: 12%; animation-delay: 0s"></div>
    <div class="petal" style="left: 38%; animation-delay: 3s"></div>
    <div class="petal" style="left: 64%; animation-delay: 6s"></div>
    <div class="petal" style="left: 86%; animation-delay: 9s"></div>

    <div class="ink-card">
      <div class="seal-logo">票韵</div>
      <div class="brand">票韵</div>
      <div class="brand-en">T I C K E T G O</div>
      <div class="brush"></div>

      <div class="ink-tabs">
        <span :class="{ active: mode === 'password' && !isRegister }" @click="switchMode('password')">密码登录</span>
        <span :class="{ active: mode === 'code' && !isRegister }" @click="switchMode('code')">验证码登录</span>
      </div>

      <div class="ink-field">
        <span class="fi">📱</span>
        <input v-model.trim="form.phone" class="ink-input" placeholder="请输入手机号" maxlength="11" />
      </div>

      <div v-if="isRegister" class="ink-field">
        <span class="fi">👤</span>
        <input v-model.trim="form.nickName" class="ink-input" placeholder="昵称（选填）" maxlength="20" />
      </div>

      <div v-if="mode === 'password' || isRegister" class="ink-field">
        <span class="fi">🔒</span>
        <input
          v-model="form.password"
          class="ink-input"
          type="password"
          placeholder="请输入密码"
          @keyup.enter="submit"
        />
      </div>

      <div v-if="mode === 'code' && !isRegister" class="ink-field" style="position: relative">
        <span class="fi">🔑</span>
        <input v-model.trim="form.code" class="ink-input" placeholder="短信验证码" maxlength="6" style="padding-right: 120px" />
        <button class="sms-btn" :disabled="smsCountdown > 0" @click="onSendSms">
          {{ smsCountdown > 0 ? smsCountdown + 's 后重发' : '获取验证码' }}
        </button>
      </div>

      <div v-if="needCaptcha" class="captcha-row">
        <div class="ink-field" style="flex: 1">
          <span class="fi">🔐</span>
          <input v-model.trim="form.captchaInput" class="ink-input" placeholder="图形验证码" maxlength="6" />
        </div>
        <div class="captcha-box" title="点击刷新" @click="loadCaptcha">
          <img v-if="captcha.image" :src="captcha.image" alt="captcha" />
          <span v-else>点击刷新</span>
        </div>
      </div>

      <button class="ink-sub" :disabled="loading" @click="submit">{{ submitText }}</button>

      <div class="ink-aux">
        <label><input type="checkbox" style="vertical-align: middle" /> 记住我</label>
        <a>忘记密码？</a>
      </div>

      <div class="ink-divider">其他登录方式</div>
      <div class="ink-social">
        <span title="微信">💬</span>
        <span title="微博">🌐</span>
        <span title="QQ">🐧</span>
      </div>

      <div class="ink-aux" style="justify-content: center; margin-top: 20px; display: block">
        {{ isRegister ? '已有账号？' : '还没有账号？' }}
        <a style="color: var(--primary)" @click="toggleRegister">
          {{ isRegister ? '返回登录' : '立即注册' }}
        </a>
      </div>
    </div>
  </div>
</template>
