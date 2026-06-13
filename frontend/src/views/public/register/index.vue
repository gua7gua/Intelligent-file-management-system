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
        <div class="flow-step"><strong>公开检索</strong>仅检索非密、公开、未销毁的正式档案。</div>
        <div class="flow-step"><strong>征集清单</strong>只登记捐赠意向和文件名，不上传文件本体。</div>
        <div class="flow-step"><strong>内部账号</strong>由系统管理员维护，不在公众注册页创建。</div>
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
              :disabled="codeSent"
              @click="handleSendCode"
            >
              {{ codeSent ? '验证码已发送' : '发送验证码' }}
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
            >我确认公众账号仅用于公开档案检索、下载和征集意向提交；下载电子文件需重新经过后端鉴权并记录日志。</span
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { validatePublicRegister } from '@/utils/publicValidation'
import { sendPublicSmsCode, registerPublicUser } from '@/api/auth'

const router = useRouter()

const form = reactive({
  realName: '',
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  accepted: false,
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
    await sendPublicSmsCode({ phone: form.phone, scene: 'register' })
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
  const errors = validatePublicRegister(form)
  if (errors.length > 0) {
    resultMessage.value = errors.join(' ')
    resultType.value = 'danger'
    return
  }
  try {
    const result = await registerPublicUser({
      phone: form.phone,
      smsCode: form.smsCode,
      password: form.password,
      realName: form.realName,
    })
    resultMessage.value = `注册成功：user_type=public，默认角色 ${result.roles.join(', ')}。即将进入公众概览。`
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
