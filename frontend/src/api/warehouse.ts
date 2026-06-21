import request from './request'
import type { PageData } from '@/types/api'
import type {
  ArchiveBox, ArchiveBoxCreateData, ArchiveBoxDetail, ArchiveBoxMoveData, ArchiveBoxParams,
  LocationStatusData, StorageLocation, StorageLocationParams,
  WarehouseRoom, WarehouseRoomCreateData, WarehouseRoomParams, WarehouseRoomUpdateData,
} from '@/types/warehouse'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询库房列表（§16.1） */
export function getWarehouseRooms(params?: WarehouseRoomParams): Promise<WarehouseRoom[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockWarehouseRooms(params))
  }
  return request.get('/admin/warehouse/rooms', { params })
}

/** 新增库房（§16.2） */
export function createWarehouseRoom(data: WarehouseRoomCreateData): Promise<WarehouseRoom> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockCreateWarehouseRoom(data))
  }
  return request.post('/admin/warehouse/rooms', data)
}

/** 更新库房（§16.3） */
export function updateWarehouseRoom(roomId: number, data: WarehouseRoomUpdateData): Promise<WarehouseRoom> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockUpdateWarehouseRoom(roomId, data))
  }
  return request.put(`/admin/warehouse/rooms/${roomId}`, data)
}

/** 删除库房（§16.10，无活动档案盒时允许） */
export function deleteWarehouseRoom(roomId: number): Promise<void> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockDeleteWarehouseRoom(roomId))
  }
  return request.delete(`/admin/warehouse/rooms/${roomId}`)
}

/** 查询架位（§16.4） */
export function getStorageLocations(params?: StorageLocationParams): Promise<PageData<StorageLocation>> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockStorageLocations(params))
  }
  return request.get('/admin/warehouse/locations', { params })
}

/** 停用/启用架位（§16.5） */
export function updateLocationStatus(locationId: number, data: LocationStatusData): Promise<StorageLocation> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockUpdateLocationStatus(locationId, data))
  }
  return request.put(`/admin/warehouse/locations/${locationId}/status`, data)
}

/** 查询档案盒（§16.6） */
export function getArchiveBoxes(params?: ArchiveBoxParams): Promise<PageData<ArchiveBox>> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockArchiveBoxes(params))
  }
  return request.get('/admin/warehouse/boxes', { params })
}

/** 档案盒详情（§16.7） */
export function getArchiveBoxDetail(boxId: number): Promise<ArchiveBoxDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockArchiveBoxDetail(boxId))
  }
  return request.get(`/admin/warehouse/boxes/${boxId}`)
}

/** 新增档案盒（§16.8） */
export function createArchiveBox(data: ArchiveBoxCreateData): Promise<ArchiveBox> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockCreateArchiveBox(data))
  }
  return request.post('/admin/warehouse/boxes', data)
}

/** 移动档案盒（§16.9） */
export function moveArchiveBox(boxId: number, data: ArchiveBoxMoveData): Promise<ArchiveBox> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockMoveArchiveBox(boxId, data))
  }
  return request.post(`/admin/warehouse/boxes/${boxId}/move`, data)
}

/** 删除空档案盒，释放所在架位（§16.10） */
export function deleteArchiveBox(boxId: number): Promise<void> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockDeleteArchiveBox(boxId))
  }
  return request.delete(`/admin/warehouse/boxes/${boxId}`)
}
