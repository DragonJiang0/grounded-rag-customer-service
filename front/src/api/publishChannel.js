import http from './http'

export const publishChannelApi = {
  createApi: (appId, data) => http.post(`/app-publish-channels/createApi/${appId}`, data),

  apiList: (appId) => http.get(`/app-publish-channels/apiList/${appId}`),

  editApi: (data) => http.post('/app-publish-channels/editApi', data),

  deleteApi: (id) => http.post(`/app-publish-channels/deleteApi/${id}`),

  /**
   * 创建免密窗口。
   * @param {number|string} appId
   * @param {{ name:string }} data
   * @returns {Promise<string>} 明文 windowKey（形如 wk-xxxxxx，仅本次返回）
   */
  createWindow: (appId, data) => http.post(`/app-publish-channels/createWindow/${appId}`, data),

  /**
   * 编辑免密窗口，仅允许修改名称。
   * @param {{ id:number, name:string }} data
   */
  editWindow: (data) => http.post('/app-publish-channels/editWindow', data),

  /**
   * 免密窗口列表（windowKey 已脱敏）。
   * @param {number|string} appId
   */
  windowList: (appId) => http.get(`/app-publish-channels/windowList/${appId}`),

  /**
   * 按 ID 删除免密窗口。
   * @param {number|string} id
   */
  deleteWindow: (id) => http.post(`/app-publish-channels/deleteWindow/${id}`),

  /**
   * 创建账号密码登录窗口。
   * 创建后密码由服务端初始化为 1234llm 并以 BCrypt 密文落库，窗口默认启用；
   * 同一应用内窗口名与登录账号不可重复。
   * @param {number|string} appId
   * @param {{ name:string, username:string }} data
   * @returns {Promise<string>} 窗口唯一标识 windowKey（非统一包装，直接为字符串）
   */
  createPasswordWindow: (appId, data) =>
    http.post(`/app/setting/createPasswordWindow/${appId}`, data),

  /**
   * 查询账号密码窗口列表（纯数组，非统一包装）。
   * 该应用下全部窗口，不分页、按创建时间倒序，不返回密码。
   * @param {number|string} appId
   * @returns {Promise<Array<{ id:number, appId:number, name:string, windowKey:string,
   *   username:string, status:number, lastUsedAt:string|null,
   *   createdAt:string, updatedAt:string }>>}
   */
  passwordWindowList: (appId) => http.get(`/app/setting/passwordWindowList/${appId}`),

  /**
   * 切换账号密码窗口状态（按 ID 反转启用/禁用，1↔0）。
   * @param {number|string} id
   * @returns {Promise<void>} 无响应体；记录不存在返回 404
   */
  togglePasswordWindowStatus: (id) =>
    http.post(`/app/setting/togglePasswordWindowStatus/${id}`),

  /**
   * 删除账号密码登录窗口。
   * @param {number|string} id
   * @returns {Promise<void>} 无响应体；记录不存在返回 404
   */
  deletePasswordWindow: (id) => http.post(`/app/setting/deletePasswordWindow/${id}`),

  /**
   * 重置账号密码窗口密码为默认口令 1234llm。
   * 不影响启用/禁用状态；记录不存在返回 404。
   * @param {number|string} id
   * @returns {Promise<void>} 无响应体
   */
  resetPasswordWindowPassword: (id) =>
    http.post(`/app/setting/resetPasswordWindowPassword/${id}`),
}

export const chatHistoryApi = {
  /**
   * 按应用分页查聊天记录。
   * appId 走 URL，渠道与时间范围放 JSON body，未传的筛选条件表示不限。
   * @param {number|string} appId
   * @param {{ page?:number, size?:number, channel?:number, startTime?:string, endTime?:string }} data
   */
  page: (appId, data) => http.post(`/app/setting/chatHistory/${appId}`, data),
}

export const botLoginApi = {
  /**
   * 发起 Bot 扫码登录，返回本次二维码。
   * 二维码内容用于页面展示（后端直出链接，前端统一用二维码组件渲染成图）。
   * @param {number|string} appId 要绑定的应用 ID：扫码后该机器人消息按此应用触发 embabel 回答
   * @returns {Promise<{ qrcode:string, qrcodeImgContent:string, expireAtMillis:number }>}
   *   qrcode 为后续轮询用的令牌，qrcodeImgContent 为二维码内容 / 链接。
   */
  qrcode: (appId) => http.post('/bot/qrcode', { appId }),

  /**
   * 轮询扫码结果（前端定时调用，直到 status 为确认或二维码过期）。
   * @param {{ qrcode:string }} data
   * @returns {Promise<{ status:string, isLoggedIn:boolean, ilinkBotId:string, ilinkUserId:string }>}
   */
  qrcodeStatus: (data) => http.post('/bot/qrcode/status', data),

  /**
   * 微信个人号（Bot 登录凭证）列表，按应用查询，不分页，最新绑定在前。
   * 只返回展示字段，不含 token。
   * @param {number|string} appId 放在路径中
   * @returns {Promise<Array<{ id:number, appId:number, scannerName:string,
   *   ilinkBotId:string, ilinkUserId:string, status:number, updatedAt:string }>>}
   *   status 为 1 表示已登录可用，0 表示未登录或凭证失效。
   */
  credentialList: (appId) => http.get(`/app/setting/botCredentialList/${appId}`),

  /**
   * 更新微信个人号凭证的扫码人（展示名）。
   * 按凭证 ID 覆盖 bot_credential.scanner_name 并刷新 updated_at；
   * 传空串表示清空。
   * @param {number|string} botCredentialId 放在路径中
   * @param {{ scannerName:string }} data 最长 64 字
   * @returns {Promise<void>} 无响应体；凭证不存在返回 404
   */
  updateBotCredentialScanner: (botCredentialId, data) =>
    http.post(`/app/setting/updateBotCredentialScanner/${botCredentialId}`, data),
}
