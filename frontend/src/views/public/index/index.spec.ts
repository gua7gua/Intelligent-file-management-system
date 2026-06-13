import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PublicHome from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('PublicHome', () => {
  it('renders public home statistics and main entries', async () => {
    const wrapper = mount(PublicHome)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('公开档案')
    expect(wrapper.text()).toContain('公开检索')
    expect(wrapper.text()).toContain('征集清单')
  })
})
