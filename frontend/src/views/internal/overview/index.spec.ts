import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InternalOverview from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalOverview', () => {
  it('renders metrics, recent views and borrow requests from mock', async () => {
    const wrapper = mount(InternalOverview)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('最近查阅')
    expect(wrapper.text()).toContain('待审批申请')
    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('BR-202606-021')
  })

  it('shows export voucher for approved and view link for others', async () => {
    const wrapper = mount(InternalOverview)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('导出凭证')
    expect(wrapper.text()).toContain('查看申请')
  })
})
