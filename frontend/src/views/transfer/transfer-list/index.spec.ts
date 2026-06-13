import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TransferList from './index.vue'

describe('TransferList', () => {
  it('renders transfer draft form and local file parsing boundary', () => {
    const wrapper = mount(TransferList)

    expect(wrapper.text()).toContain('编制移交清单')
    expect(wrapper.text()).toContain('只解析')
    expect(wrapper.text()).toContain('不上传文件本体')
    expect(wrapper.text()).toContain('提交清单')
  })
})
