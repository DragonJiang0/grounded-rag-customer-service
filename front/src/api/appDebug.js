import { postSSE } from './sse'

export const appDebugApi = {
  /**
   * 调试对话（SSE 流）：POST /app/setting/debug
   * 传入提示词与问题，后端校验占位符后返回事件流：
   *   status  开始调试 / 调试完成
   *   message 收到的问题、渲染后的提示词
   *   done    调试完成（携带最终渲染结果）
   * 校验失败返回普通 JSON 错误，由 postSSE 抛错。
   *
   * 模型参数 ops 与提示词/问题同级单独传递，作为请求体的独立属性：
   *   { appId, taskId, prompt, question, memoryEnabled, memoryRounds, ops }
   * 未启用的参数为 null，表示不覆盖、回落到模型默认值。
   *
   * memoryEnabled 表示是否开启会话记忆，memoryRounds 为记忆保留的问答轮数；
   * 未开启记忆时 memoryRounds 传 null，由服务端按默认行为处理。
   *
   * @param {object} data      { appId, taskId, prompt, question, memoryEnabled, memoryRounds }
   * @param {object} ops       模型参数 { temperature, topP, maxOutputTokens }
   * @param {object} handlers  { status, message, done }
   * @param {AbortSignal} signal
   */
  debug: (data, ops, handlers, signal) =>
    postSSE('/app/setting/debug', { ...data, ops }, handlers, signal),
}
