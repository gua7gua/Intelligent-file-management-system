// src/utils/appraisalValidation.ts
import type { AppraisalItem } from '@/types/appraisal'

/** 校验单条鉴定结论，返回错误信息数组（空表示通过） */
export function validateAppraisalItem(item: AppraisalItem): string[] {
  const errors: string[] = []
  if (!item.appraisalResult) {
    errors.push('请选择鉴定结论（延长保存或待销毁）。')
    return errors
  }
  if (item.appraisalResult === 'extend') {
    if (!item.newRetentionPeriod) errors.push('延长保存需填写新保管期限。')
    if (!item.newRetentionUntil) errors.push('延长保存需填写新到期日。')
  }
  return errors
}

/** 校验整批鉴定是否可完成，返回错误信息数组（空表示可完成） */
export function validateAppraisalCompletion(items: AppraisalItem[]): string[] {
  const errors: string[] = []
  const unprocessed = items.filter((i) => !i.appraisalResult)
  if (unprocessed.length > 0) {
    errors.push(`仍有未处理的鉴定条目，请逐条给出结论后再完成。`)
  }
  items.forEach((item) => {
    const itemErrors = validateAppraisalItem(item)
    itemErrors.forEach((msg) => {
      if (!errors.includes(msg)) errors.push(msg)
    })
  })
  return errors
}
