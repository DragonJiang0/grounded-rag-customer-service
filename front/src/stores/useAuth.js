/* ============================================================
   useAuth.js —— 登录态：仅维护本地令牌。
   后端无状态 JWT，没有服务端登出/黑名单，退出就是清掉本地令牌。
   当前用户名直接在前端写死，不再依赖任何用户信息接口（如 /auth/me）。
   ============================================================ */

import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'

const TOKEN_KEY = 'token'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')

/** 写死当前登录用户，避免再调用 /auth/me 之类的接口 */
const CURRENT_USER = '管理员'

function setToken(value) {
  token.value = value || ''
  if (value) {
    localStorage.setItem(TOKEN_KEY, value)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** 登录成功后写入令牌；失败时把后端错误原样抛给调用方展示 */
async function login(username, password) {
  const data = await authApi.login({ username: username.trim(), password })
  setToken(data.token)
}

/** 登出：先通知后端作废令牌，无论成功与否都清掉本地令牌 */
async function logout() {
  try {
    await authApi.logout()
  } catch (err) {
    // 后端已失效或网络异常都不影响本地登出，仅记录日志
    console.warn('[auth] 调用 /auth/logout 失败：', err?.message || err)
  } finally {
    setToken('')
  }
}

export function useAuth() {
  return {
    token,
    currentUser: CURRENT_USER,
    isLoggedIn: computed(() => !!token.value),
    login,
    logout,
  }
}
