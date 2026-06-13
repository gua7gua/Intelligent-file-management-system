export interface ParsedLocalFile {
  inputTitle: string
  expectedFilename: string
  electronicFormat: string
  localFileSize: number
}

export function parseLocalFiles(files: File[]): ParsedLocalFile[] {
  return files.map((file) => {
    const lastDot = file.name.lastIndexOf('.')
    const title = lastDot > 0 ? file.name.slice(0, lastDot) : file.name
    const extension = lastDot > 0 ? file.name.slice(lastDot + 1).toUpperCase() : ''

    return {
      inputTitle: title,
      expectedFilename: file.name,
      electronicFormat: extension,
      localFileSize: file.size,
    }
  })
}
