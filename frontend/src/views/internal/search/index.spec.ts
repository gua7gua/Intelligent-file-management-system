import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InternalSearch from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalSearch', () => {
  it('renders AI panel, structured filters, and result area', async () => {
    const wrapper = mount(InternalSearch)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('AI 检索条件生成')
    expect(wrapper.text()).toContain('结构化检索条件')
    expect(wrapper.text()).toContain('检索结果')
  })

  it('loads results on search and opens detail panel', async () => {
    const wrapper = mount(InternalSearch)
    await waitForAsyncData()

    const searchBtn = wrapper.findAll('button').find((b) => b.text().includes('确认检索'))
    await searchBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')

    const detailBtn = wrapper.findAll('button').find((b) => b.text() === '详情')
    await detailBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('KJ-2025-0188')
  })
})
