import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RegisterPage from './index.vue'

describe('RegisterPage', () => {
  it('renders public registration boundary and required fields', () => {
    const wrapper = mount(RegisterPage)

    expect(wrapper.text()).toContain('公众账号')
    expect(wrapper.find('#phone').exists()).toBe(true)
    expect(wrapper.find('#code').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })
})
