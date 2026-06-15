import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import UserManagement from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/user-management', component: UserManagement }] })
  await router.push('/admin/user-management')
  await router.isReady()
  return mount(UserManagement, { global: { plugins: [router, ElementPlus] } })
}

describe('UserManagement', () => {
  it('renders metrics and account list', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('用户管理')
    expect(w.text()).toContain('小刘')
    expect(w.text()).toContain('back_archivist')
  })

  it('renders organization and fonds reference lists', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('克拉玛依市档案馆')
    expect(w.text()).toContain('F001')
  })

  it('enters new user state on 新建用户', async () => {
    const w = await mountIt()
    await wait()
    const btn = w.findAll('button').find((b) => b.text().includes('新建用户'))
    await btn?.trigger('click')
    await flushPromises()
    const inputs = w.findAll('input')
    const loginInput = inputs.find((i) => (i.element as HTMLInputElement).disabled === false)
    expect((loginInput?.element as HTMLInputElement).value ?? '').toBe('')
  })

  it('shows +新增组织 button beside 所属单位', async () => {
    const w = await mountIt()
    await wait()
    expect(w.findAll('button').some((b) => b.text().includes('新增组织'))).toBe(true)
  })

  it('opens org dialog and creates org on submit', async () => {
    const w = await mountIt()
    await wait()
    const btn = w.findAll('button').find((b) => b.text().includes('新增组织'))
    await btn?.trigger('click')
    await flushPromises()
    const nameInput = w.find('input[data-testid="orgName"]')
    expect(nameInput.exists()).toBe(true)
    await nameInput.setValue('市文旅局')
    const submit = w.findAll('button').find((b) => b.text().includes('确定'))
    await submit?.trigger('click')
    await flushPromises()
    await wait()
    expect(w.text()).toContain('市文旅局')
  })
})
