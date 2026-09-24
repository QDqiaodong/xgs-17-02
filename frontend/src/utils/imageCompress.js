import Compressor from 'compressorjs'

export function compressImage(file, options = {}) {
  return new Promise((resolve, reject) => {
    const {
      quality = 0.8,
      maxWidth = 1280,
      maxHeight = 1280,
      mimeType = 'image/jpeg'
    } = options

    new Compressor(file, {
      quality,
      maxWidth,
      maxHeight,
      mimeType,
      success(result) {
        resolve(result)
      },
      error(err) {
        reject(err)
      }
    })
  })
}

export async function compressAndUpload(file, uploadFn) {
  try {
    const compressed = await compressImage(file, {
      quality: 0.75,
      maxWidth: 1024,
      maxHeight: 1024
    })
    const res = await uploadFn(compressed)
    return res
  } catch (error) {
    return uploadFn(file)
  }
}
