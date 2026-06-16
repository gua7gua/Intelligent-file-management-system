<template>
  <div class="register-layout">
    <!-- 左侧能力说明 -->
    <section class="card panel">
      <p class="status info">社会公众账号</p>
      <h1 class="page-title" style="margin-top: 12px">创建公众账号</h1>
      <p class="page-subtitle">
        注册后可下载公开电子文件、提交征集清单、查看本人下载记录和征集状态。
      </p>
      <div class="flow-strip" style="grid-template-columns: 1fr">
        <div class="flow-step"><strong>公开检索</strong>检索已公开的正式档案。</div>
        <div class="flow-step"><strong>征集清单</strong>登记捐赠意向和相关文件信息。</div>
      </div>
    </section>

    <!-- 右侧注册表单 -->
    <section class="card panel">
      <h2 class="section-title">注册信息</h2>
      <form class="grid" style="gap: 13px" @submit.prevent="handleSubmit">
        <div class="form-grid" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
          <div class="field">
            <label for="realName">姓名</label>
            <input id="realName" v-model="form.realName" required />
          </div>
          <div class="field">
            <label for="phone">手机号</label>
            <input id="phone" v-model="form.phone" inputmode="tel" required />
          </div>
        </div>
        <div class="form-grid" style="grid-template-columns: 1fr auto">
          <div class="field">
            <label for="code">短信验证码</label>
            <input id="code" v-model="form.smsCode" inputmode="numeric" required />
          </div>
          <div class="field">
            <label>&nbsp;</label>
            <button
              type="button"
              class="button secondary"
              :disabled="countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
            </button>
          </div>
        </div>
        <div class="form-grid" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
          <div class="field">
            <label for="password">设置密码</label>
            <input id="password" v-model="form.password" type="password" required />
          </div>
          <div class="field">
            <label for="confirmPassword">确认密码</label>
            <input id="confirmPassword" v-model="form.confirmPassword" type="password" required />
          </div>
        </div>
        <label class="check-row">
          <input v-model="form.accepted" type="checkbox" />
          <span
            >我确认公众账号仅用于公开档案检索、下载和征集意向提交；下载公开电子文件需登录公众账号并遵守使用约定。</span
          >
        </label>
        <div class="actions">
          <button type="submit" class="button"><span class="icon">✓</span>创建公众账号</button>
          <router-link to="/login" class="button ghost">已有账号登录</router-link>
        </div>
      </form>
      <div v-if="resultMessage" style="margin-top: 14px" aria-live="polite">
        <div :class="['notice', resultType]">{{ resultMessage }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { validatePublicRegister } from '@/utils/publicValidation'
import { sendPublicSmsCode, registerPublicUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  realName: '',
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  accepted: false,
})

const countdown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null
const resultMessage = ref('')
const resultType = ref('')

const phonePattern = /^1[3-9]\d{9}$/

function startCountdown() {
  countdown.value = 60
  if (smsTimer) clearInterval(smsTimer)
  smsTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 0
      if (smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }
  }, 1000)
}

async function handleSendCode() {
  if (!phonePattern.test(form.phone.trim())) {
    resultMessage.value = '请输入正确的 11 位手机号。'
    resultType.value = 'danger'
    return
  }
  try {
    await sendPublicSmsCode({ phone: form.phone, scene: 'register' })
    startCountdown()
    resultMessage.value = '验证码已发送至手机号。'
    resultType.value = ''
  } catch {
    resultMessage.value = '验证码发送失败，请稍后重试。'
    resultType.value = 'danger'
  }
}

onUnmounted(() => {
  if (smsTimer) clearInterval(smsTimer)
})

async function handleSubmit() {
  resultMessage.value = ''
  const errors = validatePublicRegister(form)
  if (errors.length > 0) {
    resultMessage.value = errors.join(' ')
    resultType.value = 'danger'
    return
  }
  try {
    await registerPublicUser({
      phone: form.phone,
      smsCode: form.smsCode,
      password: form.password,
      realName: form.realName,
    })
    // 注册成功后自动登录，直接进入公众概览
    await authStore.login(form.phone, form.password, 'public')
    resultMessage.value = `注册成功，即将进入公众概览。`
    resultType.value = ''
    setTimeout(() => router.push('/public/overview'), 2000)
  } catch {
    resultMessage.value = '注册失败，请稍后重试。'
    resultType.value = 'danger'
  }
}
</script>

<style scoped>
.register-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(360px, 1.1fr);
  gap: 18px;
  align-items: start;
}

.check-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #f9fbfc;
}

.check-row input {
  width: 18px;
  height: 18px;
  margin-top: 3px;
}

@media (max-width: 900px) {
  .register-layout {
    grid-template-columns: 1fr;
  }
}
</style>
