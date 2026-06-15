import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import AuditLogs from './index.vue'

async function wait() { await new Promise((r) => setTimeout(r, 80)); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/audit-logs', component: AuditLogs }] })
  await router.push('/admin/audit-logs')
  await router.isReady()
  return mount(AuditLogs, { global: { plugins: [router, ElementPlus] } })
}

describe('AuditLogs', () => {
  it('renders title and audit records', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('审计日志')
    expect(w.text()).toContain('禁用账号')
    expect(w.text()).toContain('自动备份')
  })

  it('shows 导出 button and no 删除/编辑 entry', async () => {
    const w = await mountIt()
    await wait()
    expect(w.findAll('button').some((b) => b.text().includes('导出'))).toBe(true)
    expect(w.findAll('button').some((b) => b.text().includes('删除') || b.text().includes('编辑'))).toBe(false)
  })

  it('appends more records on 加载更多', async () => {
    const w = await mountIt()
    await wait()
    const before = w.findAll('tbody tr').length
    const more = w.findAll('button').find((b) => b.text().includes('加载更多'))
    expect(more).toBeTruthy()
    await more!.trigger('click')
    await wait()
    const after = w.findAll('tbody tr').length
    expect(after).toBeGreaterThan(before)
  })

  it('filters by actorType system', async () => {
    const w = await mountIt()
    await wait()
    // 选项：全部 / internal / public / system → 选 system
    await w.find('select[data-testid="actorType"]').setValue('system')
    await w.findAll('button').find((b) => b.text().includes('查询'))!.trigger('click')
    await wait()
    expect(w.text()).toContain('自动备份')
    expect(w.findAll('tbody tr').length).toBeGreaterThan(0)
  })
})
