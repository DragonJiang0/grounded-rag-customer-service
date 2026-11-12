/* ============================================================
   useAgent.js —— Agent 设置：助手身份与话术。
   后端暂无对应接口，这里仅维护前端本地状态与默认值。
   ============================================================ */

import { reactive, ref } from 'vue'

/** 助手设置，含默认值，供气泡头像与兜底话术使用 */
const agent = reactive({
  name: '智答助手',
  avatar: '智',
  welcome: '你好，我是智能客服助手，有什么可以帮您？',
  fallback: '抱歉，我暂时没有找到相关答案，已为您转接人工客服。',
})
const loading = ref(false)

/** 本地状态无需拉取，保留空实现以兼容调用方 */
async function load() {
  return agent
}

/** 本地保存：暂无接口，仅做校验与提示 */
async function save() {
  if (!agent.name.trim()) {
    return ElMessage.warning('请输入助手名称')
  }
  if (!agent.avatar.trim()) {
    return ElMessage.warning('请输入头像文字')
  }
  if (!agent.welcome.trim()) {
    return ElMessage.warning('请输入欢迎语')
  }
  if (!agent.fallback.trim()) {
    return ElMessage.warning('请输入兜底回复')
  }
  ElMessage.success('Agent 设置已保存')
}

export function useAgent() {
  return { agent, loading, load, save }
}
