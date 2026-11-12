import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),

  /**
   * 修改当前登录用户的密码：POST /users/changePassword
   * 需携带登录令牌（请求拦截器统一注入 Authorization）。
   *   参数：{ oldPassword: string*, newPassword: string*（6-128 字符） }
   *   返回：无响应体，HTTP 200。
   *   失败：ProblemDetail{status, title, detail}，由 errorMessage 取 detail 展示。
   *     400 原密码错误 / 新密码与原密码相同
   *     404 用户不存在
   *     401 未登录
   * @param {{ oldPassword: string, newPassword: string }} data
   */
  changePassword: (data) => http.post('/users/changePassword', data),
}
