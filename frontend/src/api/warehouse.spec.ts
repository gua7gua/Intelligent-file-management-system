import { describe, expect, it } from 'vitest'
import {
  createArchiveBox, createWarehouseRoom, getArchiveBoxDetail,
  getStorageLocations, getWarehouseRooms, moveArchiveBox, updateLocationStatus,
} from './warehouse'

describe('warehouse api mock mode', () => {
  it('lists rooms and flags warning room', async () => {
    const rooms = await getWarehouseRooms()
    expect(rooms.length).toBeGreaterThanOrEqual(3)
    const warned = rooms.find((r) => r.warning)
    expect(warned).toBeTruthy()
    expect(warned?.occupancyRate).toBeGreaterThanOrEqual(warned!.warningThreshold)
  })

  it('filters locations by room and occupied', async () => {
    const all = await getStorageLocations({ roomId: 1, pageSize: 200 })
    expect(all.records.every((l) => l.roomId === 1)).toBe(true)
    const occupied = await getStorageLocations({ roomId: 1, occupied: true, pageSize: 200 })
    expect(occupied.records.every((l) => l.occupied)).toBe(true)
  })

  it('box detail includes items', async () => {
    const detail = await getArchiveBoxDetail(101)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items[0].archiveNo).toBeTruthy()
  })

  it('create box occupies a free location', async () => {
    const free = (await getStorageLocations({ roomId: 3, occupied: false, pageSize: 200 })).records[0]
    const box = await createArchiveBox({
      locationId: free.id, categoryId: 1, capacity: 30,
    })
    expect(box.locationId).toBe(free.id)
    const after = await getStorageLocations({ roomId: 3, occupied: true, pageSize: 200 })
    expect(after.records.some((l) => l.id === free.id)).toBe(true)
  })

  it('rejects disabling an occupied location', async () => {
    const occupied = (await getStorageLocations({ occupied: true, pageSize: 200 })).records[0]
    await expect(updateLocationStatus(occupied.id, { status: 'disabled', reason: '维修' })).rejects.toThrow()
  })

  it('move box frees source and occupies target', async () => {
    const box = await getArchiveBoxDetail(101)
    const target = (await getStorageLocations({ roomId: 3, occupied: false, pageSize: 200 })).records[0]
    const moved = await moveArchiveBox(box.id, { targetLocationId: target.id, reason: '整理' })
    expect(moved.locationId).toBe(target.id)
  })

  it('create room generates locations by spec', async () => {
    const room = await createWarehouseRoom({
      roomNo: '404', roomName: '测试库房', rackCount: 2, layersPerRack: 2, boxesPerLayer: 2, warningThreshold: 0.85,
    })
    expect(room.capacity).toBe(8)
    const locs = await getStorageLocations({ roomId: room.id, pageSize: 200 })
    expect(locs.total).toBe(8)
  })
})
