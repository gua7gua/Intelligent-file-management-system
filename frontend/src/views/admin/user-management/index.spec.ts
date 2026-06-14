import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import UserManagement from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/user-management', component: UserManagement }] })
  await router.push('/admin/user-management')
  await router.isReady()
  return mount(UserManagement, { global: { plugins: [router] } })
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
    // 进入新建态后登录名输入框为空
    const inputs = w.findAll('input')
    const loginInput = inputs.find((i) => (i.element as HTMLInputElement).disabled === false)
    expect((loginInput?.element as HTMLInputElement).value ?? '').toBe('')
  })
})
