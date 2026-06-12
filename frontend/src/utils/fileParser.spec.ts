import { describe, expect, it } from 'vitest'
import { parseLocalFiles } from './fileParser'

describe('parseLocalFiles', () => {
  it('extracts filename, title, extension, and size without uploading file content', () => {
    const files = [
      new File(['demo'], '2025Q1-accounting-vouchers.pdf', { type: 'application/pdf' }),
      new File(['photo'], 'family.photos.1998.JPG', { type: 'image/jpeg' }),
    ]

    const result = parseLocalFiles(files)

    expect(result).toEqual([
      {
        inputTitle: '2025Q1-accounting-vouchers',
        expectedFilename: '2025Q1-accounting-vouchers.pdf',
        electronicFormat: 'PDF',
        localFileSize: 4,
      },
      {
        inputTitle: 'family.photos.1998',
        expectedFilename: 'family.photos.1998.JPG',
        electronicFormat: 'JPG',
        localFileSize: 5,
      },
    ])
  })
})
