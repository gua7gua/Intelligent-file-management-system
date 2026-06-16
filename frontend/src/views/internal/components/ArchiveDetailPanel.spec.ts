import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ArchiveDetailPanel from './ArchiveDetailPanel.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('ArchiveDetailPanel', () => {
  it('loads and renders archive detail from mock API', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: 101 } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('KJ-2025-0188')
    expect(wrapper.text()).toContain('如需纸质原件，请提交借阅申请')
  })

  it('disables borrow button for electronic-only archive', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: 102 } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('不支持')
    const borrowBtn = wrapper.findAll('button').find((b) => b.text().includes('申请借阅'))
    expect(borrowBtn?.attributes('disabled')).toBeDefined()
    expect(borrowBtn?.attributes('title')).toContain('纯电子档案不支持纸质借阅')
  })

  it('shows placeholder when archiveId is null', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: null } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('点击左侧结果行')
  })
})
