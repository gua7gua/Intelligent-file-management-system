import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Warehouse from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 80))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/warehouse', component: Warehouse }],
  })
  await router.push('/admin/warehouse')
  await router.isReady()
  return mount(Warehouse, { global: { plugins: [router] } })
}

describe('Warehouse', () => {
  it('renders room list with warning room', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('综合档案库房')
    expect(wrapper.text()).toContain('401')
    expect(wrapper.text()).toContain('容量告警')
  })

  it('loads locations and renders slots after room select', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('架位编码')
    expect(wrapper.findAll('.slot').length).toBeGreaterThan(0)
  })

  it('opens create room dialog', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const btn = wrapper.findAll('button').find((b) => b.text().includes('添加库房'))
    await btn?.trigger('click')
    expect(wrapper.text()).toContain('添加库房')
    expect(wrapper.find('.modal').exists()).toBe(true)
  })

  it('renders recent boxes table', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('最近占用盒位')
    expect(wrapper.text()).toContain('BX-2026-001')
  })
})
