 /**
 * 统一的 AJAX 请求工具（chat 侧）
 * 所有接口调用通过 Header 传递 accesskey：
 *   Authorization: Bearer <accesskey>
 *
 * 注意：本封装与登录页的 ./loginRequest.js 是两套独立封装。
 * 本封装面向 chat 页（/window/*、/loginedwindow/*），
 * 401 只作为错误抛出（由页面自行提示 / 决定后续动作），不做任何路由跳转，
 * 避免 chat 页的鉴权失败把用户踢到登录页。
 */

 const BASE_URL = '/portal'

 let accesskey = ''

 /** 设置/更新全局 accesskey */
 export function setAccesskey(key) {
  accesskey = key || ''
 }

 /** 401 时仅清理本地凭证的回调（不跳转路由） */
 let unauthorizedHandler = null

 /** 注册 401 处理回调：约定只做凭证清理，不做跳转 */
 export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === 'function' ? handler : null
 }

 /** 命中 401 时触发（仅清凭证） */
 function handleUnauthorized() {
  if (unauthorizedHandler) unauthorizedHandler()
 }


/** 组装请求头 */
function buildHeaders(headers = {}) {
  const finalHeaders = { ...headers }
  if (accesskey) {
    finalHeaders.Authorization = `Bearer ${accesskey}`
  }
  return finalHeaders
}

/**
 * 发起请求
 * @param {string} url 请求地址
 * @param {object} options fetch 配置项，可含 method/params/body/headers 等
 */
export async function request(url, options = {}) {
  const { method = 'GET', params, headers, body, ...rest } = options

  // 拼接 query 参数
  let fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`
  if (params && Object.keys(params).length) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) query.append(k, v)
    })
    fullUrl += `${fullUrl.includes('?') ? '&' : '?'}${query.toString()}`
  }

  const finalHeaders = buildHeaders(headers)
  let finalBody = body
  // 对象类型的 body 自动 JSON 序列化
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = finalHeaders['Content-Type'] || 'application/json'
    finalBody = JSON.stringify(body)
  }

  const res = await fetch(fullUrl, {
    method,
    headers: finalHeaders,
    body: finalBody,
    ...rest,
  })

  if (!res.ok) {
    // 401：登录态失效，仅清理本地凭证，不跳转（由调用方决定如何提示）
    if (res.status === 401) handleUnauthorized()
    // 解析 RFC 7807 ProblemDetail / 通用错误体，取可读文案
    let detail = ''
    try {
      const data = await res.json()
      detail = data?.detail || data?.message || data?.title || ''
    } catch {
      // 响应体不是 JSON 时忽略
    }
    const error = new Error(detail || `HTTP ${res.status}`)
    error.status = res.status
    throw error
  }

  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return res.json()
  }
  return res.text()
}

/**
 * 把一块文本（已去掉 event/data 前缀）解析成 { event, data }
 * 服务端事件格式：
 *   event: status
 *   data: {...}
 */
function parseSSEChunk(raw) {
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
  if (!text) return null
  // data 是 JSON 时直接给对象，否则原样返回字符串
  try {
    return { event, data: JSON.parse(text) }
  } catch {
    return { event, data: text }
  }
}

/** 处理缓存中已用空行分隔好的完整事件块，返回未处理完的尾巴 */
function flushSSEBuffer(buffer, dispatch) {
  let rest = buffer
  let idx
  while ((idx = rest.indexOf('\n\n')) !== -1) {
    const item = parseSSEChunk(rest.slice(0, idx))
    if (item) dispatch(item.event, item.data)
    rest = rest.slice(idx + 2)
  }
  return rest
}

/**
 * 发起 SSE 流式请求
 * 请求头同样自动携带 Authorization: Bearer <accesskey>
 * @param {string} url 请求地址
 * @param {object} options 配置项，可含 method/params/body/headers/signal
 *        handlers: { [eventName]: (data) => void } 按事件名回调
 *        （也可用 onEvent(event, data) 统一接收）
 */
export async function requestStream(url, options = {}) {
  const { method = 'POST', params, headers, body, handlers, onEvent, ...rest } = options

  let fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`
  if (params && Object.keys(params).length) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) query.append(k, v)
    })
    fullUrl += `${fullUrl.includes('?') ? '&' : '?'}${query.toString()}`
  }

  const finalHeaders = buildHeaders({ Accept: 'text/event-stream', ...headers })
  let finalBody = body
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = finalHeaders['Content-Type'] || 'application/json'
    finalBody = JSON.stringify(body)
  }

  const res = await fetch(fullUrl, {
    method,
    headers: finalHeaders,
    body: finalBody,
    ...rest,
  })

  const contentType = res.headers.get('content-type') || ''

  // 校验失败等情况：后端返回普通 JSON 错误，而不是事件流
  if (!res.ok || !contentType.includes('text/event-stream')) {
    // 401：登录态失效，仅清理本地凭证，不跳转
    if (res.status === 401) handleUnauthorized()
    let detail
    try {
      const data = await res.json()
      detail = data?.detail || data?.message
    } catch {
      // 响应体不是 JSON 时忽略
    }
    const error = new Error(detail || `请求失败（HTTP ${res.status}）`)
    error.status = res.status
    throw error
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (event, data) => {
    if (typeof handlers?.[event] === 'function') handlers[event](data)
    if (typeof onEvent === 'function') onEvent(event, data)
  }

  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // 兼容 \r\n 换行的服务端实现
      buffer = flushSSEBuffer(buffer.replace(/\r\n/g, '\n'), dispatch)
    }
    // 流结束：处理最后一段没有以空行结尾的事件
    const tail = parseSSEChunk(buffer)
    if (tail) dispatch(tail.event, tail.data)
  } finally {
    reader.releaseLock?.()
  }
}

export default request
