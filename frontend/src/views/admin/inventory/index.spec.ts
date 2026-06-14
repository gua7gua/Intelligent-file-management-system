import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Inventory from './index.vue'

const stubs = { ElMessageBox: { template: '<div />' } }

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/inventory', component: Inventory }],
  })
  await router.push('/admin/inventory')
  await router.isReady()
  return mount(Inventory, { global: { plugins: [router], stubs } })
}

describe('Inventory', () => {
  it('renders running tasks', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('2026 年度 401 库房科技档案盘点')
    expect(wrapper.text()).toContain('进行中')
  })

  it('shows detail items after selecting a running task', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await wrapper.find('.task-item').trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('采油一厂设备验收报告')
    expect(wrapper.text()).toContain('盘点结果')
  })

  it('opens create task modal', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const btn = wrapper.findAll('button').find((b) => b.text().includes('新建盘点任务'))
    await btn?.trigger('click')
    expect(wrapper.text()).toContain('新建盘点任务')
    expect(wrapper.find('.modal').exists()).toBe(true)
  })

  it('filter tasks by status tab', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const draftTab = tabs.find((t) => t.text().includes('草稿'))
    await draftTab?.trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('2026 年度 403 库房音像档案盘点')
  })
})
