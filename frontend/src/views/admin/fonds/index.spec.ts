import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import FondsManage from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/fonds', component: FondsManage },
      { path: '/admin/user-management', component: { template: '<div>用户管理</div>' } },
    ],
  })
  await router.push('/admin/fonds')
  await router.isReady()
  return mount(FondsManage, { global: { plugins: [router, ElementPlus] } })
}

describe('FondsManage', () => {
  it('renders title, metrics and fonds list', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('全宗管理')
    expect(w.text()).toContain('F001')
    expect(w.text()).toContain('克拉玛依市档案馆综合全宗')
    expect(w.text()).toContain('启用')
  })

  it('shows new-fonds button', async () => {
    const w = await mountIt()
    await wait()
    expect(w.findAll('button').some((b) => b.text().includes('新建全宗'))).toBe(true)
  })

  it('enters create mode with editable fondsNo on 新建全宗', async () => {
    const w = await mountIt()
    await wait()
    const btn = w.findAll('button').find((b) => b.text().includes('新建全宗'))
    await btn?.trigger('click')
    await flushPromises()
    const noInput = w.find('input[data-testid="fondsNo"]').element as HTMLInputElement
    expect(noInput.disabled).toBe(false)
    expect(noInput.value).toBe('')
  })

  it('locks fondsNo readonly when editing an existing row', async () => {
    const w = await mountIt()
    await wait()
    const editBtn = w.findAll('button').find((b) => b.text().includes('编辑'))
    await editBtn?.trigger('click')
    await flushPromises()
    const noInput = w.find('input[data-testid="fondsNo"]').element as HTMLInputElement
    expect(noInput.disabled).toBe(true)
  })

  it('renders category distribution and recent intake for selected fonds', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('门类分布')
    expect(w.text()).toContain('最近入库记录')
    expect(w.text()).toContain('文书档案')
  })
})
