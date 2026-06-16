import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import SystemSettings from './index.vue'

async function wait() { await vi.dynamicImportSettled(); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/system-settings', component: SystemSettings }] })
  await router.push('/admin/system-settings')
  await router.isReady()
  return mount(SystemSettings, { global: { plugins: [router] } })
}

describe('SystemSettings', () => {
  it('renders config sections and format tags', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('系统配置')
    expect(w.text()).toContain('文件上传配置')
    expect(w.text()).toContain('能力开关')
    expect(w.text()).toContain('上传格式白名单')
    expect(w.findAll('.format-tag').length).toBeGreaterThan(0)
  })

  it('shows non-editable audit retention row', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('审计日志保留策略')
  })

  it('renders ai and public search switches on by default', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('AI 功能开关')
    expect(w.text()).toContain('公开检索开关')
  })
})
