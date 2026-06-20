import type { PageData } from '@/types/api'
import type {
  ArchiveBox, ArchiveBoxCreateData, ArchiveBoxDetail, ArchiveBoxMoveData, ArchiveBoxParams,
  BoxItem, LocationStatusData, StorageLocation, StorageLocationParams,
  WarehouseRoom, WarehouseRoomCreateData, WarehouseRoomParams, WarehouseRoomUpdateData,
} from '@/types/warehouse'

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/** 库房（对齐 WarehouseRoomResponse） */
const rooms: WarehouseRoom[] = [
  { id: 1, roomNo: '401', roomName: '综合档案库房', rackCount: 8, layersPerRack: 5, boxesPerLayer: 8, capacity: 320, warningThreshold: 0.85, status: 'active', occupiedSlots: 205, occupancyRate: 0.64, warning: false },
  { id: 2, roomNo: '402', roomName: '会计档案库房', rackCount: 6, layersPerRack: 4, boxesPerLayer: 8, capacity: 192, warningThreshold: 0.85, status: 'active', occupiedSlots: 169, occupancyRate: 0.88, warning: true },
  { id: 3, roomNo: '403', roomName: '科技档案库房', rackCount: 4, layersPerRack: 4, boxesPerLayer: 8, capacity: 128, warningThreshold: 0.85, status: 'active', occupiedSlots: 64, occupancyRate: 0.5, warning: false },
]

interface RoomSpec { roomId: number; roomNo: string; racks: number; layers: number; boxes: number }
const specs: RoomSpec[] = [
  { roomId: 1, roomNo: '401', racks: 2, layers: 5, boxes: 8 },
  { roomId: 2, roomNo: '402', racks: 1, layers: 4, boxes: 8 },
  { roomId: 3, roomNo: '403', racks: 1, layers: 4, boxes: 8 },
]

const disabledCodes = new Set(['401-01-01-08', '401-02-03-04', '402-01-02-06'])

function buildLocations(): StorageLocation[] {
  const out: StorageLocation[] = []
  let id = 1000
  for (const s of specs) {
    for (let r = 1; r <= s.racks; r++) {
      for (let l = 1; l <= s.layers; l++) {
        for (let b = 1; b <= s.boxes; b++) {
          const code = `${s.roomNo}-${pad(r)}-${pad(l)}-${pad(b)}`
          out.push({
            id: id++, roomId: s.roomId, rackNo: r, layerNo: l, boxSlotNo: b, locationCode: code,
            status: disabledCodes.has(code) ? 'disabled' : 'active', occupied: false,
          })
        }
      }
    }
  }
  return out
}

const locations: StorageLocation[] = buildLocations()
const locByCode = new Map(locations.map((l) => [l.locationCode, l]))

function mkItems(prefix: string, count: number): BoxItem[] {
  const items: BoxItem[] = []
  for (let i = 1; i <= count; i++) {
    items.push({
      archiveId: Number(`${prefix}${i}`), archiveNo: `KJ-2025-${pad(Number(prefix))}-${pad(i)}`,
      title: `${prefix} 第 ${i} 件档案`, sortNo: i, pageCount: 10 + i, physicalStatus: 'normal',
    })
  }
  return items
}

const boxes: ArchiveBoxDetail[] = [
  { id: 101, boxNo: 'BX-2026-001', locationId: 0, locationCode: '401-01-01-01', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '克拉玛依工业园扩建批复', capacity: 30, usedCount: 12, status: 'normal', items: mkItems('18', 12) },
  { id: 102, boxNo: 'BX-2026-002', locationId: 0, locationCode: '401-01-01-02', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '油田道路施工图', capacity: 30, usedCount: 8, status: 'normal', items: mkItems('19', 8) },
  { id: 103, boxNo: 'BX-2026-003', locationId: 0, locationCode: '401-01-01-04', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '年度项目审批材料', capacity: 30, usedCount: 28, status: 'full', items: mkItems('20', 28) },
  { id: 104, boxNo: 'BX-2024-016', locationId: 0, locationCode: '401-02-01-01', roomNo: '401', categoryId: 2, fondsId: 2, yearLabel: '2024', spineText: '干部任免材料', capacity: 30, usedCount: 6, status: 'normal', items: mkItems('21', 6) },
  { id: 105, boxNo: 'BX-2026-007', locationId: 0, locationCode: '401-02-01-03', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '工程竣工验收材料', capacity: 30, usedCount: 10, status: 'normal', items: mkItems('22', 10) },
  { id: 106, boxNo: 'BX-2024-032', locationId: 0, locationCode: '402-01-01-01', roomNo: '402', categoryId: 3, fondsId: 3, yearLabel: '2024', spineText: '会计凭证汇总', capacity: 30, usedCount: 30, status: 'full', items: mkItems('23', 30) },
  { id: 107, boxNo: 'BX-2025-019', locationId: 0, locationCode: '403-01-01-01', roomNo: '403', categoryId: 1, fondsId: 1, yearLabel: '2025', spineText: '管线改造竣工图', capacity: 30, usedCount: 6, status: 'normal', items: mkItems('24', 6) },
]

// 回填盒的 locationId 与架位占用
for (const bx of boxes) {
  const loc = locByCode.get(bx.locationCode)
  if (loc) {
    bx.locationId = loc.id
    loc.occupied = true
    loc.currentBoxId = bx.id
    loc.currentBoxNo = bx.boxNo
    loc.boxItemCount = bx.items.length
  }
}

function recomputeRoom(room: WarehouseRoom): void {
  const used = locations.filter((l) => l.roomId === room.id && l.occupied).length
  room.occupiedSlots = used
  room.occupancyRate = room.capacity > 0 ? used / room.capacity : 0
  room.warning = room.occupancyRate >= room.warningThreshold
}

export function mockWarehouseRooms(params?: WarehouseRoomParams): WarehouseRoom[] {
  let list = rooms.slice()
  if (params?.status) list = list.filter((r) => r.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((r) => r.roomNo.includes(kw) || r.roomName.includes(kw))
  }
  return list
}

let nextRoomId = 4
export function mockCreateWarehouseRoom(data: WarehouseRoomCreateData): WarehouseRoom {
  if (rooms.some((r) => r.roomNo === data.roomNo)) throw new Error('库房号已存在')
  const capacity = data.rackCount * data.layersPerRack * data.boxesPerLayer
  if (capacity <= 0) throw new Error('容量参数必须大于 0')
  const room: WarehouseRoom = {
    id: nextRoomId++, roomNo: data.roomNo, roomName: data.roomName, rackCount: data.rackCount,
    layersPerRack: data.layersPerRack, boxesPerLayer: data.boxesPerLayer, capacity,
    warningThreshold: data.warningThreshold, status: 'active', occupiedSlots: 0, occupancyRate: 0, warning: false,
  }
  rooms.push(room)
  let id = 9000 + locations.length
  for (let r = 1; r <= data.rackCount; r++) {
    for (let l = 1; l <= data.layersPerRack; l++) {
      for (let b = 1; b <= data.boxesPerLayer; b++) {
        const code = `${data.roomNo}-${pad(r)}-${pad(l)}-${pad(b)}`
        const loc: StorageLocation = { id: id++, roomId: room.id, rackNo: r, layerNo: l, boxSlotNo: b, locationCode: code, status: 'active', occupied: false }
        locations.push(loc)
        locByCode.set(code, loc)
      }
    }
  }
  return room
}

export function mockUpdateWarehouseRoom(id: number, data: WarehouseRoomUpdateData): WarehouseRoom {
  const room = rooms.find((r) => r.id === id)
  if (!room) throw new Error('库房不存在')
  if (data.roomName !== undefined) room.roomName = data.roomName
  if (data.warningThreshold !== undefined) {
    room.warningThreshold = data.warningThreshold
    recomputeRoom(room)
  }
  if (data.status !== undefined) room.status = data.status
  return room
}

export function mockDeleteWarehouseRoom(roomId: number): void {
  const idx = rooms.findIndex((r) => r.id === roomId)
  if (idx < 0) throw new Error('库房不存在')
  rooms.splice(idx, 1)
  // 一并清理该库房的架位（mock 端简化，不做活动盒校验）
  for (let i = locations.length - 1; i >= 0; i--) {
    if (locations[i].roomId === roomId) locations.splice(i, 1)
  }
}

export function mockStorageLocations(params?: StorageLocationParams): PageData<StorageLocation> {
  let list = locations.slice()
  if (params?.roomId) list = list.filter((l) => l.roomId === params.roomId)
  if (params?.rackNo) list = list.filter((l) => l.rackNo === params.rackNo)
  if (params?.status) list = list.filter((l) => l.status === params.status)
  if (params?.occupied !== undefined) list = list.filter((l) => l.occupied === params.occupied)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const start = (pageNo - 1) * pageSize
  return {
    records: list.slice(start, start + pageSize), pageNo, pageSize, total: list.length,
    hasNext: start + pageSize < list.length,
  }
}

export function mockUpdateLocationStatus(id: number, data: LocationStatusData): StorageLocation {
  const loc = locations.find((l) => l.id === id)
  if (!loc) throw new Error('架位不存在')
  if (data.status === 'disabled' && loc.occupied) throw new Error('已占用架位不得直接停用')
  loc.status = data.status
  return loc
}

export function mockArchiveBoxes(params?: ArchiveBoxParams): PageData<ArchiveBox> {
  let list = boxes.slice()
  if (params?.boxNo) list = list.filter((b) => b.boxNo.includes(params.boxNo!))
  if (params?.roomId) {
    const room = rooms.find((r) => r.id === params.roomId)
    if (room) list = list.filter((b) => b.roomNo === room.roomNo)
  }
  if (params?.categoryId) list = list.filter((b) => b.categoryId === params.categoryId)
  if (params?.fondsId) list = list.filter((b) => b.fondsId === params.fondsId)
  if (params?.status) list = list.filter((b) => b.status === params.status)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  const records = list.slice(start, start + pageSize).map(({ items, ...rest }) => rest)
  return { records, pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockArchiveBoxDetail(id: number): ArchiveBoxDetail {
  const found = boxes.find((b) => b.id === id)
  if (!found) throw new Error('档案盒不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextBoxId = 200
export function mockCreateArchiveBox(data: ArchiveBoxCreateData): ArchiveBox {
  const loc = locations.find((l) => l.id === data.locationId)
  if (!loc) throw new Error('架位不存在')
  if (loc.status !== 'active') throw new Error('架位已停用')
  if (loc.occupied) throw new Error('架位已被占用')
  const room = rooms.find((r) => r.id === loc.roomId)
  const id = nextBoxId++
  const boxNo = `BX-${data.yearLabel}-${String(id).padStart(3, '0')}`
  const box: ArchiveBoxDetail = {
    id, boxNo, locationId: loc.id, locationCode: loc.locationCode, roomNo: room?.roomNo ?? '',
    categoryId: data.categoryId, fondsId: data.fondsId, yearLabel: data.yearLabel, spineText: data.spineText,
    capacity: data.capacity, usedCount: 0, status: 'normal', items: [],
  }
  boxes.push(box)
  loc.occupied = true
  loc.currentBoxId = box.id
  loc.currentBoxNo = box.boxNo
  loc.boxItemCount = 0
  if (room) recomputeRoom(room)
  const { items, ...rest } = box
  return rest
}

export function mockMoveArchiveBox(id: number, data: ArchiveBoxMoveData): ArchiveBox {
  const box = boxes.find((b) => b.id === id)
  if (!box) throw new Error('档案盒不存在')
  const target = locations.find((l) => l.id === data.targetLocationId)
  if (!target) throw new Error('目标架位不存在')
  if (target.status !== 'active') throw new Error('目标架位已停用')
  if (target.occupied) throw new Error('目标架位已被占用')
  const source = locations.find((l) => l.id === box.locationId)
  const sourceRoom = source ? rooms.find((r) => r.id === source.roomId) : undefined
  if (source) {
    source.occupied = false
    source.currentBoxId = undefined
    source.currentBoxNo = undefined
    source.boxItemCount = undefined
  }
  box.locationId = target.id
  box.locationCode = target.locationCode
  box.roomNo = target.locationCode.split('-')[0]
  target.occupied = true
  target.currentBoxId = box.id
  target.currentBoxNo = box.boxNo
  target.boxItemCount = box.items.length
  if (sourceRoom) recomputeRoom(sourceRoom)
  const targetRoom = rooms.find((r) => r.id === target.roomId)
  if (targetRoom && targetRoom !== sourceRoom) recomputeRoom(targetRoom)
  const { items, ...rest } = box
  return rest
}
