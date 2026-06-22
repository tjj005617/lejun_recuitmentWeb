/**
 * Web Speech API 封装
 * 用于实时语音识别（字幕显示），内置 VAD 语音活动检测
 * 浏览器支持：Chrome/Edge（推荐），Safari 部分支持，Firefox 不支持
 */

/**
 * 检测浏览器是否支持 Web Speech API
 * @returns {boolean}
 */
export function isSpeechRecognitionSupported() {
  return !!(window.SpeechRecognition || window.webkitSpeechRecognition)
}

/**
 * 创建语音识别器
 * @param {Object} options
 * @param {string} [options.lang='zh-CN'] - 识别语言
 * @param {boolean} [options.continuous=true] - 是否连续识别
 * @param {boolean} [options.interimResults=true] - 是否返回中间结果
 * @param {function(string)} [options.onInterim] - 中间结果回调（实时字幕）
 * @param {function(string)} [options.onFinal] - 最终结果回调
 * @param {function()} [options.onEnd] - 识别自然结束回调（用户停止说话后触发）
 * @param {function(string)} [options.onError] - 错误回调
 * @returns {{ start: () => void, stop: () => void, abort: () => void, isSupported: boolean }}
 */
export function createSpeechRecognition(options = {}) {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

  if (!SpeechRecognition) {
    return { start() {}, stop() {}, abort() {}, isSupported: false }
  }

  const recognition = new SpeechRecognition()
  recognition.lang = options.lang || 'zh-CN'
  recognition.continuous = options.continuous !== false
  recognition.interimResults = options.interimResults !== false
  recognition.maxAlternatives = 1

  // 是否主动调用了 stop/abort
  let manualStop = false

  recognition.onresult = (event) => {
    let interimText = ''
    let finalText = ''

    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        finalText += transcript
      } else {
        interimText += transcript
      }
    }

    // 中间结果：实时字幕
    if (interimText && options.onInterim) {
      options.onInterim(interimText)
    }

    // 最终结果
    if (finalText && options.onFinal) {
      options.onFinal(finalText)
    }
  }

  recognition.onerror = (event) => {
    // 'no-speech' 和 'aborted' 不算真正的错误，静默处理
    if (event.error === 'no-speech' || event.error === 'aborted') {
      console.log('[SpeechRecognition] 非致命错误:', event.error)
      return
    }
    console.error('[SpeechRecognition] 错误:', event.error)
    if (options.onError) {
      options.onError(event.error)
    }
  }

  recognition.onend = () => {
    // 如果不是手动停止的，且处于 continuous 模式，自动重启
    // Chrome 的 SpeechRecognition 会在静默一段时间后自动停止
    if (!manualStop && recognition.continuous) {
      try {
        recognition.start()
      } catch {
        // 忽略已经在运行的错误
      }
      return
    }
    if (options.onEnd) {
      options.onEnd()
    }
  }

  return {
    isSupported: true,

    start() {
      manualStop = false
      try {
        recognition.start()
      } catch {
        // 可能已经在运行
      }
    },

    stop() {
      manualStop = true
      try {
        recognition.stop()
      } catch {
        // 可能已经停止
      }
    },

    abort() {
      manualStop = true
      try {
        recognition.abort()
      } catch {
        // 忽略
      }
    }
  }
}
