<template>
  <main class="auth-shell">
    <!-- 左侧说明区 -->
    <section class="auth-aside" aria-label="系统门户说明">
      <div>
        <p class="status info">统一认证入口</p>
        <h1 class="page-title" style="color: #fff; margin-top: 14px">智能档案管理系统</h1>
        <p style="max-width: 560px; color: rgba(255, 255, 255, 0.82)">
          登录后按角色进入公众门户、内部查阅者门户、移交单位门户或管理后台。菜单展示只用于体验分流，接口权限由后端统一校验。
        </p>
      </div>
      <div class="portal-list">
        <div class="portal-item">
          <strong>移交单位门户</strong>
          <span>编制移交清单、导出打印、查看验收与入库进度。</span>
        </div>
        <div class="portal-item">
          <strong>公众门户</strong>
          <span>检索公开档案、提交征集清单、查看本人下载记录。</span>
        </div>
        <div class="portal-item">
          <strong>内部查阅者门户</strong>
          <span>按密级和数据范围检索档案、预览下载、申请纸质借阅。</span>
        </div>
        <div class="portal-item">
          <strong>管理后台</strong>
          <span>验收、AI 补全入库、上架、借阅审批、鉴定销毁和系统配置。</span>
        </div>
      </div>
    </section>

    <!-- 右侧表单区 -->
    <section class="auth-card" aria-label="登录表单">
      <h1>统一登录</h1>
      <p class="muted">请选择本次登录角色，原型将展示对应门户跳转意图。</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <div class="form-field">
          <label for="account">账号 / 手机号 / 工号</label>
          <el-form-item prop="username">
            <el-input
              id="account"
              v-model="form.username"
              placeholder="请输入账号"
              autocomplete="username"
              size="large"
            />
          </el-form-item>
        </div>
        <div class="form-field">
          <label for="password">密码</label>
          <el-form-item prop="password">
            <el-input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              size="large"
            />
          </el-form-item>
        </div>
        <div class="form-field">
          <label for="role">登录角色</label>
          <el-select id="role" v-model="form.role" placeholder="请选择角色" size="large" style="width: 100%">
            <el-option label="档案管理员（前台）" value="front_admin" />
            <el-option label="档案管理员（后台）" value="back_admin" />
            <el-option label="移交单位经办人" value="transfer_user" />
            <el-option label="内部查阅者" value="internal_user" />
            <el-option label="社会公众" value="public_user" />
            <el-option label="馆领导" value="leader" />
            <el-option label="系统管理员" value="sys_admin" />
          </el-select>
          <span class="hint">一人多角色时，后端仍按当前会话角色和数据范围校验。</span>
        </div>

        <div v-if="targetPortal" class="notice">
          登录后将进入：<strong>{{ targetPortal }}</strong>
        </div>

        <div class="actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin">
            登录
          </el-button>
          <router-link to="/public" class="button ghost">公众首页</router-link>
        </div>
      </el-form>

      <div class="login-footer">
        <router-link to="/public/register" class="button secondary">公众注册</router-link>
        <router-link to="/public/forgot-password" class="button ghost">忘记密码</router-link>
      </div>

      <div v-if="loginMessage" class="login-status" aria-live="polite">
        <div :class="['notice', loginMessageType]">{{ loginMessage }}</div>
      </div>

      <p class="hint">公众注册与找回密码只服务社会公众账号；内部账号、角色、状态由系统管理员在用户管理中维护。</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginMessage = ref('')
const loginMessageType = ref('')

const form = reactive({
  username: '',
  password: '',
  role: 'back_admin',
})

const rules: Record<string, FormItemRule[]> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/** 角色对应的门户路径 */
const roleTargets: Record<string, string> = {
  front_admin: '管理后台',
  back_admin: '管理后台',
  transfer_user: '移交单位门户',
  internal_user: '内部查阅者门户',
  public_user: '公众门户',
  leader: '管理后台',
  sys_admin: '管理后台',
}

/** 当前角色对应的门户名称 */
const targetPortal = computed(() => roleTargets[form.role] || '')

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  loginMessage.value = ''
  try {
    await authStore.login(form.username, form.password)
    const redirect = (route.query.redirect as string) || authStore.getDefaultPortal()
    router.push(redirect)
  } catch {
    loginMessage.value = '登录失败，请检查账号和密码。'
    loginMessageType.value = 'danger'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.portal-list {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.portal-item {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.88);
}

.portal-item strong {
  display: block;
  margin-bottom: 4px;
  color: #ffffff;
}

.auth-card h1 {
  margin: 0 0 8px;
  font-size: 25px;
}

.login-form {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.form-field {
  display: grid;
  gap: 6px;
}

.form-field label {
  color: #34414d;
  font-size: 13px;
  font-weight: 700;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.login-status {
  min-height: 44px;
  margin-top: 14px;
}

.login-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.login-footer .button {
  text-decoration: none;
}
</style>
