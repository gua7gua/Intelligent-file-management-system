import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import TransferList from './index.vue'

describe('TransferList', () => {
  it('renders transfer draft form and local file parsing boundary', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/transfer/transfer-list', component: TransferList }],
    })
    await router.push('/transfer/transfer-list')
    await router.isReady()
    const wrapper = mount(TransferList, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('编制移交清单')
    expect(wrapper.text()).toContain('文件不会上传')
    expect(wrapper.text()).toContain('提交清单')
  })
})
