import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Statistics from './index.vue'

vi.mock('element-plus', () => ({ ElMessage: { success: () => {}, warning: () => {} } }))

async function mountComponent() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/statistics', component: Statistics }] })
  await router.push('/admin/statistics')
  await router.isReady()
  const wrapper = mount(Statistics, { global: { plugins: [router] } })
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
  return wrapper
}

describe('AdminStatistics', () => {
  it('renders four metric cards', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.findAll('.metric').length).toBe(4)
    expect(wrapper.text()).toContain('馆藏总量')
  })

  it('renders yearly bars and distributions', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.findAll('.bar-row').length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('年度进馆趋势')
    expect(wrapper.text()).toContain('档案门类分布')
    expect(wrapper.text()).toContain('纸电载体分布')
  })

  it('renders business breakdown and data sources', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.text()).toContain('业务汇总')
    expect(wrapper.text()).toContain('数据源状态')
  })

  it('narrows bars after refreshing with a year range', async () => {
    const wrapper = await mountComponent()
    const before = wrapper.findAll('.bar-row').length
    await wrapper.find('#yearStart').setValue(2025)
    await wrapper.find('#yearEnd').setValue(2026)
    await wrapper.find('button.refresh').trigger('click')
    await new Promise((r) => setTimeout(r, 50))
    await flushPromises()
    const after = wrapper.findAll('.bar-row').length
    expect(after).toBeLessThanOrEqual(before)
    expect(after).toBeGreaterThan(0)
  })
})
