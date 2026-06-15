function escapeCell(v: unknown): string {
  if (v === null || v === undefined) return ''
  const s = typeof v === 'object' ? JSON.stringify(v) : String(v)
  if (/[",\n\r]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
  return s
}

/** 将记录数组转为 CSV 字符串（首行表头取自 columns 或首条记录的键） */
export function toCsv(rows: Record<string, unknown>[], columns?: string[]): string {
  if (rows.length === 0) return ''
  const cols = columns ?? Object.keys(rows[0])
  const header = cols.map(escapeCell).join(',')
  const body = rows.map((r) => cols.map((c) => escapeCell(r[c])).join(','))
  return [header, ...body].join('\n')
}

/** 触发浏览器下载 CSV（含 BOM，Excel 中文不乱码） */
export function downloadCsv(filename: string, rows: Record<string, unknown>[], columns?: string[]): void {
  const csv = toCsv(rows, columns)
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
