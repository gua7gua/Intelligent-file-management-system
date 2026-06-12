interface TransferDraftItem {
  inputTitle: string
  retentionPeriod: string
  carrierStatus: string
  securityLevel: number
  openStatus: string
  allowDigitization: boolean
}

interface TransferDraft {
  title: string
  departmentName: string
  contactPhone: string
  archiveYear?: number
  expectedTransferDate: string
  items: TransferDraftItem[]
}

export function validateTransferDraft(draft: TransferDraft): string[] {
  const errors: string[] = []

  if (!draft.title.trim()) errors.push('请填写清单标题。')
  if (!draft.departmentName.trim()) errors.push('请填写移交部门。')
  if (!draft.contactPhone.trim()) errors.push('请填写联系电话。')
  if (!draft.archiveYear) errors.push('请选择档案所属年度。')
  if (!draft.expectedTransferDate.trim()) errors.push('请选择预计移交日期。')
  if (draft.items.length === 0) errors.push('请至少添加一条清单条目。')

  draft.items.forEach((item, index) => {
    const row = index + 1
    if (!item.inputTitle.trim()) errors.push(`第 ${row} 条请填写档案标题。`)
    if (!item.retentionPeriod) errors.push(`第 ${row} 条请选择保管期限。`)
    if (!item.carrierStatus) errors.push(`第 ${row} 条请选择载体状态。`)
    if (item.securityLevel > 0 && item.openStatus === 'open') {
      errors.push(`第 ${row} 条涉密档案不能设置为公开。`)
    }
  })

  return errors
}
