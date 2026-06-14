import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import BorrowApproval from './index.vue'

const stubs = {
  ElMessageBox: { template: '<div />' },
}

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/borrow-approval', component: BorrowApproval }],
  })
  await router.push('/admin/borrow-approval')
  await router.isReady()
  return mount(BorrowApproval, { global: { plugins: [router], stubs } })
}

describe('BorrowApproval', () => {
  it('renders list covering multiple statuses', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('JY-2026-0017')
    expect(wrapper.text()).toContain('待审批')
  })

  it('filters by status tab', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const loanTab = tabs.find((t) => t.text().includes('借出中'))
    await loanTab?.trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('JY-2026-0009')
  })

  it('disables approve buttons for non-applied request', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const loanTab = tabs.find((t) => t.text().includes('借出中'))
    await loanTab?.trigger('click')
    await waitForAsyncData()
    const firstCard = wrapper.find('.request-card')
    await firstCard.trigger('click')
    await waitForAsyncData()
    const approveBtn = wrapper.findAll('button').find((b) => b.text().includes('审批通过'))
    expect((approveBtn?.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows detail fields after selecting', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await wrapper.find('.request-card').trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('可借检查')
    expect(wrapper.text()).toContain('凭证、出库与归还')
  })
})
