export interface FondsFormData {
  fondsNo: string
  fondsName: string
  organizationId?: number
}

export interface FondsValidationResult {
  valid: boolean
  errors: string[]
}

/** 校验新建/编辑全宗表单（新建时全宗号可留空由后端自动生成；填写时校验唯一；名称必填、所属单位必填） */
export function validateFondsForm(
  data: FondsFormData,
  existingFondsNos: string[],
  isCreate: boolean,
): FondsValidationResult {
  const errors: string[] = []
  const no = data.fondsNo.trim()
  if (no && isCreate && existingFondsNos.map((n) => n.toLowerCase()).includes(no.toLowerCase())) {
    errors.push('全宗号已存在，请沿用唯一编号体系')
  }
  if (!data.fondsName.trim()) errors.push('全宗名称不能为空')
  if (data.organizationId === undefined || data.organizationId === null) errors.push('请选择所属单位')
  return { valid: errors.length === 0, errors }
}
