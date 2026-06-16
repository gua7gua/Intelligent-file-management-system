<template>
  <div class="user-mgmt">
    <section>
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">维护组织、全宗、账号和预设角色。</p>
    </section>

    <section class="grid four" aria-label="用户概览">
      <div class="metric card"><div class="metric-num">{{ internalCount }}</div><div class="metric-label">内部账号</div></div>
      <div class="metric card"><div class="metric-num">{{ publicCount }}</div><div class="metric-label">公众账号</div></div>
      <div class="metric card"><div class="metric-num">{{ disabledCount }}</div><div class="metric-label">禁用账号</div><div class="metric-note">历史日志与业务记录保留</div></div>
      <div class="metric card"><div class="metric-num">{{ sysAdminCount }}</div><div class="metric-label">系统管理员</div></div>
    </section>

    <div class="toolbar">
      <div class="actions-left">
        <select v-model="roleFilter" aria-label="角色筛选">
          <option value="">全部角色</option>
          <option v-for="r in roles" :key="r.roleCode" :value="r.roleCode">{{ r.roleName }}</option>
        </select>
        <el-button @click="applyFilter">筛选</el-button>
      </div>
      <div class="actions">
        <el-button @click="newUser">新建用户</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存用户</el-button>
      </div>
    </div>

    <div class="user-layout">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">账号列表</h2>
          <div v-if="loading && users.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadAll">重试</button></div>
          <div v-else-if="users.length === 0" class="detail-empty">暂无用户</div>
          <div v-else class="table-wrap">
            <table>
              <colgroup>
                <col /><col /><col /><col /><col /><col /><col />
                <col style="width: 200px" />
              </colgroup>
              <thead>
                <tr><th>登录名</th><th>姓名</th><th>单位/部门</th><th>角色</th><th>数据范围</th><th>密级上限</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="u in users" :key="u.id">
                  <td class="mono">{{ u.loginName }}</td>
                  <td>{{ u.realName }}</td>
                  <td>{{ (u.organizationName || '-') + ' / ' + (u.departmentName || '-') }}</td>
                  <td><span class="status" :class="roleTagClass(u.roleCodes[0])">{{ roleNameOf(u.roleCodes[0]) }}</span></td>
                  <td>{{ dataScopeLabel[u.dataScope] || u.dataScope }}</td>
                  <td>{{ securityLabel(u.maxSecurityLevel) }}</td>
                  <td><span class="status" :class="u.status === 'active' ? 'success' : 'danger'">{{ UserStatusLabel[u.status] }}</span></td>
                  <td>
                    <div class="actions">
                      <el-button size="small" @click="editUser(u)">编辑</el-button>
                      <el-button size="small" type="warning" @click="resetPwd(u)">重置</el-button>
                      <el-button size="small" :type="u.status === 'active' ? 'danger' : 'success'" @click="toggleStatus(u)">{{ u.status === 'active' ? '禁用' : '启用' }}</el-button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="grid two">
          <div class="card panel">
            <h2 class="section-title">组织</h2>
            <ul class="mini-list">
              <li v-for="o in organizations" :key="o.id">
                <strong>{{ o.orgName }}</strong>
                <span class="muted">{{ orgTypeLabel[o.orgType] || o.orgType }} · 状态 {{ o.status === 'active' ? '正常' : '停用' }}</span>
                <el-button
                  size="small"
                  :type="o.status === 'active' ? 'warning' : 'success'"
                  @click="toggleOrgStatus(o)"
                >{{ o.status === 'active' ? '停用' : '启用' }}</el-button>
              </li>
            </ul>
          </div>
          <div class="card panel">
            <h2 class="section-title">全宗</h2>
            <ul class="mini-list">
              <li v-for="f in fonds" :key="f.id">
                <strong class="mono">{{ f.fondsNo }} · {{ f.fondsName }}</strong>
                <span class="muted">关联组织：{{ f.organizationName || '-' }}</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">账号编辑</h2>
          <div class="form-grid">
            <div class="field"><label>登录名</label><input v-model="form.loginName" :disabled="!isCreate" /></div>
            <div class="field"><label>姓名</label><input v-model="form.realName" /></div>
            <div class="field">
              <label>用户类型</label>
              <select v-model="form.userType"><option value="internal">内部账号</option><option value="public">公众账号</option></select>
            </div>
            <div class="field"><label>联系电话</label><input v-model="form.phone" /></div>
            <div class="field">
              <label>所属单位 <button type="button" class="link" @click="openOrgDialog">+新增组织</button></label>
              <select v-model="form.organizationId">
                <option v-for="o in organizations" :key="o.id" :value="o.id">{{ o.orgName }}</option>
              </select>
            </div>
            <div class="field"><label>所属部门</label><input v-model="form.departmentName" /></div>
            <div class="field">
              <label>数据范围</label>
              <select v-model="form.dataScope"><option value="all">全部</option><option value="own_org">本机构</option><option value="own_fonds">本全宗</option></select>
            </div>
            <div class="field">
              <label>密级上限</label>
              <select v-model.number="form.maxSecurityLevel">
                <option :value="0">非密</option><option :value="1">内部</option><option :value="2">秘密</option><option :value="3">机密</option><option :value="4">绝密</option>
              </select>
            </div>
          </div>
          <div v-if="isCreate" class="field" style="margin-top:10px"><label>初始密码</label><input v-model="form.initialPassword" type="text" /></div>

          <h3 class="section-title" style="margin-top:16px">预设角色</h3>
          <div class="role-grid">
            <label class="role-check" v-for="r in internalRoles" :key="r.roleCode">
              <input type="checkbox" :value="r.roleCode" v-model="form.roleCodes" />
              {{ r.roleName }}
            </label>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">系统角色</h2>
          <div class="role-tags">
            <span class="status" v-for="r in roles" :key="r.roleCode">{{ r.roleName }}</span>
          </div>
        </div>

        <div class="notice">
          <strong>审计留痕</strong>
          <div>用户禁用、密码重置、角色变更会记录审计日志，历史审批、移交和操作记录保留原用户信息。</div>
        </div>
      </aside>
    </div>

    <el-dialog v-if="orgDialogVisible" v-model="orgDialogVisible" title="新增组织" width="420px" :append-to-body="false">
      <div class="form-grid" style="grid-template-columns:1fr">
        <div class="field"><label>组织名称</label><input v-model="orgForm.orgName" data-testid="orgName" placeholder="组织名称" /></div>
        <div class="field">
          <label>组织类型</label>
          <select v-model="orgForm.orgType">
            <option value="archive_org">档案机构</option>
            <option value="government">政府机关</option>
            <option value="enterprise">企业</option>
            <option value="public_institution">事业单位</option>
          </select>
        </div>
        <div class="field"><label>联系人</label><input v-model="orgForm.contactName" /></div>
        <div class="field"><label>联系电话</label><input v-model="orgForm.contactPhone" /></div>
      </div>
      <template #footer>
        <el-button @click="orgDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orgSaving" @click="submitOrg">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, getRoles, getUserDetail, getUsers, resetUserPassword, updateUser, updateUserStatus } from '@/api/user-management'
import { createOrganization, getOrganizations, updateOrganization } from '@/api/organizations'
import { getFonds } from '@/api/fonds'
import type { Role, User } from '@/types/user-management'
import type { Organization } from '@/types/organization'
import type { FondsReference } from '@/types/fonds'
import type { UserTypeValue } from '@/types/enums'
import { UserStatusLabel } from '@/types/enums'
import { canDisableUser, validateUserForm } from '@/utils/userValidation'

const users = ref<User[]>([])
const roles = ref<Role[]>([])
const organizations = ref<Organization[]>([])
const fonds = ref<FondsReference[]>([])
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const orgDialogVisible = ref(false)
const orgSaving = ref(false)
const orgForm = reactive({
  orgName: '',
  orgType: 'government' as 'archive_org' | 'government' | 'enterprise' | 'public_institution',
  contactName: '',
  contactPhone: '',
})
const roleFilter = ref('')
const appliedRoleFilter = ref('')

const isCreate = ref(true)
const editingId = ref<number | null>(null)
const form = reactive({
  loginName: '',
  realName: '',
  userType: 'internal' as UserTypeValue,
  phone: '',
  organizationId: undefined as number | undefined,
  departmentName: '',
  dataScope: 'all' as 'all' | 'own_org' | 'own_fonds',
  maxSecurityLevel: 0,
  roleCodes: [] as string[],
  initialPassword: '123456',
})

const internalRoles = computed(() => roles.value.filter((r) => r.roleCode !== 'public_user'))
const internalCount = computed(() => users.value.filter((u) => u.userType === 'internal').length)
const publicCount = computed(() => users.value.filter((u) => u.userType === 'public').length)
const disabledCount = computed(() => users.value.filter((u) => u.status === 'disabled').length)
const sysAdminCount = computed(() => users.value.filter((u) => u.roleCodes.includes('sys_admin')).length)

const securityLevels = ['非密', '内部', '秘密', '机密', '绝密']
function securityLabel(level: number): string {
  return securityLevels[level] ?? '非密'
}
function roleTagClass(code: string): string {
  return code === 'director' ? 'warning' : code === 'sys_admin' ? 'info' : ''
}
const dataScopeLabel: Record<string, string> = { all: '全部', own_org: '本机构', own_fonds: '本全宗' }
const orgTypeLabel: Record<string, string> = { archive_org: '档案机构', government: '政府机关', enterprise: '企业', public_institution: '事业单位' }
function roleNameOf(code?: string): string {
  if (!code) return '-'
  return roles.value.find((r) => r.roleCode === code)?.roleName ?? code
}

async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    const [u, r, o, f] = await Promise.all([getUsers({ roleCode: appliedRoleFilter.value || undefined }), getRoles(), getOrganizations(), getFonds()])
    users.value = u.records
    roles.value = r
    organizations.value = o.records
    fonds.value = f.records
  } catch {
    loadError.value = true
    ElMessage.error('用户数据加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  appliedRoleFilter.value = roleFilter.value
  loadAll()
}

function newUser() {
  isCreate.value = true
  editingId.value = null
  form.loginName = ''
  form.realName = ''
  form.userType = 'internal'
  form.phone = ''
  form.organizationId = organizations.value[0]?.id
  form.departmentName = ''
  form.dataScope = 'all'
  form.maxSecurityLevel = 0
  form.roleCodes = ['internal_reader']
  form.initialPassword = '123456'
  ElMessage.success('已进入新建用户状态。')
}

async function editUser(u: User) {
  const detail = await getUserDetail(u.id)
  isCreate.value = false
  editingId.value = u.id
  form.loginName = detail.loginName
  form.realName = detail.realName
  form.userType = detail.userType
  form.phone = detail.phone ?? ''
  form.organizationId = detail.organizationId
  form.departmentName = detail.departmentName ?? ''
  form.dataScope = detail.dataScope
  form.maxSecurityLevel = detail.maxSecurityLevel
  form.roleCodes = [...detail.roleCodes]
  ElMessage.success('已载入账号：' + detail.loginName)
}

async function saveUser() {
  const existing = users.value.filter((u) => u.id !== editingId.value).map((u) => u.loginName)
  const result = validateUserForm(
    { loginName: form.loginName, realName: form.realName, roleCodes: form.roleCodes, maxSecurityLevel: form.maxSecurityLevel, dataScope: form.dataScope, initialPassword: form.initialPassword },
    existing,
    isCreate.value,
  )
  if (!result.valid) {
    ElMessage.error(result.errors[0])
    return
  }
  saving.value = true
  try {
    if (isCreate.value) {
      await createUser({
        userType: form.userType, loginName: form.loginName, realName: form.realName, phone: form.phone,
        organizationId: form.organizationId, departmentName: form.departmentName, maxSecurityLevel: form.maxSecurityLevel,
        dataScope: form.dataScope, roleCodes: form.roleCodes, initialPassword: form.initialPassword,
      })
      ElMessage.success('用户已保存。')
    } else {
      await updateUser(editingId.value!, {
        realName: form.realName, phone: form.phone, organizationId: form.organizationId, departmentName: form.departmentName,
        maxSecurityLevel: form.maxSecurityLevel, dataScope: form.dataScope, roleCodes: form.roleCodes,
      })
      ElMessage.success('用户已更新。')
    }
    await loadAll()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存用户失败')
  } finally {
    saving.value = false
  }
}

async function resetPwd(u: User) {
  try {
    await ElMessageBox.confirm(`确认重置用户「${u.realName}」的密码？`, '重置密码', { type: 'warning' })
    await resetUserPassword(u.id, { newPassword: '123456' })
    ElMessage.success('已生成密码重置记录，需通知用户首次登录修改。')
  } catch {
    // 用户取消
  }
}

async function toggleStatus(u: User) {
  const next = u.status === 'active' ? 'disabled' : 'active'
  if (next === 'disabled') {
    const check = canDisableUser(u, users.value, { status: 'disabled' })
    if (!check.allowed) {
      ElMessage.error(check.reason || '不可禁用')
      return
    }
    try {
      const { value } = await ElMessageBox.prompt('请输入禁用原因', '禁用账号', { type: 'warning' })
      await updateUserStatus(u.id, { status: 'disabled', reason: value })
      ElMessage.success('账号已禁用，历史业务记录保留。')
    } catch {
      // 取消
    }
  } else {
    await updateUserStatus(u.id, { status: 'active' })
    ElMessage.success('账号已启用。')
  }
  await loadAll()
}

function openOrgDialog() {
  orgForm.orgName = ''
  orgForm.orgType = 'government'
  orgForm.contactName = ''
  orgForm.contactPhone = ''
  orgDialogVisible.value = true
}

async function submitOrg() {
  if (!orgForm.orgName.trim()) {
    ElMessage.error('组织名称不能为空')
    return
  }
  orgSaving.value = true
  try {
    await createOrganization({ orgName: orgForm.orgName, orgType: orgForm.orgType, contactName: orgForm.contactName, contactPhone: orgForm.contactPhone })
    ElMessage.success('组织已新增。')
    orgDialogVisible.value = false
    const o = await getOrganizations()
    organizations.value = o.records
  } catch (e) {
    ElMessage.error((e as Error).message || '新增组织失败')
  } finally {
    orgSaving.value = false
  }
}

// 组织停用/启用（§17.7 更新 status；停用而非物理删除，保留历史档案归属）
async function toggleOrgStatus(o: Organization) {
  const next: 'active' | 'disabled' = o.status === 'active' ? 'disabled' : 'active'
  const action = next === 'disabled' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}组织「${o.orgName}」？`, `${action}组织`, { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateOrganization(o.id, { status: next })
    ElMessage.success(`已${action}组织。`)
    const res = await getOrganizations()
    organizations.value = res.records
  } catch (e) {
    ElMessage.error((e as Error).message || `${action}组织失败`)
  }
}

onMounted(loadAll)
</script>

<style scoped>
.user-mgmt { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.grid.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin: 16px 0; }
.actions { display: flex; gap: 8px; }
.actions-left { display: flex; gap: 8px; align-items: center; }
/* 操作列：固定单行、重置 Element Plus 按钮间默认 12px 外边距，避免阶梯/三角形错排 */
.user-mgmt .table-wrap td .actions { flex-wrap: nowrap; gap: 6px; align-items: center; }
.user-mgmt :deep(.table-wrap .el-button + .el-button) { margin-left: 0; }
.user-mgmt :deep(.table-wrap .actions .el-button) { flex-shrink: 0; }
.user-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 16px; align-items: start; }
.stack { display: grid; gap: 16px; }
.role-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.role-check { display: flex; min-height: 44px; align-items: center; gap: 8px; padding: 9px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; font-weight: 700; }
.role-check input { width: 17px; height: 17px; }
.mini-list { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.mini-list li { display: grid; gap: 5px; padding: 11px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select { height: 34px; padding: 0 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
@media (max-width: 1120px) { .user-layout { grid-template-columns: 1fr; } }
</style>
