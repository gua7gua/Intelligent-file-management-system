import { describe, expect, it } from 'vitest'
import { createBackupTask, getBackupTasks, getFileCheckRecords, triggerFileCheck } from './preservation'

describe('preservation api mock mode', () => {
  it('lists backup tasks and filters by scope', async () => {
    const all = await getBackupTasks()
    expect(all.records.length).toBeGreaterThan(0)
    const db = await getBackupTasks({ backupScope: 'database' })
    expect(db.records.every((t) => t.backupScope === 'database')).toBe(true)
  })

  it('includes a failed task with message', async () => {
    const all = await getBackupTasks()
    const failed = all.records.find((t) => t.status === 'failed')
    expect(failed).toBeTruthy()
    expect(failed!.message).toBeTruthy()
  })

  it('creates a running backup task', async () => {
    const task = await createBackupTask({ backupScope: 'both' })
    expect(task.status).toBe('running')
    expect(task.taskNo).toMatch(/^BAK-/)
  })

  it('lists file check records with not_configured authenticity', async () => {
    const recs = await getFileCheckRecords()
    expect(recs.records.length).toBeGreaterThan(0)
    const auth = recs.records.find((r) => r.checkType === 'authenticity')
    expect(auth?.checkResult).toBe('not_configured')
  })

  it('trigger check returns records for requested types', async () => {
    const created = await triggerFileCheck(5001, { checkTypes: ['integrity', 'authenticity'] })
    expect(created).toHaveLength(2)
    expect(created.map((r) => r.checkType).sort()).toEqual(['authenticity', 'integrity'])
  })
})
