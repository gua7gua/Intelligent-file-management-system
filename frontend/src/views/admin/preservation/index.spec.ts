import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Preservation from './index.vue'

async function wait() {
  await new Promise((r) => setTimeout(r, 80))
  await flushPromises()
}
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/preservation', component: Preservation }] })
  await router.push('/admin/preservation')
  await router.isReady()
  return mount(Preservation, { global: { plugins: [router] } })
}

describe('Preservation', () => {
  it('renders overview metrics and backup scope options', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('档案保存')
    expect(w.text()).toContain('数据库备份')
    expect(w.text()).toContain('电子文件备份')
    expect(w.text()).toContain('同时备份')
  })

  it('renders backup task table with failed reason', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('BAK-')
    expect(w.text()).toContain('对象存储连接超时')
  })

  it('renders four-property check cards with not_configured authenticity', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('完整性')
    expect(w.text()).toContain('真实性')
    expect(w.text()).toContain('not_configured')
  })
})
