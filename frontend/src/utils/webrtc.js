/**
 * WebRTC 工具封装（原生 RTCPeerConnection）
 * 信令通过应用内 WebSocket 传递（video_offer / video_answer / video_ice）
 * 视频中继使用二进制帧：[1B flags][2B codecLen][NB codec][8B senderId][视频数据]
 */

/** STUN + TURN 服务器配置（TURN 用于穿透对称 NAT / UDP 被封的环境） */
const ICE_SERVERS = [
  // STUN：探测公网 IP 和端口映射
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  { urls: 'stun:stun2.l.google.com:19302' },
  // TURN 中继（OpenRelay）— TCP/TLS 优先，运营商和防火墙几乎不会封 443 端口
  {
    urls: 'turns:openrelay.metered.ca:443?transport=tcp',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  },
  {
    urls: 'turn:openrelay.metered.ca:443',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  },
  {
    urls: 'turn:openrelay.metered.ca:80',
    username: 'openrelayproject',
    credential: 'openrelayproject'
  }
]

/**
 * 获取本地摄像头+麦克风媒体流
 * @returns {Promise<MediaStream>}
 */
export async function getLocalStream() {
  // 先获取摄像头+麦克风
  try {
    return await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        frameRate: { ideal: 30, max: 30 },
        facingMode: 'user'
      },
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
  } catch (e) {
    // 失败时降级：只获取摄像头，保证视频可用
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        frameRate: { ideal: 30, max: 30 },
        facingMode: 'user'
      },
      audio: false
    })
    // 再尝试单独获取麦克风并添加到流
    try {
      const audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })
      audioStream.getAudioTracks().forEach(track => stream.addTrack(track))
    } catch {
      // 麦克风获取失败，仅用摄像头
    }
    return stream
  }
}

/**
 * 创建 RTCPeerConnection 并绑定事件
 * @param {Object} callbacks
 * @param {(stream: MediaStream) => void} callbacks.onRemoteStream - 收到对方媒体流
 * @param {(candidate: RTCIceCandidate) => void} callbacks.onIceCandidate - 本地 ICE 候选
 * @param {() => void} callbacks.onConnected - 连接建立
 * @param {() => void} callbacks.onDisconnected - 连接断开
 * @param {(err: Error) => void} callbacks.onError - 错误
 * @returns {RTCPeerConnection}
 */
export function createPeerConnection({ onRemoteStream, onIceCandidate, onConnected, onDisconnected, onError }) {
  const pc = new RTCPeerConnection({
    iceServers: ICE_SERVERS,
    iceCandidatePoolSize: 2 // 预分配候选，加速 ICE 收敛
  })

  pc.onicecandidate = (event) => {
    if (event.candidate) {
      if (onIceCandidate) onIceCandidate(event.candidate)
    }
  }

  // 记录 ICE 候选收集失败（ TURN 不可达时会触发）
  pc.addEventListener('icecandidateerror', (event) => {
    console.warn('[WebRTC] ICE candidate error:', event.errorCode, event.errorText, 'url:', event.url)
  })

  pc.ontrack = (event) => {
    if (event.streams && event.streams[0] && onRemoteStream) {
      onRemoteStream(event.streams[0])
    }
  }

  pc.oniceconnectionstatechange = () => {
    const state = pc.iceConnectionState
    if (state === 'connected' || state === 'completed') {
      if (onConnected) onConnected()
    } else if (state === 'failed') {
      if (onDisconnected) onDisconnected()
    } else if (state === 'disconnected') {
      if (onDisconnected) onDisconnected()
    }
  }

  pc.onerror = (event) => {
    console.error('[WebRTC] error:', event)
    if (onError) onError(event)
  }

  return pc
}

/**
 * 发起方：添加本地流 + 创建 SDP Offer
 * @param {RTCPeerConnection} pc
 * @param {MediaStream} localStream
 * @returns {Promise<RTCSessionDescription>} SDP Offer
 */
export async function createOffer(pc, localStream) {
  // 添加本地轨道到 peer connection
  localStream.getTracks().forEach(track => pc.addTrack(track, localStream))

  const offer = await pc.createOffer()
  await pc.setLocalDescription(offer)
  return pc.localDescription
}

/**
 * 接收方：处理 SDP Offer + 添加本地流 + 创建 SDP Answer
 * @param {RTCPeerConnection} pc
 * @param {RTCSessionDescription} offer - 收到的 SDP Offer
 * @param {MediaStream} localStream
 * @returns {Promise<RTCSessionDescription>} SDP Answer
 */
export async function handleOffer(pc, offer, localStream) {
  await pc.setRemoteDescription(new RTCSessionDescription(offer))

  if (!localStream) {
    throw new Error('localStream 为空，无法添加本地轨道')
  }

  localStream.getTracks().forEach(track => pc.addTrack(track, localStream))

  const answer = await pc.createAnswer()
  await pc.setLocalDescription(answer)
  return pc.localDescription
}

/**
 * 发起方：处理 SDP Answer
 * @param {RTCPeerConnection} pc
 * @param {RTCSessionDescription} answer - 收到的 SDP Answer
 */
export async function handleAnswer(pc, answer) {
  await pc.setRemoteDescription(new RTCSessionDescription(answer))
}

/**
 * 添加 ICE 候选（处理远端发来的 candidate）
 * @param {RTCPeerConnection} pc
 * @param {RTCIceCandidateInit} candidate
 */
export async function addIceCandidate(pc, candidate) {
  try {
    await pc.addIceCandidate(new RTCIceCandidate(candidate))
  } catch (err) {
    console.error('[WebRTC] failed to add ice candidate:', err.message || err)
  }
}

/**
 * ICE 重启：当连接失败时重新触发 ICE 收敛
 * @param {RTCPeerConnection} pc
 * @returns {Promise<RTCSessionDescription>} 新的 SDP Offer
 */
export async function restartIce(pc) {
  const offer = await pc.createOffer({ iceRestart: true })
  await pc.setLocalDescription(offer)
  return pc.localDescription
}

/**
 * 关闭连接并释放资源
 * @param {RTCPeerConnection|null} pc
 * @param {MediaStream|null} localStream
 */
export function closeConnection(pc, localStream) {
  if (localStream) {
    localStream.getTracks().forEach(track => track.stop())
  }
  if (pc) {
    try { pc.close() } catch {}
  }
}

// ==================== WebSocket Media Relay（P2P 失败时的回退方案） ====================

/** 发送方 MediaRecorder 实例 */
let relayMediaRecorder = null

/**
 * 构造二进制帧
 * 协议：[1B flags] [2B codec长度] [NB codec字符串] [8B senderId] [剩余: 视频数据]
 * @param {ArrayBuffer} videoBuffer - MediaRecorder 产出的原始视频数据
 * @param {boolean} isInit - 是否为初始化段
 * @param {string} codec - MIME 类型
 * @param {number} senderId - 发送者ID
 * @returns {ArrayBuffer} 完整的二进制帧
 */
function buildBinaryFrame(videoBuffer, isInit, codec, senderId) {
  const codecBytes = new TextEncoder().encode(codec)
  const headerLen = 1 + 2 + codecBytes.length + 8  // flags + codecLen + codec + senderId
  const frame = new ArrayBuffer(headerLen + videoBuffer.byteLength)
  const view = new DataView(frame)
  const u8 = new Uint8Array(frame)

  // 1B flags：bit0 = init
  view.setUint8(0, isInit ? 1 : 0)
  // 2B codec 长度（big-endian）
  view.setUint16(1, codecBytes.length, false)
  // NB codec 字符串
  u8.set(codecBytes, 3)
  // 8B senderId（big-endian，分两次写入规避 JS 精度限制）
  view.setUint32(3 + codecBytes.length, Math.floor(senderId / 0x100000000), false)
  view.setUint32(3 + codecBytes.length + 4, senderId >>> 0, false)
  // 剩余：原始视频数据
  u8.set(new Uint8Array(videoBuffer), headerLen)

  return frame
}

/**
 * 启动 WebSocket Media Relay（发送方）
 * 用 MediaRecorder 编码本地流，通过二进制帧发送原始视频数据（无 base64 开销）
 * @param {MediaStream} localStream - 本地媒体流
 * @param {(msg: object) => void} sendFn - 文本消息发送函数（如 wsSend）
 * @param {object} options - 配置项
 * @param {(buf: ArrayBuffer) => void} options.sendBinary - 二进制帧发送函数
 * @param {number} options.senderId - 发送者用户ID
 */
export function startMediaRelay(localStream, sendFn, options = {}) {
  if (relayMediaRecorder) return

  const { sendBinary, senderId } = options

  // 选择浏览器支持的编码格式（VP8 优先，兼容性最好）
  const mimeType = MediaRecorder.isTypeSupported('video/webm; codecs=vp8,opus')
    ? 'video/webm; codecs=vp8,opus'
    : MediaRecorder.isTypeSupported('video/webm; codecs=vp9,opus')
      ? 'video/webm; codecs=vp9,opus'
      : 'video/webm'

  // 中继模式画质设置（720p / 15fps / 1Mbps — 二进制帧无膨胀）
  const videoTrack = localStream.getVideoTracks()[0]
  if (videoTrack) {
    videoTrack.applyConstraints({
      width: { ideal: 1280 },
      height: { ideal: 720 },
      frameRate: { ideal: 15, max: 18 }
    }).catch(e => {
      console.warn('[Relay] applyConstraints failed:', e.message)
    })
  }

  relayMediaRecorder = new MediaRecorder(localStream, {
    mimeType,
    videoBitsPerSecond: 800000, // 800kbps 视频（720p 码率，减小帧体积降低 appendBuffer 耗时）
    audioBitsPerSecond: 128000   // 64kbps 音频
  })

  let relaySendCount = 0

  relayMediaRecorder.ondataavailable = (e) => {
    if (e.data.size === 0) return
    relaySendCount++
    const isInit = relaySendCount === 1

    e.data.arrayBuffer().then(buffer => {
      try {
        if (sendBinary && senderId) {
          // 二进制帧模式：构造帧头 + 原始视频数据，直接发送
          const frame = buildBinaryFrame(buffer, isInit, mimeType, senderId)
          sendBinary(frame)
        } else {
          // 降级：base64 + JSON 文本帧（兼容旧后端）
          const bytes = new Uint8Array(buffer)
          let binary = ''
          for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i])
          }
          sendFn({ type: 'video_media', data: btoa(binary), init: isInit, codec: mimeType })
        }
      } catch (err) {
        console.error(`[Relay] send failed for chunk #${relaySendCount}:`, err.message)
      }
    })
  }

  relayMediaRecorder.onstop = () => {
    relayMediaRecorder = null
  }

  relayMediaRecorder.onerror = (e) => {
    console.error('[Relay] MediaRecorder error:', e.error?.message || e.error)
    relayMediaRecorder = null
  }

  // 每 180ms 切割一次数据（阈值：160ms 会卡死）
  relayMediaRecorder.start(180)
}

/**
 * 停止 WebSocket Media Relay（发送方）
 */
export function stopMediaRelay() {
  if (relayMediaRecorder && relayMediaRecorder.state !== 'inactive') {
    relayMediaRecorder.stop()
  }
  relayMediaRecorder = null
  // 停止 WebCodecs 编码器
  if (webCodecsEncoder) {
    try { webCodecsEncoder.close() } catch {}
    webCodecsEncoder = null
  }
  if (webCodecsRafId) {
    cancelAnimationFrame(webCodecsRafId)
    webCodecsRafId = null
  }
  if (webCodecsStreamTrack) {
    webCodecsStreamTrack = null
  }
  // 清理音频录制器
  if (webCodecsAudioRecorder && webCodecsAudioRecorder.state !== 'inactive') {
    try { webCodecsAudioRecorder.stop() } catch {}
  }
  webCodecsAudioRecorder = null
}

// ==================== WebCodecs 直接编码方案（绕过 WebM 容器 + MediaSource） ====================

/** WebCodecs 编码器实例 */
let webCodecsEncoder = null
let webCodecsRafId = null
let webCodecsStreamTrack = null
/** WebCodecs 音频：用独立 MediaRecorder 只录音频 */
let webCodecsAudioRecorder = null

/**
 * 检测 WebCodecs 是否可用（需要 VideoEncoder + VideoDecoder + MediaStreamTrackProcessor）
 * @returns {boolean}
 */
export function isWebCodecsSupported() {
  return typeof VideoEncoder !== 'undefined' &&
         typeof VideoDecoder !== 'undefined' &&
         typeof VideoFrame !== 'undefined' &&
         typeof MediaStreamTrackProcessor !== 'undefined'
}

/**
 * 启动 WebCodecs 视频发送方
 * 用 VideoEncoder 直接编码 VP8 原始帧，跳过 WebM 容器，接收端可用 VideoDecoder 异步解码
 * @param {MediaStream} localStream - 本地媒体流
 * @param {(buf: ArrayBuffer) => void} sendBinaryFn - 二进制帧发送函数
 * @param {number} senderId - 发送者ID
 * @param {object} [options]
 * @param {() => void} [options.onError] - 编码失败时的回调（用于降级到 MediaRecorder）
 */
export function startWebCodecsSender(localStream, sendBinaryFn, senderId, options = {}) {
  if (webCodecsEncoder) {
    console.warn('[WebCodecs] encoder already running, skipping')
    return
  }

  const videoTrack = localStream.getVideoTracks()[0]
  if (!videoTrack) {
    console.error('[WebCodecs] no video track found')
    return
  }

  // 限制到 1080p 上限，帧率取原生值（不超过 30fps）
  const settings = videoTrack.getSettings()
  const nativeWidth = Math.min(settings.width || 1280, 1920)
  const nativeHeight = Math.min(settings.height || 720, 1080)
  const nativeFps = Math.min(settings.frameRate || 30, 30)

  // 如果原生分辨率超过 1080p，降下来
  if ((settings.width || 0) > 1920 || (settings.height || 0) > 1080) {
    videoTrack.applyConstraints({
      width: { ideal: 1920 },
      height: { ideal: 1080 }
    }).catch(e => console.warn('[WebCodecs] applyConstraints failed:', e.message))
  }

  const codec = 'vp8'
  const bitrate = 2500000 // 2.5Mbps
  const frameInterval = 1000 / nativeFps
  let sendCount = 0
  let lastFrameTime = 0

  // 创建编码器
  const encoder = new VideoEncoder({
    output: (chunk, metadata) => {
      const buffer = new ArrayBuffer(chunk.byteLength)
      chunk.copyTo(buffer)
      sendCount++

      // codec="vp8" 表示 WebCodecs 原始帧，接收端用 VideoDecoder 解码
      const frame = buildBinaryFrame(buffer, chunk.type === 'key', 'vp8', senderId)
      sendBinaryFn(frame)

    },
    error: (e) => {
      console.error('[WebCodecs] encoder error:', e.message)
      webCodecsEncoder = null
    }
  })

  try {
    encoder.configure({
      codec,
      width: nativeWidth,
      height: nativeHeight,
      bitrate,
      framerate: nativeFps,
      latencyMode: 'realtime' // 低延迟优先
    })
  } catch (e) {
    console.error('[WebCodecs] encoder.configure failed:', e.message)
    webCodecsEncoder = null
    encoder.close()
    return
  }

  webCodecsEncoder = encoder
  webCodecsStreamTrack = videoTrack

  // 用 MediaStreamTrackProcessor 采集帧
  let reader
  try {
    const processor = new MediaStreamTrackProcessor({ track: videoTrack })
    reader = processor.readable.getReader()
  } catch (e) {
    console.error('[WebCodecs] MediaStreamTrackProcessor failed:', e.message)
    webCodecsEncoder = null
    encoder.close()
    if (options.onError) options.onError()
    return
  }

  const processFrames = async () => {
    let readCount = 0
    while (webCodecsEncoder === encoder) {
      let result
      try {
        result = await reader.read()
      } catch (e) {
        console.error('[WebCodecs] reader.read() failed:', e.message)
        break
      }
      const { value: frame, done } = result
      readCount++
      if (done || webCodecsEncoder !== encoder) {
        if (frame) frame.close()
        break
      }

      const now = performance.now()
      // 按帧率限流：距离上一帧不足间隔则跳过
      if (now - lastFrameTime < frameInterval) {
        frame.close()
        continue
      }
      lastFrameTime = now

      // 编码队列过长时跳帧，防止积压
      if (encoder.encodeQueueSize > 2) {
        frame.close()
        console.warn('[WebCodecs] encoder queue full, dropping frame')
        continue
      }

      const makeKeyFrame = sendCount === 0 || sendCount % 72 === 0 // 每 3 秒一个关键帧 (24fps)
      try {
        encoder.encode(frame, { keyFrame: makeKeyFrame })
      } catch (e) {
        console.error('[WebCodecs] encode failed:', e.message, 'frame size:', frame.displayWidth, 'x', frame.displayHeight)
      }
      frame.close()
    }
    reader.releaseLock()
  }

  processFrames().catch(e => {
    console.error('[WebCodecs] processFrames crashed:', e.message)
    if (webCodecsEncoder === encoder) {
      webCodecsEncoder = null
      encoder.close()
    }
    // 触发降级到 MediaRecorder
    if (options.onError) options.onError()
  })

  // ===== 音频：用独立 MediaRecorder 只录音频 =====
  // 优先 MP4 AAC（手机 MediaSource 广泛支持），fallback WebM Opus
  const audioTrack = localStream.getAudioTracks()[0]
  if (audioTrack) {
    try {
      const audioStream = new MediaStream([audioTrack])
      // 优先尝试 MP4 AAC（Android MediaSource 支持率远高于 WebM Opus）
      let mimeType = 'audio/webm; codecs=opus'
      let audioCodec = 'opus-audio' // 接收端标识
      if (MediaRecorder.isTypeSupported('audio/mp4; codecs=mp4a.40.2')) {
        mimeType = 'audio/mp4; codecs=mp4a.40.2'
        audioCodec = 'aac-audio'
      } else if (MediaRecorder.isTypeSupported('audio/webm; codecs=opus')) {
        mimeType = 'audio/webm; codecs=opus'
        audioCodec = 'opus-audio'
      } else if (MediaRecorder.isTypeSupported('audio/webm')) {
        mimeType = 'audio/webm'
        audioCodec = 'webm-audio'
      }

      let audioSendCount = 0
      const audioRecorder = new MediaRecorder(audioStream, {
        mimeType,
        audioBitsPerSecond: 128000
      })

      audioRecorder.ondataavailable = (e) => {
        if (e.data.size === 0) return
        audioSendCount++
        e.data.arrayBuffer().then(buffer => {
          try {
            const frame = buildBinaryFrame(buffer, audioSendCount === 1, audioCodec, senderId)
            sendBinaryFn(frame)
          } catch (err) {
            console.error('[WebCodecs] audio send failed:', err.message)
          }
        })
      }

      audioRecorder.onerror = (e) => {
        console.error('[WebCodecs] audio recorder error:', e.error?.message)
      }

      // 每 180ms 切割一次音频数据
      audioRecorder.start(180)
      webCodecsAudioRecorder = audioRecorder
      console.log(`[WebCodecs] audio recorder started (${mimeType}, 64kbps)`)
    } catch (e) {
      console.warn('[WebCodecs] audio recorder init failed:', e.message, '— video only')
    }
  }
}

/**
 * 创建独立的音频接收器（双 Audio 元素交替播放）
 * 与视频接收器完全分离，避免跨模块状态干扰
 * 双元素交替：当 A 正在播放时设置 B 的 src，减少 play() 频率限制
 * @returns {{ handleData: Function, cleanup: Function }}
 */
export function createAudioReceiver() {
  let audioInitData = null
  let audioMime = 'audio/mp4'
  let audioElA = null
  let audioElB = null
  let audioToggle = false // false = 用 A, true = 用 B

  function handleData(data) {
    // 仅处理音频帧
    if (data.codec !== 'opus-audio' && data.codec !== 'aac-audio' && data.codec !== 'webm-audio') return

    const raw = data.data instanceof Uint8Array
      ? data.data
      : Uint8Array.from(atob(data.data), c => c.charCodeAt(0))

    // init 段：存储并创建 Audio 元素
    if (data.init) {
      audioInitData = new Uint8Array(raw)
      audioMime = data.codec === 'aac-audio' ? 'audio/mp4' : 'audio/webm'
      // 创建双 Audio 元素
      if (!audioElA) {
        audioElA = document.createElement('audio')
        audioElA.style.display = 'none'
        audioElA.volume = 1.0
        document.body.appendChild(audioElA)
      }
      if (!audioElB) {
        audioElB = document.createElement('audio')
        audioElB.style.display = 'none'
        audioElB.volume = 1.0
        document.body.appendChild(audioElB)
      }
      return
    }

    if (!audioInitData) return

    // 交替选择 Audio 元素：A 正在播放时设置 B，反之亦然
    const el = audioToggle ? audioElB : audioElA
    audioToggle = !audioToggle

    // 合并 init + chunk → blob
    const combined = new Uint8Array(audioInitData.byteLength + raw.byteLength)
    combined.set(audioInitData, 0)
    combined.set(raw, audioInitData.byteLength)
    const blob = new Blob([combined], { type: audioMime })
    const url = URL.createObjectURL(blob)

    // 释放该元素上一次的 URL
    const oldUrl = el._currentUrl
    if (oldUrl) {
      try { URL.revokeObjectURL(oldUrl) } catch {}
    }
    el._currentUrl = url
    el.src = url
    el.play().catch(e => {
      // 播放失败时尝试恢复：静默重试一次
      setTimeout(() => {
        try { el.play() } catch {}
      }, 200)
    })
  }

  function cleanup() {
    audioInitData = null
    const els = [audioElA, audioElB]
    els.forEach(el => {
      if (!el) return
      try { el.pause(); el.src = '' } catch {}
      if (el._currentUrl) { try { URL.revokeObjectURL(el._currentUrl) } catch {} }
      el._currentUrl = null
      if (el.parentElement) el.parentElement.removeChild(el)
    })
    audioElA = null
    audioElB = null
  }

  return { handleData, cleanup }
}

/**
 * 创建 WebCodecs 视频接收器
 * 用 VideoDecoder 解码 VP8 原始帧 + canvas 渲染，完全绕过 MediaSource/appendBuffer
 * 音频已分离到独立的 createAudioReceiver，此函数仅处理视频
 * @returns {{ handleData: Function, cleanup: Function, failed: boolean, getCanvas: Function }}
 */
export function createWebCodecsReceiver() {
  let decoder = null
  let canvas = null
  let ctx = null
  let generation = 0
  let frameCount = 0
  let failed = false
  let firstFrameTimer = null

  function handleData(data, el) {
    if (failed) return

    try {
      const raw = data.data instanceof Uint8Array
        ? data.data
        : Uint8Array.from(atob(data.data), c => c.charCodeAt(0))

      // ===== 视频帧处理（WebCodecs VideoDecoder + canvas）=====
      // 音频帧已路由到独立的 createAudioReceiver，此处只处理视频
      if (!decoder) {
        if (!el || !el.tagName) {
          console.error('[WebCodecs] receiver: invalid element:', typeof el)
          return
        }

        canvas = document.createElement('canvas')
        canvas.style.cssText = 'width:100%;height:100%;object-fit:contain;background:#000'
        ctx = canvas.getContext('2d')

        el.style.display = 'none'
        if (el.parentElement) {
          el.parentElement.appendChild(canvas)
        }

        decoder = new VideoDecoder({
          output: (videoFrame) => {
            if (failed) { videoFrame.close(); return }
            if (firstFrameTimer) { clearTimeout(firstFrameTimer); firstFrameTimer = null }

            if (canvas.width !== videoFrame.displayWidth || canvas.height !== videoFrame.displayHeight) {
              canvas.width = videoFrame.displayWidth
              canvas.height = videoFrame.displayHeight
            }
            ctx.drawImage(videoFrame, 0, 0)
            videoFrame.close()
            frameCount++
          },
          error: (e) => {
            console.error('[WebCodecs] decoder error:', e.message)
            failed = true
            if (firstFrameTimer) { clearTimeout(firstFrameTimer); firstFrameTimer = null }
            cleanup()
          }
        })

        const codec = data.codec === 'vp8' ? 'vp8' : 'vp9'
        try {
          decoder.configure({ codec })
        } catch (e) {
          console.error('[WebCodecs] decoder.configure failed:', e.message)
          failed = true
          cleanup()
          return
        }

        firstFrameTimer = setTimeout(() => {
          if (frameCount === 0 && !failed) {
            console.error('[WebCodecs] no frame rendered in 5s, marking as failed')
            failed = true
            cleanup()
          }
        }, 5000)
      }

      const chunk = new EncodedVideoChunk({
        type: data.init ? 'key' : 'delta',
        timestamp: performance.now() * 1000,
        data: raw
      })
      decoder.decode(chunk)

    } catch (e) {
      console.error('[WebCodecs] receiver error:', e.message)
      failed = true
      if (firstFrameTimer) { clearTimeout(firstFrameTimer); firstFrameTimer = null }
      cleanup()
    }
  }

  function cleanup() {
    generation++
    if (firstFrameTimer) { clearTimeout(firstFrameTimer); firstFrameTimer = null }
    if (decoder) {
      try { decoder.close() } catch {}
      decoder = null
    }
    if (canvas && canvas.parentElement) {
      canvas.parentElement.removeChild(canvas)
    }
    canvas = null
    ctx = null
    frameCount = 0
  }

  return {
    handleData,
    cleanup,
    getCanvas: () => canvas,
    get failed() { return failed }
  }
}

/**
 * 创建独立的 Relay 接收器状态
 * 解决全局变量冲突问题：每个视频通话使用独立的状态实例
 */
export function createRelayReceiver() {
  let mediaSource = null
  let sourceBuffer = null
  let videoEl = null
  let queue = []
  let initReceived = false
  let generation = 0
  let isPlaying = false // 跟踪播放状态，避免重复调用 play()

  /**
   * 处理接收到的 relay 数据
   * @param {object} data - { init: boolean, data: string (base64), codec: string }
   * @param {HTMLVideoElement} el - 要绑定播放的 video 元素
   */
  function handleData(data, el) {
    try {
      if (!el || !el.tagName) {
        console.error('[Relay] handleRelayData: videoEl is not a valid DOM element:', typeof el)
        return
      }

      // 二进制模式：data.data 已经是 Uint8Array；文本模式：需要 atob 解码
      const raw = data.data instanceof Uint8Array
        ? data.data
        : Uint8Array.from(atob(data.data), c => c.charCodeAt(0))
      if (data.init) {
        initReceived = true
        queue = []
        videoEl = el

        generation++
        const currentGen = generation

        if (mediaSource) {
          try { URL.revokeObjectURL(el.src) } catch {}
          try { mediaSource.endOfStream() } catch {}
          mediaSource = null
          sourceBuffer = null
        }

        if (typeof MediaSource === 'undefined') {
          console.error('[Relay] MediaSource API not available')
          return
        }

        mediaSource = new MediaSource()
        el.srcObject = null
        el.src = URL.createObjectURL(mediaSource)
        isPlaying = false // 重置播放状态

        const sourceopenTimeout = setTimeout(() => {
          if (currentGen === generation && (!sourceBuffer || mediaSource?.readyState !== 'open')) {
            console.error('[Relay] sourceopen timeout')
          }
        }, 3000)

        // 用函数封装 sourceopen 处理，避免事件竞态
        const onSourceOpen = () => {
          clearTimeout(sourceopenTimeout)
          if (currentGen !== generation) return
          if (sourceBuffer) return // 防止重复初始化

          let codec = data.codec || 'video/webm'
          if (!data.codec) {
            const codecs = [
              'video/webm; codecs=vp8,opus',
              'video/webm; codecs=vp9,opus',
              'video/webm'
            ]
            for (const c of codecs) {
              if (MediaSource.isTypeSupported(c)) {
                codec = c
                break
              }
            }
          }
          if (!MediaSource.isTypeSupported(codec)) {
            console.warn('[Relay] codec not supported:', codec, 'falling back to video/webm')
            codec = 'video/webm'
          }

          try {
            sourceBuffer = mediaSource.addSourceBuffer(codec)
            sourceBuffer.mode = 'sequence'
            sourceBuffer.addEventListener('updateend', drainQueue)
            sourceBuffer.addEventListener('error', (e) => {
              console.error('[Relay] SourceBuffer error:', e)
            })
            sourceBuffer.appendBuffer(raw)
            // 只在未播放时才调用 play()，避免重复播放
            if (!isPlaying) {
              el.play().then(() => {
                isPlaying = true
              }).catch(e => {})
            }
          } catch (e) {
            console.error('[Relay] addSourceBuffer/appendBuffer failed:', e.message, 'codec:', codec)
          }
        }

        // 先注册监听，再检查 readyState，确保不丢失事件
        mediaSource.addEventListener('sourceopen', onSourceOpen)
        if (mediaSource.readyState === 'open') {
          onSourceOpen()
        }

        mediaSource.addEventListener('error', (e) => {
          console.error('[Relay] MediaSource error:', e)
        })
      } else if (sourceBuffer && !sourceBuffer.updating) {
        try {
          sourceBuffer.appendBuffer(raw)
        } catch (e) {
          console.warn('[Relay] appendBuffer failed, queuing:', e.message)
          queue.push(raw)
        }
      } else {
        queue.push(raw)
        if (queue.length > 200) {
          queue.splice(0, queue.length - 100)
        }
      }
    } catch (e) {
      console.error('[Relay] handleRelayData error:', e.message || e)
    }
  }

  function drainQueue() {
    if (!sourceBuffer || sourceBuffer.updating) return
    if (queue.length === 0) return
    const chunk = queue.shift()
    try {
      sourceBuffer.appendBuffer(chunk)
    } catch (e) {
      console.error('[Relay] drainQueue appendBuffer failed:', e.message)
      if (queue.length > 0) {
        setTimeout(drainQueue, 0)
      }
    }
  }

  function cleanup() {
    generation++
    isPlaying = false
    if (mediaSource) {
      try { mediaSource.endOfStream() } catch {}
      mediaSource = null
    }
    sourceBuffer = null
    queue = []
    initReceived = false
    if (videoEl) {
      try { videoEl.pause() } catch {}
      try { videoEl.src = '' } catch {}
      videoEl = null
    }
  }

  return { handleData, cleanup }
}
