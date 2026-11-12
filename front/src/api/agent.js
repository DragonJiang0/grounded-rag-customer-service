import http from './http'
import { postSSE } from './sse'

/* ============================================================
   agent.js —— Agent 相关接口。
   ============================================================ */

export const agentApi = {
  /**
   * Agent 调试对话（SSE 流）：POST /agent/debug
   * 与应用调试（/app/setting/debug）的事件协议完全一致：
   *   status / message / done，前端复用同一套解析逻辑。
   * 请求体：{ agentName, taskId, prompt, question, memoryEnabled, memoryRounds, ops }
   *
   * @param {object} data      { agentName, taskId, prompt, question, memoryEnabled, memoryRounds }
   * @param {object} ops       模型参数 { temperature, topP, maxOutputTokens }
   * @param {object} handlers  { status, message, done }
   * @param {AbortSignal} signal
   */
  debug: (data, ops, handlers, signal) =>
    postSSE('/agent/debug', { ...data, ops }, handlers, signal),

  /** Agent 调试设置详情：GET /agent/setting/info/{agentName}，返回 { content }，未配置时 content 为空串 */
  settingInfo: (agentName) => http.get(`/agent/setting/info/${encodeURIComponent(agentName)}`),

  /** 保存 Agent 调试提示词：POST /agent/setting/savePrompt，请求体 { agentName, content } */
  saveSetting: (data) => http.post('/agent/setting/savePrompt', data),

  /** 删除该 Agent 已配置的提示词：无请求体、无返回数据，成功后回到「未配置提示词」状态 */
  deleteSetting: (agentName) => http.post(`/agent/setting/deletePrompt/${encodeURIComponent(agentName)}`),
}
