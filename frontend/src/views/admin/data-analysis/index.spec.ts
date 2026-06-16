import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import DataAnalysis from './index.vue'

vi.mock('element-plus', () => ({ ElMessage: { success: () => {}, warning: () => {} } }))

async function mountComponent() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/data-analysis', component: DataAnalysis }] })
  await router.push('/admin/data-analysis')
  await router.isReady()
  const wrapper = mount(DataAnalysis, { global: { plugins: [router] } })
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
  return wrapper
}

describe('DataAnalysis', () => {
  it('renders task list and item suggestions', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.text()).toContain('任务状态')
    expect(wrapper.text()).toContain('异常建议列表')
    expect(wrapper.findAll('tbody tr').length).toBeGreaterThan(0)
  })

  it('selecting an item shows detail and adopt buttons', async () => {
    const wrapper = await mountComponent()
    await wrapper.find('tbody tr').trigger('click')
    await new Promise((r) => setTimeout(r, 20))
    await flushPromises()
    expect(wrapper.text()).toContain('条目详情')
    expect(wrapper.findAll('button').some((b) => b.text().includes('采纳'))).toBe(true)
  })

  it('create scan adds a running task', async () => {
    const wrapper = await mountComponent()
    const before = wrapper.findAll('.task-item').length
    await wrapper.find('button.scan-start').trigger('click')
    await new Promise((r) => setTimeout(r, 50))
    await flushPromises()
    expect(wrapper.findAll('.task-item').length).toBeGreaterThan(before)
  })
})
