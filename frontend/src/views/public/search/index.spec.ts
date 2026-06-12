import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PublicSearch from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('PublicSearch', () => {
  it('renders AI query, public search results, and download boundary', async () => {
    const wrapper = mount(PublicSearch)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('公开档案检索')
    expect(wrapper.text()).toContain('AI')
    expect(wrapper.text()).toContain('下载需登录')
  })
})
