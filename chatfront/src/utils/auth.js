/**
 * 登录态（token）管理
 * token 存 localStorage，刷新页面后保持登录，除非接口返回 401 才清除。
 */
const TOKEN_KEY = 'chatfront_token'

/** 读取本地 token */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 写入 token */
export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** 清除 token（401 或主动登出） */
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** 是否已登录 */
export function isLogined() {
  return !!getToken()
}

const APPID_KEY = 'chatfront_appid'

/** 读取登录时使用的 appid（用于 401 后跳回对应登录页） */
export function getAppid() {
  return localStorage.getItem(APPID_KEY) || ''
}

/** 记录 appid */
export function setAppid(appid) {
  if (appid) {
    localStorage.setItem(APPID_KEY, appid)
  } else {
    localStorage.removeItem(APPID_KEY)
  }
}

/** 清除 appid（登出） */
export function clearAppid() {
  localStorage.removeItem(APPID_KEY)
}
