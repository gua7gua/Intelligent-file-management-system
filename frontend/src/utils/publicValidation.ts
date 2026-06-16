const phonePattern = /^1\d{10}$/

interface RegisterForm {
  realName: string
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
  accepted: boolean
}

interface ResetPasswordForm {
  phone: string
  smsCode: string
  newPassword: string
  confirmPassword: string
}

interface CollectionDraft {
  title: string
  contactName: string
  contactPhone: string
  archiveYear?: number
  agreementAccepted: boolean
  items: unknown[]
}

export function validatePublicRegister(form: RegisterForm): string[] {
  const errors: string[] = []
  if (!form.realName.trim()) errors.push('请填写姓名。')
  if (!phonePattern.test(form.phone)) errors.push('请输入有效的 11 位手机号。')
  if (!form.smsCode.trim()) errors.push('请填写短信验证码。')
  if (form.password !== form.confirmPassword) errors.push('两次密码不一致。')
  if (!form.accepted) errors.push('请确认公众账号使用说明。')
  return errors
}

export function validateResetPassword(form: ResetPasswordForm): string[] {
  const errors: string[] = []
  if (!phonePattern.test(form.phone)) errors.push('请输入有效的公众账号手机号。')
  if (!form.smsCode.trim()) errors.push('请填写短信验证码。')
  if (form.newPassword !== form.confirmPassword) errors.push('两次新密码不一致。')
  return errors
}

export function validateCollectionDraft(form: CollectionDraft): string[] {
  const errors: string[] = []
  if (!form.title.trim()) errors.push('请填写清单标题。')
  if (!form.contactName.trim()) errors.push('请填写联系人。')
  if (!phonePattern.test(form.contactPhone)) errors.push('请输入有效的联系电话。')
  if (!form.archiveYear) errors.push('请填写档案所属年度。')
  if (form.items.length === 0) errors.push('请至少添加一条征集条目。')
  if (!form.agreementAccepted) errors.push('请勾选在线捐赠协议。')
  return errors
}
