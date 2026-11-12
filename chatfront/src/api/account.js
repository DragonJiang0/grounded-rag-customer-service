// chat 页（登录窗口）专用：走 chat 侧封装，401 只清凭证不跳转
import { request } from '../utils/request'
import { clearAppid, clearToken } from '../utils/auth'

/**
 * 退出登录
 * POST /open/loginedwindow/logout，令牌取请求头 Authorization: Bearer {token}
 * 未携带令牌时后端按成功幂等处理
 */
export async function logout() {
  try {
    await request('/open/loginedwindow/logout', { method: 'POST' })
  } finally {
    clearToken()
    clearAppid()
  }
}

/**
 * 修改登录窗口密码
 * POST /loginedwindow/changePassword
 * @param {object} params
 * @param {string} params.oldPassword 原密码
 * @param {string} params.newPassword 新密码（6-128 字符）
 * @returns {Promise<void>} 成功无响应体（HTTP 200）
 */
export async function changePassword({ oldPassword, newPassword }) {
  await request('/loginedwindow/changePassword', {
    method: 'POST',
    body: { oldPassword, newPassword },
  })
}
