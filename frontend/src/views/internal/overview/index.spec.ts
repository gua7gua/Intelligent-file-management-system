import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InternalOverview from './index.vue'

async function waitForAsyncData() {
  // 全量并发下 jsdom environment 初始化较慢，轮询 flush 容忍 onMounted 异步链延迟
  for (let i = 0; i < 20; i++) {
    await new Promise((r) => setTimeout(r, 100))
    await flushPromises()
  }
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
