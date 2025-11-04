/**
 * 图片压缩工具
 */

export interface CompressOptions {
  // 目标质量 (0-1)，默认 0.8
  quality?: number
  // 最大宽度，默认 1920
  maxWidth?: number
  // 最大高度，默认 1920
  maxHeight?: number
  // 输出格式，默认 'image/jpeg'
  mimeType?: string
}

/**
 * 压缩图片
 * @param file 原始文件
 * @param options 压缩选项
 * @returns 压缩后的 Blob
 */
export function compressImage(file: File, options: CompressOptions = {}): Promise<Blob> {
  const {
    quality = 0.8,
    maxWidth = 1920,
    maxHeight = 1920,
    mimeType = 'image/jpeg'
  } = options

  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (e) => {
      const img = new Image()

      img.onload = () => {
        // 计算缩放比例
        let width = img.width
        let height = img.height

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = width * ratio
          height = height * ratio
        }

        // 创建 Canvas
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('无法获取 Canvas 上下文'))
          return
        }

        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height)

        // 转换为 Blob
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob)
            } else {
              reject(new Error('图片压缩失败'))
            }
          },
          mimeType,
          quality
        )
      }

      img.onerror = () => {
        reject(new Error('图片加载失败'))
      }

      img.src = e.target?.result as string
    }

    reader.onerror = () => {
      reject(new Error('文件读取失败'))
    }

    reader.readAsDataURL(file)
  })
}

/**
 * Blob 转 File
 */
export function blobToFile(blob: Blob, fileName: string): File {
  return new File([blob], fileName, { type: blob.type })
}
