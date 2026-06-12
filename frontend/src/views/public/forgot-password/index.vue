<template>
  <div class="reset-layout">
    <!-- 左侧说明区 -->
    <section class="card panel">
      <p class="status warning">公众账号找回</p>
      <h1 class="page-title" style="margin-top: 12px">重置公众账号密码</h1>
      <p class="page-subtitle">本页仅处理公众账号的密码找回。内部账号请联系系统管理员重置。</p>
      <ol class="step-list">
        <li><strong>1. 校验手机号</strong><br /><span class="muted">确认手机号属于有效公众账号。</span></li>
        <li><strong>2. 验证短信码</strong><br /><span class="muted">验证码校验通过后允许设置新密码。</span></li>
        <li><strong>3. 重新登录</strong><br /><span class="muted">重置成功后回到统一登录页。</span></li>
      </ol>
    </section>

    <!-- 右侧表单区 -->
    <section class="card panel">
      <h2 class="section-title">找回密码</h2>
      <form class="grid" style="gap: 13px" @submit.prevent="handleSubmit">
        <div class="form-grid" style="grid-template-columns: 1fr auto">
          <div class="field">
            <label for="phone">公众账号手机号</label>
            <input id="phone" v-model="form.phone" inputmode="tel" required />
          </div>
          <div class="field">
            <label>&nbsp;</label>
            <button
              type="button"
              class="button secondary"
              :disabled="codeSent"
              @click="handleSendCode"
            >
              {{ codeSent ? '验证码已发送' : '发送验证码' }}
            </button>
          </div>
        </div>
        <div class="field">
          <label for="code">短信验证码</label>
          <input id="code" v-model="form.smsCode" inputmode="numeric" required />
        </div>
        <div class="form-grid" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
          <div class="field">
            <label for="password">新密码</label>
            <input id="password" v-model="form.newPassword" type="password" required />
          </div>
          <div class="field">
            <label for="confirmPassword">确认新密码</label>
            <input id="confirmPassword" v-model="form.confirmPassword" type="password" required />
          </div>
        </div>
        <div class="actions">
          <button type="submit" class="button"><span class="icon">✓</span>确认重置</button>
          <router-link to="/login" class="button ghost">返回登录</router-link>
        </div>
      </form>
      <div v-if="resultMessage" style="margin-top: 14px" aria-live="polite">
        <div :class="['notice', resultType]">{{ resultMessage }}</div>
      </div>
      <p class="hint">系统管理员、档案管理员、移交单位经办人、内部查阅者和馆领导账号不通过公众找回流程处理。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { validateResetPassword } from '@/utils/publicValidation'
import { sendPublicSmsCode, resetPublicPassword } from '@/api/auth'

const router = useRouter()

const form = reactive({
  phone: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: '',
})

const codeSent = ref(false)
const resultMessage = ref('')
const resultType = ref('')

async function handleSendCode() {
  if (!form.phone.trim()) {
    resultMessage.value = '请先输入手机号。'
    resultType.value = 'danger'
    return
  }
  try {
    await sendPublicSmsCode({ phone: form.phone, scene: 'forgot_password' })
    codeSent.value = true
    resultMessage.value = '验证码已发送至手机号。'
    resultType.value = ''
  } catch {
    resultMessage.value = '验证码发送失败，请稍后重试。'
    resultType.value = 'danger'
  }
}

async function handleSubmit() {
  resultMessage.value = ''
  const errors = validateResetPassword(form)
  if (errors.length > 0) {
    resultMessage.value = errors.join(' ')
    resultType.value = 'danger'
    return
  }
  try {
    await resetPublicPassword({
      phone: form.phone,
      smsCode: form.smsCode,
      newPassword: form.newPassword,
    })
    resultMessage.value = '密码已重置。请返回统一登录页使用公众角色登录。'
    resultType.value = ''
    setTimeout(() => router.push('/login'), 2000)
  } catch {
    resultMessage.value = '重置失败，请检查验证码是否正确。'
    resultType.value = 'danger'
  }
}
</script>

<style scoped>
.reset-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(360px, 1.2fr);
  gap: 18px;
  align-items: start;
}

.step-list {
  display: grid;
  gap: 10px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.step-list li {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fff;
}

@media (max-width: 900px) {
  .reset-layout {
    grid-template-columns: 1fr;
  }
}
</style>
