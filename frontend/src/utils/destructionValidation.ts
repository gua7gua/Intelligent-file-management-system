// src/utils/destructionValidation.ts
import type { DestroyMethodValue } from '@/types/enums'

/** 销毁确认表单输入（含 UI 专用字段 irrevocableConfirm / photoCount） */
export interface DestroyConfirmInput {
  destroyMethod: DestroyMethodValue | ''
  supervisorName1: string
  supervisorName2: string
  destroyNote: string
  irrevocableConfirm: boolean
  photoCount: number
}

/** 校验销毁确认表单，返回错误信息数组（空表示通过） */
export function validateDestroyConfirm(input: DestroyConfirmInput): string[] {
  const errors: string[] = []
  if (!input.destroyMethod) errors.push('请选择销毁方式。')
  if (!input.supervisorName1.trim() || !input.supervisorName2.trim()) {
    errors.push('请填写两名监销人。')
  } else if (input.supervisorName1.trim() === input.supervisorName2.trim()) {
    errors.push('两名监销人不能为同一人。')
  }
  if (!input.destroyNote.trim()) errors.push('请填写销毁说明。')
  if (!input.irrevocableConfirm) errors.push('请确认销毁后档案状态不可恢复。')
  if (input.photoCount < 1) errors.push('请至少上传一张现场照片。')
  return errors
}
