import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createPinia } from 'pinia'
import PublicOverview from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('PublicOverview', () => {
  it('renders personal overview, collections, and download records', async () => {
    const wrapper = mount(PublicOverview, { global: { plugins: [createPinia()] } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('公众概览')
    expect(wrapper.text()).toContain('征集清单')
    expect(wrapper.text()).toContain('下载记录')
  })
})
