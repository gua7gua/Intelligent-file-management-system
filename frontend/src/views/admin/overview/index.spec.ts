import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Overview from './index.vue'

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/overview', component: Overview },
      { path: '/admin/archive-management', component: { template: '<div />' } },
      { path: '/admin/preservation', component: { template: '<div />' } },
    ],
  })
  await router.push('/admin/overview')
  await router.isReady()
  const wrapper = mount(Overview, { global: { plugins: [router] } })
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
  return wrapper
}

describe('AdminOverview', () => {
  it('renders eight metric cards with values', async () => {
    const wrapper = await mountComponent()
    const metrics = wrapper.findAll('.metric')
    expect(metrics.length).toBe(8)
    expect(wrapper.text()).toContain('馆藏总量')
    expect(wrapper.text()).toContain('存储使用率')
  })

  it('renders todo entries and audit logs', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.findAll('.todo').length).toBeGreaterThanOrEqual(4)
    expect(wrapper.findAll('.log-row').length).toBeGreaterThanOrEqual(4)
  })

  it('updates role notice text on role switch', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.text()).toContain('后台管理员')
    const select = wrapper.find('#roleSelect')
    await select.setValue('馆领导')
    expect(wrapper.find('.notice').text()).toContain('馆领导')
  })

  it('renders storage warning severity when usage exceeds threshold', async () => {
    const wrapper = await mountComponent()
    const storageMetric = wrapper.findAll('.metric').find((m) => m.text().includes('存储使用率'))
    expect(storageMetric?.classes()).toContain('danger')
  })
})
