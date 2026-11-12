/**
 * 登录侧（login 页面）专用请求封装，与 chat 侧完全独立。
 *
 * 与 chat 侧的唯一区别在 401 处理策略：
 *   - 本封装：401 视为「账号或密码错误」等业务失败，只把错误抛给页面提示，绝不跳转。
 *   - chat 侧（./request.js）：401 仅清理本地失效凭证，同样不做路由跳转。
 *
 * 两套封装各自维护自己的凭证（登录前无凭证，登录成功后由页面写入第二个参数），
 * 互不影响，避免 chat 页的 401 处理污染登录页。
 */

const BASE_URL = '/portal'

let accesskey = ''

/** 设置/更新登录侧请求凭证（登录页通常不需要） */
export function setAccesskey(key) {
  accesskey = key || ''
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
 * 发起登录侧请求。
 * 任何非 2xx 都只抛出携带 status 与 ProblemDetail 文案的错误，不做跳转、不清凭证。
 *
 * @param {string} url 请求地址（自动拼接 /portal 前缀）
 * @param {object} options fetch 配置项，可含 method/params/body/headers 等
 */
export async function request(url, options = {}) {
  const { method = 'GET', params, headers, body, ...rest } = options

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
    // 只解析错误文案，一律不跳转
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

export default request
