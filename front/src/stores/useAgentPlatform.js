/* ============================================================
   useAgentPlatform.js —— 自定义 Agent：已注册 Agent 列表。
   数据来自 manage 的 GET /agent/list（经 gRPC 读 portal 的 AgentPlatform），
   只含名称 / 描述 / goals / actions，供管理台展示与绑定选择。
   ============================================================ */

import { computed, ref } from 'vue'
import { agentPlatformApi } from '@/api/agentPlatform'
import { errorMessage } from '@/api/http'

/** 后端返回的 Agent 数组（直接是数组，无包装） */
const agents = ref([])
const loading = ref(false)

const total = computed(() => agents.value.length)

/** 归一化：后端可能返回 null，goals / actions 缺失时补空数组 */
function normalize(list) {
  return (Array.isArray(list) ? list : []).map(a => ({
    name: a?.name || '',
    description: a?.description || '',
    goals: Array.isArray(a?.goals) ? a.goals : [],
    actions: Array.isArray(a?.actions) ? a.actions : [],
  }))
}

async function load() {
  loading.value = true
  try {
    // 响应体即数组，不做 code/message/data 解包
    agents.value = normalize(await agentPlatformApi.list())
  } catch (e) {
    agents.value = []
    ElMessage.error(errorMessage(e, '加载 Agent 列表失败'))
  } finally {
    loading.value = false
  }
}

/** 绑定选择用的下拉选项：值为 Agent 名称（元数据里没有 id） */
const agentOptions = computed(() => agents.value.map(a => ({ label: a.name, value: a.name })))

export function useAgentPlatform() {
  return { agents, agentOptions, total, loading, load }
}
