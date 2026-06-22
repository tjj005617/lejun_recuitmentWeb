import request from './request'

/**
 * 语音识别
 * @param {Blob} audioBlob WAV 格式的音频 Blob
 * @returns {Promise<string>} 识别出的文字
 */
export function recognizeSpeech(audioBlob) {
  const formData = new FormData()
  formData.append('file', audioBlob, 'recording.wav')
  return request.post('/stt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
