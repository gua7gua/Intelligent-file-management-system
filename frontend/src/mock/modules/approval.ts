// src/mock/modules/approval.ts
import type {
  ApprovalDestructionListSummary,
  ApprovalOpinionData,
  ApprovalParams,
  ApprovalRequest,
  ApprovalRequestDetail,
} from '@/types/approval'
import type { DestructionItem } from '@/types/destruction'

const destructionItems: DestructionItem[] = [
  {
    id: 9001,
    archiveId: 301,
    archiveNoSnapshot: 'ARC-000301',
    titleSnapshot: '2014 年基建档案',
    categorySnapshot: '基建档案',
    pageCountSnapshot: 120,
    retentionSnapshot: '10y',
    securityLevelSnapshot: 0,
    appraisalOpinionSnapshot: '到期无保存价值',
    fileDeleteStatus: 'not_started',
  },
]

const approvals: ApprovalRequestDetail[] = [
  {
    id: 20,
    approvalType: 'security_adjust',
    targetType: 'archive',
    targetId: 101,
    evidenceArchiveId: 102,
    oldValue: '0',
    newValue: '2',
    reason: '凭证档案为机密，目标档案应同步调整',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T10:00:00+08:00',
    targetArchiveNo: 'ARC-000101',
    targetArchiveTitle: '2024 年城市规划文本',
    targetArchive: {
      id: 101,
      archiveNo: 'ARC-000101',
      title: '2024 年城市规划文本',
      categoryName: '文书档案',
      organizationName: '克拉玛依市自然资源局',
      fondsName: '自然资源局全宗',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    evidenceArchive: {
      id: 102,
      archiveNo: 'ARC-000102',
      title: '涉密区域规划底图',
      categoryName: '文书档案',
      organizationName: '克拉玛依市自然资源局',
      fondsName: '自然资源局全宗',
      securityLevel: 2,
      lifecycleStatus: 'normal',
    },
    evidenceMatched: true,
  },
  {
    id: 21,
    approvalType: 'open_adjust',
    targetType: 'archive',
    targetId: 103,
    evidenceArchiveId: 104,
    oldValue: 'open',
    newValue: 'closed',
    reason: '公开范围复核，调整为不公开',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T10:30:00+08:00',
    targetArchiveNo: 'ARC-000103',
    targetArchiveTitle: '2023 年信访处理记录',
    targetArchive: {
      id: 103,
      archiveNo: 'ARC-000103',
      title: '2023 年信访处理记录',
      categoryName: '文书档案',
      organizationName: '克拉玛依市信访局',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    evidenceArchive: {
      id: 104,
      archiveNo: 'ARC-000104',
      title: '其他单位人事档案',
      categoryName: '人事档案',
      organizationName: '克拉玛依市人社局',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    evidenceMatched: false,
  },
  {
    id: 22,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: 11,
    reason: '到期鉴定后按制度提交销毁',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T11:00:00+08:00',
    targetListNo: 'DES-011',
    targetListName: '2014 年基建档案销毁清册',
    destructionList: {
      id: 11,
      listNo: 'DES-011',
      listName: '2014 年基建档案销毁清册',
      itemCount: 1,
      appraisalBatchNo: 'APP-002',
      items: destructionItems,
    },
  },
  {
    id: 23,
    approvalType: 'security_adjust',
    targetType: 'archive',
    targetId: 105,
    oldValue: '1',
    newValue: '0',
    reason: '密级下调',
    status: 'approved',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-14T09:00:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-14T15:00:00+08:00',
    approvalOpinion: '同意下调',
    targetArchiveNo: 'ARC-000105',
    targetArchiveTitle: '2022 年会议纪要',
  },
  {
    id: 24,
    approvalType: 'open_adjust',
    targetType: 'archive',
    targetId: 106,
    oldValue: 'closed',
    newValue: 'open',
    reason: '申请公开',
    status: 'rejected',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-14T09:30:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-14T15:30:00+08:00',
    approvalOpinion: '依据不足，退回补充说明',
    targetArchiveNo: 'ARC-000106',
    targetArchiveTitle: '2021 年财务决算',
  },
  {
    id: 25,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: 10,
    reason: '到期销毁',
    status: 'approved',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-13T11:00:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-13T16:00:00+08:00',
    approvalOpinion: '同意销毁',
    targetListNo: 'DES-010',
    targetListName: '2014 年设备档案销毁清册',
  },
]

export function mockApprovals(params?: ApprovalParams): {
  records: ApprovalRequest[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = approvals.slice()
  if (params?.approvalType) list = list.filter((a) => a.approvalType === params.approvalType)
  if (params?.status) list = list.filter((a) => a.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter(
      (a) =>
        a.targetArchiveTitle?.includes(kw) ||
        a.targetListName?.includes(kw) ||
        a.reason.includes(kw),
    )
  }
  return {
    records: list.map(({ targetArchive, evidenceArchive, destructionList, evidenceMatched, ...rest }) => rest),
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: list.length,
    hasNext: false,
  }
}

export function mockApprovalDetail(id: number): ApprovalRequestDetail {
  const found = approvals.find((a) => a.id === id)
  if (!found) throw new Error('审批单不存在')
  return JSON.parse(JSON.stringify(found))
}

export function mockApproveApproval(id: number, data: ApprovalOpinionData): ApprovalRequestDetail {
  const detail = mockApprovalDetail(id)
  detail.status = 'approved'
  detail.approvalOpinion = data.opinion
  detail.approvedAt = '2026-06-15T18:00:00+08:00'
  return detail
}

export function mockRejectApproval(id: number, data: ApprovalOpinionData): ApprovalRequestDetail {
  const detail = mockApprovalDetail(id)
  detail.status = 'rejected'
  detail.approvalOpinion = data.opinion
  detail.approvedAt = '2026-06-15T18:00:00+08:00'
  return detail
}

/** 销毁审批通过时联动更新清册状态（供 destruction mock 引用） */
export function resolveDestructionListSummary(id: number): ApprovalDestructionListSummary | undefined {
  const a = approvals.find((x) => x.approvalType === 'destruction' && x.targetId === id)
  return a?.destructionList
}
