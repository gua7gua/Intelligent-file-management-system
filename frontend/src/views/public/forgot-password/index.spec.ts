import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ForgotPassword from './index.vue'

describe('ForgotPassword', () => {
  it('renders public-only reset boundary and form fields', () => {
    const wrapper = mount(ForgotPassword)

    expect(wrapper.text()).toContain('重置公众账号密码')
    expect(wrapper.text()).toContain('如需重置内部账号，请联系管理员。')
    expect(wrapper.find('#phone').exists()).toBe(true)
    expect(wrapper.find('#code').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })
})
