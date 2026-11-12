import http from './http'

export const appApi = {
  listAll: () => http.get('/apps/listAll'),

  /**
   * 应用详情：GET /apps/detail/{id}
   * 返回 AppDetailVO，除应用基础字段（含 multiAgentEnabled）外，
   * 还带 corpusGroups（语料分组对象数组，按绑定顺序）与 agents（Agent 名称数组）。
   */
  detail: (id) => http.get(`/apps/detail/${id}`),

  /**
   * 新增应用：POST /apps/add，请求体 JSON
   *   name             string*           ≤64
   *   description      string            ≤500，选填
   *   multiAgentEnabled boolean          默认 false
   *   corpusGroupIds   array<Long>*      至少 1 个
   *   agents           array<string>     开关为 true 时至少 1 个
   * 返回：无响应体，HTTP 200。
   * @param {{ name: string, description?: string, multiAgentEnabled?: boolean,
   *           corpusGroupIds: number[], agents?: string[] }} data
   */
  create: (data) => http.post('/apps/add', data),

  /**
   * 编辑应用：POST /apps/edit，请求体 JSON
   *   id               Long*            必填，其余字段与 add 相同
   * 注意：除 id 外的字段均为全量覆盖，不传即丢失；
   *       语料分组绑定与 Agent 绑定都按「先清空、再重建」整组替换。
   * 开关为 true 但 agents 为空时返回 HTTP 500，detail 为
   * 「启用多 Agent 后请至少选择一个 Agent」。
   * 返回：无响应体，HTTP 200。
   * @param {{ id: number, name: string, description?: string, multiAgentEnabled?: boolean,
   *           corpusGroupIds: number[], agents?: string[] }} data
   */
  update: (data) => http.post('/apps/edit', data),

  remove: (id) => http.post(`/apps/delete/${id}`),

  settingInfo: (appId) => http.get(`/app/setting/info/${appId}`),

  saveSetting: (data) => http.post('/app/setting/savePrompt', data),

  /** 删除该应用已配置的提示词：无请求体、无返回数据，成功后回到「未配置提示词」状态 */
  deleteSetting: (appId) => http.post(`/app/setting/deletePrompt/${appId}`),

  /**
   * 保存该应用的模型对话参数（生效范围含已发布渠道）。
   * 字段为 null 表示不覆盖该参数，回落到模型默认值。
   */
  saveChatOptions: (data) => http.post('/app/setting/saveChatOpts', data),

  /**
   * 保存该应用的对话记忆配置：只更新 memoryEnabled / memoryRounds，不影响模型参数。
   * 成功后无响应体，看 HTTP 200 即可。
   */
  saveChatMemory: (data) => http.post('/app/setting/saveChatMemory', data),
}
