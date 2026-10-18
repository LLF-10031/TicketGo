<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updateAvatar, updatePassword, updateProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const userStore = useUserStore()

const savingProfile = ref(false)
const savingPwd = ref(false)
const pwdDialog = ref(false)

const profileForm = reactive({
  nickName: '',
  avatarUrl: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const phone = computed(() => {
  const p = userStore.profile || {}
  return p.phone || p.mobile || ''
})

const maskedPhone = computed(() => {
  if (!phone.value) return '未绑定'
  return String(phone.value).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
})

const userInitial = computed(() => userStore.initial)

function syncForm() {
  const p = userStore.profile || {}
  profileForm.nickName = p.nick_name || p.nickName || p.nickname || ''
  profileForm.avatarUrl = p.avatar_url || p.avatarUrl || p.avatar || ''
}

async function onAvatarUploaded(url) {
  profileForm.avatarUrl = url
  try {
    await updateAvatar(url)
    userStore.updateLocalProfile({ avatar_url: url })
    ElMessage.success('头像已更新')
  } catch (e) {
    userStore.updateLocalProfile({ avatar_url: url })
    ElMessage.warning('头像接口暂不可用，已本地保存')
  }
}

async function saveProfile() {
  if (!profileForm.nickName.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    await updateProfile({ nick_name: profileForm.nickName })
    userStore.updateLocalProfile({ nick_name: profileForm.nickName })
    ElMessage.success('资料已保存')
  } catch (e) {
    userStore.updateLocalProfile({ nick_name: profileForm.nickName })
    ElMessage.warning('资料接口暂不可用，已本地保存')
  } finally {
    savingProfile.value = false
  }
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialog.value = true
}

async function savePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await updatePassword({
      old_password: pwdForm.oldPassword,
      new_password: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdDialog.value = false
  } catch (e) {
    ElMessage.error((e && e.message) || '密码修改失败')
  } finally {
    savingPwd.value = false
  }
}

onMounted(async () => {
  syncForm()
  if (userStore.isLogin) {
    try {
      await userStore.fetchProfile()
      syncForm()
    } catch (e) {
      /* 后端未就绪时使用本地缓存 */
    }
  }
})
</script>

<template>
  <div class="container page">
    <div class="order-layout">
      <aside class="side-menu">
        <a class="active">个人中心</a>
        <a @click="router.push('/orders')">我的订单</a>
        <a @click="router.push('/my-coupons')">我的券包</a>
        <a @click="router.push('/cart')">购物车</a>
        <a @click="router.push('/coupons')">券中心</a>
      </aside>

      <main>
        <div class="section-title" style="margin-top: 0"><h2>个人中心</h2></div>

        <div class="panel mb-16">
          <div class="profile-head">
            <ImageUpload v-model="profileForm.avatarUrl" biz-type="avatar" :size="96" @uploaded="onAvatarUploaded" />
            <div>
              <div class="profile-name">{{ userStore.displayName }}</div>
              <div class="muted">手机号：{{ maskedPhone }}</div>
              <div class="muted mt-8">账号：{{ userInitial }} · TicketGo 会员</div>
            </div>
          </div>
        </div>

        <div class="panel mb-16">
          <h3 style="font-size: 16px; margin-bottom: 16px">基本资料</h3>
          <div class="form-row">
            <label>昵称</label>
            <el-input v-model="profileForm.nickName" placeholder="请输入昵称" maxlength="20" style="max-width: 320px" />
          </div>
          <div class="form-row">
            <label>手机号</label>
            <el-input :model-value="maskedPhone" disabled style="max-width: 320px" />
          </div>
          <div class="form-row">
            <label></label>
            <button class="btn btn-primary btn-sm" :disabled="savingProfile" @click="saveProfile">
              {{ savingProfile ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </div>

        <div class="panel mb-16">
          <h3 style="font-size: 16px; margin-bottom: 16px">账号安全</h3>
          <div class="between">
            <div>
              <div style="font-weight: 600">登录密码</div>
              <div class="muted">建议定期更换密码，保障账号安全</div>
            </div>
            <button class="btn btn-ghost btn-sm" @click="openPwdDialog">修改密码</button>
          </div>
        </div>

        <div class="panel">
          <h3 style="font-size: 16px; margin-bottom: 16px">快捷入口</h3>
          <div class="quick-links">
            <div class="quick-link" @click="router.push('/orders')">📦 我的订单</div>
            <div class="quick-link" @click="router.push('/my-coupons')">🎟️ 我的券包</div>
            <div class="quick-link" @click="router.push('/cart')">🛒 购物车</div>
            <div class="quick-link" @click="router.push('/coupons')">🎁 券中心</div>
          </div>
        </div>
      </main>
    </div>

    <el-dialog v-model="pwdDialog" title="修改密码" width="420px">
      <div class="form-row">
        <label>原密码</label>
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </div>
      <div class="form-row">
        <label>新密码</label>
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
      </div>
      <div class="form-row">
        <label>确认密码</label>
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </div>
      <template #footer>
        <button class="btn btn-ghost" style="margin-right: 10px" @click="pwdDialog = false">取消</button>
        <button class="btn btn-primary" :disabled="savingPwd" @click="savePassword">
          {{ savingPwd ? '提交中…' : '确认修改' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 24px;
}
.profile-name {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.form-row label {
  width: 80px;
  text-align: right;
  color: var(--text-2);
  font-size: 14px;
  flex-shrink: 0;
}
.quick-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.quick-link {
  padding: 18px;
  text-align: center;
  border: 1px solid #e8d9c5;
  border-radius: 10px;
  cursor: pointer;
  transition: 0.15s;
  font-size: 14px;
}
.quick-link:hover {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
}
@media (max-width: 700px) {
  .quick-links {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
