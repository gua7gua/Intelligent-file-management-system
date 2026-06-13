import type { BorrowRequestCreateData } from '@/types/internal'

/** 校验借阅申请表单，返回错误信息数组（空数组表示通过） */
export function validateBorrowRequest(data: Partial<BorrowRequestCreateData>): string[] {
  const errors: string[] = []

  if (!data.reason || !data.reason.trim()) {
    errors.push('请填写借阅理由。')
  }
  if (!data.expectedDays || data.expectedDays < 1) {
    errors.push('借阅天数至少 1 天。')
  }
  if (!data.expectedVisitAt) {
    errors.push('请选择预计到馆时间。')
  }
  const phone = data.contactPhone?.trim() ?? ''
  if (!phone) {
    errors.push('请填写联系电话。')
  } else if (!/^1[3-9]\d{9}$/.test(phone)) {
    errors.push('联系电话格式不正确。')
  }

  return errors
}
