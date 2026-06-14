// src/mock/modules/destruction.ts
import type {
  DestructionConfirmData,
  DestructionList,
  DestructionListDetail,
  DestructionListParams,
  DestructionPhoto,
} from '@/types/destruction'
import type { ApprovalRequest } from '@/types/approval'
import { mockApprovalDetail, resolveDestructionListSummary } from './approval'

// 清册 10：待销毁（已审批通过，approval 25）
const list10Items = [
  {
    id: 8001,
    archiveId: 302,
    archiveNoSnapshot: 'ARC-000302',
    titleSnapshot: '2014 年设备档案',
    categorySnapshot: '设备档案',
    pageCountSnapshot: 80,
    retentionSnapshot: '10y',
    securityLevelSnapshot: 0,
    appraisalOpinionSnapshot: '到期无保存价值',
    fileDeleteStatus: 'not_started' as const,
  },
]

const lists: DestructionListDetail[] = [
  {
    id: 10,
    listNo: 'DES-010',
    listName: '2014 年设备档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'pending_destroy',
    approvalRequestId: 25,
    itemCount: 1,
    createdAt: '2026-06-13T12:00:00+08:00',
    items: list10Items,
    approval: mockApprovalDetail(25) as ApprovalRequest,
    photos: [
      { id: 7001, fileName: 'scene-1.jpg', fileSize: 204800, uploadedAt: '2026-06-15T16:00:00+08:00' },
    ],
  },
  {
    id: 11,
    listNo: 'DES-011',
    listName: '2014 年基建档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'pending_approval',
    approvalRequestId: 22,
    itemCount: 1,
    createdAt: '2026-06-15T11:00:00+08:00',
    items: resolveDestructionListSummary(11)!.items,
    approval: mockApprovalDetail(22) as ApprovalRequest,
    photos: [],
  },
  {
    id: 12,
    listNo: 'DES-012',
    listName: '2013 年人事档案销毁清册',
    appraisalBatchId: 3,
    appraisalBatchNo: 'APP-003',
    status: 'destroyed',
    destroyedAt: '2026-06-12T17:00:00+08:00',
    destroyMethod: 'shredding',
    supervisorName1: '刘星',
    supervisorName2: '向加明',
    destroyNote: '现场粉碎销毁，照片已上传',
    itemCount: 1,
    createdAt: '2026-06-11T10:00:00+08:00',
    items: [
      {
        id: 8101,
        archiveId: 401,
        archiveNoSnapshot: 'ARC-000401',
        titleSnapshot: '2013 年人事档案',
        categorySnapshot: '人事档案',
        pageCountSnapshot: 60,
        retentionSnapshot: '10y',
        securityLevelSnapshot: 1,
        appraisalOpinionSnapshot: '到期销毁',
        fileDeleteStatus: 'deleted',
        fileDeletedAt: '2026-06-12T17:00:00+08:00',
      },
    ],
    approval: undefined,
    photos: [
      { id: 7101, fileName: 'destroyed-1.jpg', fileSize: 256000, uploadedAt: '2026-06-12T16:30:00+08:00' },
    ],
  },
  {
    id: 13,
    listNo: 'DES-013',
    listName: '2014 年基建与设备档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'draft',
    itemCount: 1,
    createdAt: '2026-06-14T16:00:00+08:00',
    items: list10Items,
    approval: undefined,
    photos: [],
  },
]

export function mockDestructionLists(params?: DestructionListParams): {
  records: DestructionList[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = lists.slice()
  if (params?.status) list = list.filter((l) => l.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((l) => l.listName.includes(kw) || l.listNo.includes(kw))
  }
  return {
    records: list.map(({ items, approval, photos, ...rest }) => rest),
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: list.length,
    hasNext: false,
  }
}

export function mockDestructionListDetail(id: number): DestructionListDetail {
  const found = lists.find((l) => l.id === id)
  if (!found) throw new Error('销毁清册不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextApprovalId = 26
export function mockSubmitDestructionApproval(
  id: number,
  data?: { reason?: string },
): ApprovalRequest {
  const detail = mockDestructionListDetail(id)
  detail.status = 'pending_approval'
  detail.approvalRequestId = nextApprovalId
  const approval: ApprovalRequest = {
    id: nextApprovalId,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: id,
    reason: data?.reason?.trim() || '到期鉴定后按制度提交销毁',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '胡颖',
    submittedAt: '2026-06-15T18:00:00+08:00',
    targetListNo: detail.listNo,
    targetListName: detail.listName,
  }
  nextApprovalId++
  return approval
}

let nextPhotoId = 8000
export function mockUploadDestructionPhotos(id: number, files: File[]): DestructionPhoto[] {
  const detail = mockDestructionListDetail(id)
  const photos: DestructionPhoto[] = files.map((f) => ({
    id: nextPhotoId++,
    fileName: f.name,
    fileSize: f.size,
    uploadedAt: '2026-06-15T18:30:00+08:00',
  }))
  detail.photos.push(...photos)
  return photos
}

export function mockConfirmDestruction(id: number, data: DestructionConfirmData): DestructionListDetail {
  const detail = mockDestructionListDetail(id)
  detail.status = 'destroyed'
  detail.destroyMethod = data.destroyMethod
  detail.supervisorName1 = data.supervisorName1
  detail.supervisorName2 = data.supervisorName2
  detail.destroyNote = data.destroyNote
  detail.destroyedAt = '2026-06-15T19:00:00+08:00'
  detail.items = detail.items.map((it) => ({
    ...it,
    fileDeleteStatus: 'deleted' as const,
    fileDeletedAt: '2026-06-15T19:00:00+08:00',
  }))
  return detail
}
