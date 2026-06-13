import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import InternalArchiveDetail from './detail.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalArchiveDetail', () => {
  it('reads archiveId from route and loads detail', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/internal/archives/:id', component: InternalArchiveDetail }],
    })
    await router.push('/internal/archives/101')
    const wrapper = mount(InternalArchiveDetail, { global: { plugins: [router] } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('返回检索')
  })
})
