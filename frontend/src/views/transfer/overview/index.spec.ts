import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TransferOverview from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('TransferOverview', () => {
  it('renders dashboard metrics, list, and rejection reason from mock API', async () => {
    const wrapper = mount(TransferOverview)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('移交工作台')
    expect(wrapper.text()).toContain('部分接收')
    expect(wrapper.text()).toContain('新建清单')

    // 点击部分接收批次 TR-2026-0003 查看回退原因
    const buttons = wrapper.findAll('.batch-row button')
    for (const btn of buttons) {
      if (btn.text().includes('TR-2026-0003')) {
        await btn.trigger('click')
        break
      }
    }
    // 等待 watch 触发的 loadDetail（动态 import）完成
    await waitForAsyncData()

    expect(wrapper.text()).toContain('回退原因')
  })
})
