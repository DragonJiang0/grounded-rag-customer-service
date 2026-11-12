// 登录页专用：使用登录侧独立封装，401 只提示不跳转
import { request } from '../utils/loginRequest'
import { setAppid, setToken } from '../utils/auth'

/**
 * 账号密码登录
 * POST /open/loginedwindow/login（开放接口，无需认证）
 * @param {object} params
 * @param {string} params.appKey   应用标识（路由 /login/:appkey 中的 appkey）
 * @param {string} params.username
 * @param {string} params.password
 * @returns {Promise<string>} token 明文字符串
 */
export async function login({ appKey, username, password }) {
  const data = await request('/open/loginedwindow/login', {
    method: 'POST',
    body: { appKey, username, password },
  })
  // portal 成功响应不包装统一结构，直接返回 token 字符串
  const token = typeof data === 'string' ? data.trim() : data?.token
  if (!token) throw new Error('未获取到登录凭证')
  setToken(token)
  setAppid(appKey)
  return token
}
