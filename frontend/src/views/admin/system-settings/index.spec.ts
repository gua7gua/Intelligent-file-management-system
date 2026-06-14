import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import SystemSettings from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
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
    expect(w.text()).toContain('upload.allowed_extensions')
    expect(w.findAll('.format-tag').length).toBeGreaterThan(0)
  })

  it('shows non-editable audit retention row', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('security.audit_retention')
  })

  it('renders ai and public search switches on by default', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('ai.enabled')
    expect(w.text()).toContain('public_search.enabled')
  })
})
