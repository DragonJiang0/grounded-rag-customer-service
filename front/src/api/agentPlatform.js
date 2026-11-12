import http from './http'

export const agentPlatformApi = {
  // 查询已注册 Agent 列表（manage 服务）
  //   GET /agent/list
  //   无参数，Authorization 由请求拦截器统一注入；
  //   经 gRPC 从 portal 的 AgentPlatform 读取已被注解自动注册的 Agent 元数据；
  //   成功时无 code/message/data 包装，响应体直接是数组：
  //   [{ name, description, goals: [{ name, description }], actions: [{ name, description }] }]
  list: () => http.get('/agent/list'),
}
