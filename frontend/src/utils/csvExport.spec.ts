import { describe, expect, it } from 'vitest'
import { toCsv } from './csvExport'

describe('toCsv', () => {
  it('builds header and rows from object keys', () => {
    expect(toCsv([{ a: 1, b: 2 }])).toBe('a,b\n1,2')
  })

  it('escapes commas, quotes and newlines', () => {
    const csv = toCsv([{ a: 'x,y', b: 'he said "hi"', c: 'line1\nline2' }], ['a', 'b', 'c'])
    expect(csv).toContain('"x,y"')
    expect(csv).toContain('"he said ""hi"""')
    expect(csv).toContain('"line1\nline2"')
  })

  it('stringifies objects as JSON and CSV-escapes quotes', () => {
    const csv = toCsv([{ d: { k: 1 } }], ['d'])
    // JSON 含双引号，按 RFC 4180 整体加引号并将内部 " 转义为 ""
    expect(csv).toContain('"{""k"":1}"')
  })

  it('returns empty string for no rows', () => {
    expect(toCsv([])).toBe('')
  })
})
