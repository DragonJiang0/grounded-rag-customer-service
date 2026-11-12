/* ============================================================
   sse.js —— SSE（text/event-stream）请求封装。

   走原生 fetch + ReadableStream，因为 axios 无法读取流式响应体。
   请求地址与鉴权头与 http.js 保持一致（同源 /manage 由 vite 代理转发）。

   服务端事件格式：
     event: status
     data: {...}

     event: message
     data: {...}
   多个事件之间以空行分隔，解析时按空行切块。
   ============================================================ */

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/manage'
const TOKEN_KEY = 'token'

/** 把一块文本（已去掉 event/data 前缀）解析成 { event, data } */
function parseChunk(raw) {
  let event = 'message'
  const dataLines = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  const text = dataLines.join('\n')
  if (!text) {
    return null
  }
  // data 是 JSON 时直接给对象，否则原样返回字符串
  try {
    return { event, data: JSON.parse(text) }
  } catch {
    return { event, data: text }
  }
}

/** 处理缓存中已用空行分隔好的完整事件块，返回未处理完的尾巴 */
function flush(buffer, onEvent) {
  let rest = buffer
  let idx
  while ((idx = rest.indexOf('\n\n')) !== -1) {
    const item = parseChunk(rest.slice(0, idx))
    if (item) {
      onEvent(item.event, item.data)
    }
    rest = rest.slice(idx + 2)
  }
  return rest
}

/**
 * 发起一次 SSE 请求并逐事件回调。
 *
 * @param {string} url         相对 BASE_URL 的路径，如 '/app/setting/debug'
 * @param {object} body        请求体，会被序列化为 JSON
 * @param {object} handlers    事件回调 { [eventName]: (data) => void }
 * @param {AbortSignal} signal 用于中断请求
 */
export async function postSSE(url, body, handlers = {}, signal) {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch(`${BASE_URL}${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body ?? {}),
    signal,
  })

  const contentType = res.headers.get('content-type') || ''

  // 校验失败等情况：后端返回普通 JSON 错误，而不是事件流
  if (!res.ok || !contentType.includes('text/event-stream')) {
    let detail
    try {
      detail = (await res.json())?.detail
    } catch {
      // 响应体不是 JSON 时忽略，走下面的兜底文案
    }
    const err = new Error(detail || `请求失败（HTTP ${res.status}）`)
    // 与 errorMessage() 的取值口径保持一致
    err.response = { status: res.status, data: detail ? { detail } : undefined }
    throw err
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      // 兼容 \r\n 换行的服务端实现
      buffer = flush(buffer.replace(/\r\n/g, '\n'), (event, data) => {
        handlers[event]?.(data)
      })
    }
    // 流结束：处理最后一段没有以空行结尾的事件
    const tail = parseChunk(buffer)
    if (tail) {
      handlers[tail.event]?.(tail.data)
    }
  } finally {
    reader.releaseLock?.()
  }
}
