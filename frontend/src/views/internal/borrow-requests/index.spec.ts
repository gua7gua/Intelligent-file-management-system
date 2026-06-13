import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import MyBorrowRequests from './index.vue'

const stubs = {
  ElDrawer: { template: '<div class="el-drawer-stub" />' },
}

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

async function mountComponent(initialPath = '/internal/borrow-requests') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/internal/borrow-requests', component: MyBorrowRequests }],
  })
  await router.push(initialPath)
  await router.isReady()
  return mount(MyBorrowRequests, { global: { plugins: [router], stubs } })
}

describe('MyBorrowRequests', () => {
  it('renders list covering multiple statuses', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()

    expect(wrapper.text()).toContain('BR-202606-021')
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('shows export voucher button for approved and voucher_issued', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()

    const exportButtons = wrapper.findAll('button').filter((b) => b.text().includes('导出凭证'))
    expect(exportButtons.length).toBe(2)
  })

  it('filters list by status', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()

    await wrapper.find('select').setValue('rejected')
    const queryBtn = wrapper.findAll('button').find((b) => b.text().includes('查询'))
    await queryBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('BR-202605-017')
    expect(wrapper.text()).not.toContain('BR-202606-021')
  })
})
