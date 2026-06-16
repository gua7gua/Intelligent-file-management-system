import { describe, expect, it } from 'vitest'
import {
  validateCollectionDraft,
  validatePublicRegister,
  validateResetPassword,
} from './publicValidation'

describe('publicValidation', () => {
  it('validates public registration fields', () => {
    expect(
      validatePublicRegister({
        realName: '',
        phone: '123',
        smsCode: '',
        password: 'Public@2026',
        confirmPassword: 'Public@2027',
        accepted: false,
      }),
    ).toEqual([
      '请填写姓名。',
      '请输入有效的 11 位手机号。',
      '请填写短信验证码。',
      '两次密码不一致。',
      '请确认公众账号使用说明。',
    ])
  })

  it('validates reset password fields', () => {
    expect(
      validateResetPassword({
        phone: '13800000005',
        smsCode: '',
        newPassword: 'Public@2026',
        confirmPassword: 'Public@2027',
      }),
    ).toEqual(['请填写短信验证码。', '两次新密码不一致。'])
  })

  it('requires collection agreement and at least one item', () => {
    expect(
      validateCollectionDraft({
        title: '',
        contactName: '',
        contactPhone: 'abc',
        agreementAccepted: false,
        items: [],
      }),
    ).toEqual([
      '请填写清单标题。',
      '请填写联系人。',
      '请输入有效的联系电话。',
      '请填写档案所属年度。',
      '请至少添加一条征集条目。',
      '请勾选在线捐赠协议。',
    ])
  })
})
