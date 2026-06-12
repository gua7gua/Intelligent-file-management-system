import type { DictItem } from '@/types/components'

export const mockDictionaries: Record<string, DictItem[]> = {
  roles: [
    { value: 'front_archivist', label: '前台档案管理员' },
    { value: 'back_archivist', label: '后台档案管理员' },
    { value: 'transfer_user', label: '移交单位经办人' },
    { value: 'internal_reader', label: '内部查阅者' },
    { value: 'director', label: '馆领导' },
    { value: 'sys_admin', label: '系统管理员' },
    { value: 'public_user', label: '社会公众' },
  ],
  categories: [
    { value: 1, label: '文书档案' },
    { value: 2, label: '科技档案' },
    { value: 3, label: '会计档案' },
    { value: 4, label: '音像档案' },
    { value: 5, label: '实物档案' },
  ],
  carrierStatuses: [
    { value: 'electronic', label: '纯电子' },
    { value: 'paper_electronic', label: '纸质+电子' },
    { value: 'paper', label: '纯纸质' },
  ],
  retentionPeriods: [
    { value: '10y', label: '10年' },
    { value: '30y', label: '30年' },
    { value: 'permanent', label: '永久' },
  ],
  securityLevels: [
    { value: 0, label: '非密' },
    { value: 1, label: '内部' },
    { value: 2, label: '秘密' },
    { value: 3, label: '机密' },
    { value: 4, label: '绝密' },
  ],
  openStatuses: [
    { value: 'open', label: '公开' },
    { value: 'closed', label: '不公开' },
  ],
  batchStatuses: [
    { value: 'draft', label: '草稿' },
    { value: 'pending_transfer', label: '待移交' },
    { value: 'pending_contact', label: '待联系' },
    { value: 'pending_receive', label: '待接收' },
    { value: 'received', label: '已接收' },
    { value: 'partially_received', label: '部分接收' },
    { value: 'rejected', label: '已回退' },
    { value: 'archived', label: '已入库' },
    { value: 'shelved', label: '已上架' },
  ],
  itemStatuses: [
    { value: 'draft', label: '草稿' },
    { value: 'pending_acceptance', label: '待验收' },
    { value: 'accepted', label: '已接收' },
    { value: 'rejected', label: '已回退' },
    { value: 'pending_archive', label: '待入库' },
    { value: 'archived', label: '已入库' },
  ],
}
