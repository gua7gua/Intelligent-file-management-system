import type { PageData } from '@/types/api'
import type {
  BorrowApprovalDetail, BorrowApprovalParams, BorrowApproveData,
  BorrowCheckoutData, BorrowReturnData, BorrowVoucherResult,
} from '@/types/borrow-approval'

function mkReq(over: Partial<BorrowApprovalDetail>): BorrowApprovalDetail {
  return {
    id: 0, requestNo: '', archiveId: 0, archiveNo: '', archiveTitle: '', status: 'applied',
    expectedDays: 7, appliedAt: '2026-06-12T09:00:00+08:00', reason: '', contactPhone: '',
    borrowerName: '', archiveLocationCode: '', carrierStatus: 'paper_electronic',
    checkCarrier: '纸质+电子，可申请纸质借阅', checkLifecycle: '正常', checkLoan: '可借',
    checkInventory: '未命中进行中盘点', inventoryHit: false, ...over,
  }
}

/** 种子数据工厂：每次调用生成全新副本，保证测试间状态隔离 */
function seedRequests(): BorrowApprovalDetail[] {
  return [
    mkReq({ id: 1, requestNo: 'JY-2026-0017', archiveId: 96, archiveNo: 'KJ-2024-0096', archiveTitle: '智慧城市平台建设报告', status: 'applied', expectedDays: 7, expectedVisitAt: '2026-06-16T09:30:00+08:00', appliedAt: '2026-06-12T09:00:00+08:00', reason: '用于本单位智慧城市项目复盘，需核对纸质原件签章。', contactPhone: '13800000001', borrowerName: '小李', borrowerOrg: '技术部', archiveLocationCode: '401-03-02-05' }),
    mkReq({ id: 2, requestNo: 'JY-2026-0018', archiveId: 120, archiveNo: 'KJ-2024-0120', archiveTitle: '401 库房科技档案盘点范围内材料', status: 'applied', expectedDays: 5, expectedVisitAt: '2026-06-16T10:00:00+08:00', appliedAt: '2026-06-12T10:00:00+08:00', reason: '工程复盘查阅。', contactPhone: '13800000002', borrowerName: '小赵', borrowerOrg: '工程部', archiveLocationCode: '401-04-01-02', inventoryHit: true, checkInventory: '命中进行中盘点范围', checkLoan: '盘点暂停借阅' }),
    mkReq({ id: 3, requestNo: 'JY-2026-0013', archiveId: 320, archiveNo: 'KJ-2024-0320', archiveTitle: '工程建设验收材料', status: 'approved', expectedDays: 7, expectedVisitAt: '2026-06-15T09:00:00+08:00', appliedAt: '2026-06-11T09:00:00+08:00', approvedAt: '2026-06-12T14:00:00+08:00', opinion: '同意借阅 7 天', reason: '验收核对。', contactPhone: '13800000003', borrowerName: '王磊', borrowerOrg: '工程部', archiveLocationCode: '402-02-03-07' }),
    mkReq({ id: 4, requestNo: 'JY-2026-0010', archiveId: 55, archiveNo: 'KJ-2024-0055', archiveTitle: '年度规划文本', status: 'voucher_issued', expectedDays: 10, expectedVisitAt: '2026-06-14T09:00:00+08:00', appliedAt: '2026-06-10T09:00:00+08:00', approvedAt: '2026-06-11T10:00:00+08:00', opinion: '同意', voucherNo: 'VCH-2026-0010', voucherIssuedAt: '2026-06-13T09:00:00+08:00', reason: '规划研讨。', contactPhone: '13800000004', borrowerName: '周敏', borrowerOrg: '规划部', archiveLocationCode: '401-02-02-05' }),
    mkReq({ id: 5, requestNo: 'JY-2026-0009', archiveId: 8, archiveNo: 'KJ-2023-0008', archiveTitle: '设备维修记录汇编', status: 'checked_out', expectedDays: 7, expectedVisitAt: '2026-06-05T09:00:00+08:00', appliedAt: '2026-06-04T09:00:00+08:00', approvedAt: '2026-06-04T15:00:00+08:00', voucherNo: 'VCH-2026-0009', voucherIssuedAt: '2026-06-05T08:30:00+08:00', checkedOutAt: '2026-06-05T09:30:00+08:00', dueAt: '2026-06-12T09:30:00+08:00', overdue: true, reason: '设备维护参考。', contactPhone: '13800000005', borrowerName: '陈欣', borrowerOrg: '设备科', archiveLocationCode: '401-02-04-03' }),
    mkReq({ id: 6, requestNo: 'JY-2026-0007', archiveId: 40, archiveNo: 'KJ-2024-0040', archiveTitle: '2024 会计凭证汇编', status: 'checked_out', expectedDays: 5, appliedAt: '2026-06-10T09:00:00+08:00', approvedAt: '2026-06-10T14:00:00+08:00', voucherNo: 'VCH-2026-0007', checkedOutAt: '2026-06-11T09:00:00+08:00', dueAt: '2026-06-25T09:00:00+08:00', overdue: false, reason: '审计查阅。', contactPhone: '13800000006', borrowerName: '林涛', borrowerOrg: '财务部', archiveLocationCode: '402-01-01-02' }),
    mkReq({ id: 7, requestNo: 'JY-2026-0005', archiveId: 22, archiveNo: 'KJ-2023-0022', archiveTitle: '2023 人事任免卷', status: 'returned', expectedDays: 3, appliedAt: '2026-06-01T09:00:00+08:00', approvedAt: '2026-06-01T14:00:00+08:00', checkedOutAt: '2026-06-02T09:00:00+08:00', dueAt: '2026-06-05T09:00:00+08:00', returnedAt: '2026-06-05T08:50:00+08:00', returnCheckResult: 'normal', returnNote: '实体完好', reason: '人事核对。', contactPhone: '13800000007', borrowerName: '吴芳', borrowerOrg: '人事部', archiveLocationCode: '401-05-01-01' }),
    mkReq({ id: 8, requestNo: 'JY-2026-0003', archiveId: 15, archiveNo: 'KJ-2023-0015', archiveTitle: '老旧基建图纸', status: 'abnormal_return', expectedDays: 5, appliedAt: '2026-05-28T09:00:00+08:00', approvedAt: '2026-05-28T14:00:00+08:00', checkedOutAt: '2026-05-29T09:00:00+08:00', dueAt: '2026-06-03T09:00:00+08:00', returnedAt: '2026-06-03T10:00:00+08:00', returnCheckResult: 'damaged', returnNote: '边角破损', reason: '基建复查。', contactPhone: '13800000008', borrowerName: '郑刚', borrowerOrg: '基建部', archiveLocationCode: '401-06-02-03' }),
    mkReq({ id: 9, requestNo: 'JY-2026-0006', archiveId: 60, archiveNo: 'KJ-2024-0060', archiveTitle: '涉密技术报告', status: 'rejected', expectedDays: 7, appliedAt: '2026-06-08T09:00:00+08:00', approvedAt: '2026-06-09T10:00:00+08:00', rejectReason: '密级过高，不予纸质借阅，请走电子查阅。', reason: '技术评估。', contactPhone: '13800000009', borrowerName: '孙宇', borrowerOrg: '技术部', archiveLocationCode: '401-03-01-04', checkLoan: '密级限制' }),
  ]
}

let requests: BorrowApprovalDetail[] = seedRequests()

/** 测试钩子：重置 mock 到初始种子数据 */
export function __resetBorrowApprovalMock(): void {
  requests = seedRequests()
}

export function mockBorrowApprovals(params?: BorrowApprovalParams): PageData<BorrowApprovalDetail> {
  let list = requests.slice()
  if (params?.status) list = list.filter((r) => r.status === params.status)
  if (params?.borrowerKeyword) {
    const kw = params.borrowerKeyword
    list = list.filter((r) => r.borrowerName.includes(kw) || r.requestNo.includes(kw))
  }
  if (params?.archiveKeyword) {
    const kw = params.archiveKeyword
    list = list.filter((r) => r.archiveNo.includes(kw) || r.archiveTitle.includes(kw))
  }
  if (params?.overdue) list = list.filter((r) => r.overdue === true)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockBorrowApprovalDetail(id: number): BorrowApprovalDetail {
  const found = requests.find((r) => r.id === id)
  if (!found) throw new Error('借阅申请不存在')
  return JSON.parse(JSON.stringify(found))
}

export function mockApproveBorrowRequest(id: number, data: BorrowApproveData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'applied') throw new Error('当前状态不可审批')
  if (data.approved && req.inventoryHit) throw new Error('目标档案命中进行中盘点，不可审批通过')
  if (data.approved) {
    req.status = 'approved'
    req.approvedAt = '2026-06-15T11:00:00+08:00'
    req.opinion = data.opinion || '同意'
  } else {
    const reason = data.rejectReason?.trim() || data.opinion?.trim()
    if (!reason) throw new Error('审批拒绝必须填写原因')
    req.status = 'rejected'
    req.approvedAt = '2026-06-15T11:00:00+08:00'
    req.rejectReason = reason
  }
  return JSON.parse(JSON.stringify(req))
}

export function mockCheckoutBorrowRequest(id: number, data: BorrowCheckoutData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'approved' && req.status !== 'voucher_issued') throw new Error('仅已批准申请可出库')
  if (!data.voucherNo.trim()) throw new Error('凭证号必填')
  if (req.voucherNo && data.voucherNo !== req.voucherNo) throw new Error('凭证号与已导出凭证不匹配')
  req.status = 'checked_out'
  req.voucherNo = data.voucherNo
  req.checkedOutAt = '2026-06-15T14:00:00+08:00'
  req.dueAt = data.dueAt
  req.overdue = false
  return JSON.parse(JSON.stringify(req))
}

export function mockReturnBorrowRequest(id: number, data: BorrowReturnData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'checked_out') throw new Error('仅已出库申请可归还')
  if (data.returnCheckResult !== 'normal' && !data.returnNote?.trim()) throw new Error('异常归还必须填写检查说明')
  req.returnCheckResult = data.returnCheckResult
  req.returnNote = data.returnNote
  req.returnedAt = '2026-06-15T16:00:00+08:00'
  req.status = data.returnCheckResult === 'normal' ? 'returned' : 'abnormal_return'
  return JSON.parse(JSON.stringify(req))
}

let voucherSeq = 100
export function mockExportBorrowVoucher(id: number): BorrowVoucherResult {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'approved' && req.status !== 'voucher_issued') throw new Error('仅审批通过后可导出凭证')
  const firstIssued = !req.voucherNo
  if (firstIssued) {
    req.voucherNo = `VCH-2026-${String(voucherSeq++).padStart(4, '0')}`
    req.voucherIssuedAt = '2026-06-15T12:00:00+08:00'
    req.status = 'voucher_issued'
  }
  return { voucherNo: req.voucherNo!, voucherIssuedAt: req.voucherIssuedAt!, firstIssued }
}
