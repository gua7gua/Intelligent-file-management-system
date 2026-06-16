import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CollectionPage from './index.vue'

describe('CollectionPage', () => {
  it('renders collection draft form and no-upload boundary', () => {
    const wrapper = mount(CollectionPage)

    expect(wrapper.text()).toContain('征集清单')
    expect(wrapper.text()).toContain('文件不会上传')
    expect(wrapper.text()).toContain('提交捐赠意向')
  })
})
