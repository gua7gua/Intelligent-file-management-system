import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import AccessLogs from './index.vue'

async function wait() { await vi.dynamicImportSettled(); await flushPromises() }
async function mountIt() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/access-logs', component: AccessLogs }] })
  await router.push('/admin/access-logs')
  await router.isReady()
  return mount(AccessLogs, { global: { plugins: [router, ElementPlus] } })
}

describe('AccessLogs', () => {
  it('renders title and access records', async () => {
    const w = await mountIt()
    await wait()
    expect(w.text()).toContain('访问日志')
    expect(w.text()).toContain('查看元数据')
    expect(w.text()).toContain('下载')
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
    expect(w.findAll('tbody tr').length).toBeGreaterThan(before)
  })

  it('filters by accessType download', async () => {
    const w = await mountIt()
    await wait()
    // 选项：全部 / view_metadata / preview / download → 选 download
    await w.find('select[data-testid="accessType"]').setValue('download')
    await w.findAll('button').find((b) => b.text().includes('查询'))!.trigger('click')
    await wait()
    expect(w.findAll('tbody tr').length).toBeGreaterThan(0)
    expect(w.text()).toContain('下载')
  })
})
