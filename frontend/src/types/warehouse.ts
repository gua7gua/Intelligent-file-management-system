import type { PageData, PageParams } from './api'
import type { BoxStatusValue, LocationStatusValue, WarehouseRoomStatusValue } from './enums'

/** 库房列表查询参数（§16.1） */
export interface WarehouseRoomParams {
  status?: WarehouseRoomStatusValue | ''
  keyword?: string
}

/** 库房（对齐 WarehouseRoomResponse） */
export interface WarehouseRoom {
  id: number
  roomNo: string
  roomName: string
  rackCount: number
  layersPerRack: number
  boxesPerLayer: number
  /** 总盒位 = rackCount * layersPerRack * boxesPerLayer */
  capacity: number
  /** 告警阈值，0~1，默认 0.85 */
  warningThreshold: number
  status: WarehouseRoomStatusValue
  /** 已占用盒位 */
  occupiedSlots: number
  /** 占用率，0~1 */
  occupancyRate: number
  /** 是否达到告警阈值 */
  warning: boolean
}

/** 新增库房请求（§16.2 / WarehouseRoomCreateRequest） */
export interface WarehouseRoomCreateData {
  roomNo: string
  roomName: string
  rackCount: number
  layersPerRack: number
  boxesPerLayer: number
  warningThreshold: number
}

/** 更新库房请求（§16.3 / WarehouseRoomUpdateRequest） */
export interface WarehouseRoomUpdateData {
  roomName?: string
  warningThreshold?: number
  status?: WarehouseRoomStatusValue
}

/** 架位查询参数（§16.4） */
export interface StorageLocationParams extends PageParams {
  roomId?: number
  rackNo?: number
  status?: LocationStatusValue | ''
  occupied?: boolean
}

/** 架位（对齐 StorageLocationResponse） */
export interface StorageLocation {
  id: number
  roomId: number
  rackNo: number
  layerNo: number
  boxSlotNo: number
  /** 如 401-03-02-05 */
  locationCode: string
  status: LocationStatusValue
  /** 是否被档案盒占用 */
  occupied: boolean
  currentBoxId?: number
  currentBoxNo?: string
  /** 当前盒内件数 */
  boxItemCount?: number
}

/** 架位状态变更请求（§16.5 / LocationStatusRequest） */
export interface LocationStatusData {
  status: LocationStatusValue
  reason?: string
}

/** 档案盒查询参数（§16.6） */
export interface ArchiveBoxParams extends PageParams {
  boxNo?: string
  roomId?: number
  categoryId?: number
  fondsId?: number
  status?: BoxStatusValue | ''
}

/** 档案盒列表项（对齐 ArchiveBoxResponse） */
export interface ArchiveBox {
  id: number
  boxNo: string
  locationId: number
  locationCode: string
  roomNo: string
  categoryId: number
  fondsId: number
  yearLabel: string
  spineText: string
  capacity: number
  usedCount: number
  status: BoxStatusValue
}

/** 盒内档案条目（ArchiveBoxDetailResponse.BoxItemView） */
export interface BoxItem {
  archiveId: number
  archiveNo: string
  title: string
  sortNo: number
  pageCount: number
  /** normal / damaged / lost */
  physicalStatus: string
}

/** 档案盒详情（§16.7 / ArchiveBoxDetailResponse） */
export interface ArchiveBoxDetail extends ArchiveBox {
  items: BoxItem[]
}

/** 新增档案盒请求（§16.8 / ArchiveBoxCreateRequest） */
export interface ArchiveBoxCreateData {
  locationId: number
  categoryId: number
  capacity: number
}

/** 移动档案盒请求（§16.9 / ArchiveBoxMoveRequest） */
export interface ArchiveBoxMoveData {
  targetLocationId: number
  reason: string
}

export type StorageLocationPage = PageData<StorageLocation>
export type ArchiveBoxPage = PageData<ArchiveBox>
