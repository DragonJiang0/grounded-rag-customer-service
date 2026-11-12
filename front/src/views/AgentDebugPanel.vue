<!-- ============================================================
    AgentDebugPanel.vue —— Agent 调试面板（完全独立实现，不复用应用调试）。
    左右分栏：左侧提示词（可保存/删除，持久化到 agent_prompt 表），右侧对话框。
    相对应用调试的差异（均为 Agent 场景专属）：
      · 不提供模型参数 / 记忆开关，请求体不含这些参数；
      · 支持多轮上下文：同一 taskId 内的历史问答随请求下发，可清空会话；
      · 事件流增量累积后走 markdown 渲染（支持 GFM 表格）。
    ============================================================ -->
<template>
  <div class="agent-debug">
    <div class="agent-debug__layout">
      <!-- 左：提示词 -->
      <section class="agent-pane agent-pane--prompt">
        <header class="agent-pane__head">
          <span class="agent-pane__icon"><el-icon :size="17"><MagicStick /></el-icon></span>
          <span class="agent-pane__title">提示词</span>

          <el-tooltip placement="top" effect="light">
            <template #content>
              <div class="tip-body">
                提示词可为空，如果为空，系统直接返回语料，不走大模型。
              </div>
            </template>
            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
          </el-tooltip>

          <span class="agent-pane__meta">{{ prompt.length }} 字符</span>
        </header>

        <el-input v-model="prompt" type="textarea" resize="none" class="prompt-box" />

        <footer class="agent-pane__foot">
          <div class="pane-actions">
            <el-button type="danger" size="large" :loading="deleting" :disabled="saving"
                       @click="deletePrompt">
              <el-icon class="btn-icon"><Delete /></el-icon>删除
            </el-button>
            <el-button type="primary" size="large" :loading="saving" @click="savePrompt">
              <el-icon class="btn-icon"><Check /></el-icon>保存
            </el-button>
          </div>
        </footer>
      </section>

      <!-- 右：对话框 -->
      <section class="agent-pane agent-pane--chat">
        <div ref="chatBody" class="agent-chat__body">
          <MessageBubble v-for="(m, i) in messages" :key="i" :role="m.role"
                         :bot-icon="Service">
            <template v-if="m.role === 'user'">{{ m.text }}</template>
            <template v-else>
              <div v-if="m.loading" class="agent-chat__status">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <!-- Agent 侧回答走 markdown 渲染，支持加粗、表格等 GFM 语法 -->
              <div v-if="m.md" class="md-body" v-html="m.md"></div>
              <div v-if="m.error" class="agent-chat__error">{{ m.error }}</div>
            </template>
          </MessageBubble>
        </div>

        <div class="agent-chat__input">
          <el-input v-model="input" placeholder="输入用户问题，回车发送…"
                    clearable :disabled="loading" @keyup.enter="send">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button v-if="loading" type="danger" size="large" class="send-btn" @click="stop">
            <el-icon class="btn-icon"><VideoPause /></el-icon>停止
          </el-button>
          <el-button v-else type="primary" size="large" class="send-btn" @click="send">
            <el-icon class="btn-icon"><Promotion /></el-icon>发送
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Search, Promotion, VideoPause, MagicStick, QuestionFilled, Check, Delete,
  Service, Loading,
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import MessageBubble from '@/components/MessageBubble.vue'
import { agentApi } from '@/api/agent'
import { errorMessage } from '@/api/http'

const props = defineProps({
  /** 要调试的 Agent 名称（运行时唯一标识） */
  agentName: { type: String, default: '' },
})

/** 提示词：进入页面后以接口返回为准 */
const prompt = ref('')
const input = ref('')
/** 本次页面会话的任务 ID：进入页面时用 uuid 生成一次，全程调试请求共用 */
const taskId = ref('')
const messages = ref([])
const chatBody = ref(null)
const saving = ref(false)
const deleting = ref(false)
/** 调试请求进行中：输入与发送禁用，避免并发事件流串消息 */
const loading = ref(false)
/** 当前事件流的 AbortController，切换/卸载时中断 */
let controller = null

/** 生成 uuid：优先用原生 crypto.randomUUID，非安全上下文下退化为时间戳 + 随机数 */
function createTaskId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`
}

function scrollBottom() {
  nextTick(() => {
    const el = chatBody.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

/** markdown 文本 -> html，空内容返回空串（不渲染气泡正文） */
function toHtml(text) {
  if (!text) {
    return ''
  }
  return marked.parse(String(text))
}

/** 从事件数据里取内容字段，兼容 text / content / answer 等命名 */
function pickText(data) {
  if (data == null) {
    return ''
  }
  if (typeof data === 'string') {
    return data
  }
  return data.text ?? data.content ?? data.message ?? data.answer ?? data.result ?? ''
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) {
    return
  }
  if (!props.agentName) {
    return ElMessage.warning('缺少 Agent 名称，无法调试')
  }

  // 发送前的历史问答，作为本轮上下文一起下发
  const history = messages.value
    .filter(m => !m.loading && !m.error)
    .map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.raw ?? m.text }))

  messages.value.push({ role: 'user', text })
  input.value = ''

  messages.value.push({ role: 'bot', loading: true, raw: '', md: '', error: '' })
  const bot = messages.value[messages.value.length - 1]
  loading.value = true
  scrollBottom()

  const ac = new AbortController()
  controller = ac
  const finish = () => {
    bot.loading = false
    loading.value = false
    if (controller === ac) {
      controller = null
    }
    scrollBottom()
  }

  try {
    await agentApi.debug(
      {
        agentName: props.agentName,
        taskId: taskId.value,
        prompt: prompt.value,
        question: text,
        // 多轮上下文：Agent 场景下由前端显式携带历史
        history,
      },
      {},
      {
        status: () => {
          scrollBottom()
        },
        message: (data) => {
          const content = pickText(data)
          if (content) {
            bot.raw += content
            bot.md = toHtml(bot.raw)
          }
          scrollBottom()
        },
        done: (data) => {
          const content = pickText(data)
          if (content) {
            bot.raw = content
            bot.md = toHtml(content)
          }
          scrollBottom()
        },
      },
      ac.signal,
    )

    if (!bot.md) {
      bot.raw = '暂无返回内容'
      bot.md = toHtml(bot.raw)
    }
  } catch (e) {
    // 停止 / 清空导致的主动中断不算错误
    if (e?.name !== 'AbortError') {
      bot.error = errorMessage(e, '调试请求失败')
      ElMessage.error(bot.error)
    }
  } finally {
    finish()
  }
}

/** 停止当前调试：立即中断事件流并摘掉加载图标，保留已渲染内容 */
function stop() {
  if (!controller) {
    return
  }
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'bot') {
    last.loading = false
  }
  loading.value = false
  controller.abort()
}

/** 进入页面时按 agentName 拉取已配置的提示词，未配置则保持为空 */
async function loadPrompt() {
  if (!props.agentName) {
    return
  }
  try {
    const data = await agentApi.settingInfo(props.agentName)
    prompt.value = data?.content || ''
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载提示词失败'))
  }
}

/** 删除提示词：清空该 Agent 已配置的提示词，成功后本地编辑框同步置空 */
async function deletePrompt() {
  if (!props.agentName) {
    return ElMessage.warning('缺少 Agent 名称，无法删除')
  }

  try {
    await ElMessageBox.confirm(
      '删除后该 Agent 将不再使用提示词包装答案，问答时直接返回命中语料的标准答案。',
      '确认删除提示词？',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  deleting.value = true
  try {
    await agentApi.deleteSetting(props.agentName)
    prompt.value = ''
    ElMessage.success('提示词已删除')
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除提示词失败'))
  } finally {
    deleting.value = false
  }
}

/** 保存提示词：按 agentName 保存，同一 Agent 重复保存会覆盖更新 */
async function savePrompt() {
  if (!props.agentName) {
    return ElMessage.warning('缺少 Agent 名称，无法保存')
  }

  saving.value = true
  try {
    await agentApi.saveSetting({ agentName: props.agentName, content: prompt.value })
    ElMessage.success('提示词已保存')
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存提示词失败'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  taskId.value = createTaskId()
  await loadPrompt()
  scrollBottom()
})

onBeforeUnmount(() => {
  controller?.abort()
  controller = null
})
</script>

<style scoped>
/* 撑满父级内容区：用视口高度推算，父级无确定高度时百分比无法解析 */
.agent-debug {
  display: flex;
  align-items: stretch;
  height: calc((100vh - 64px - 48px - 40px - 86px) * 0.96);
  min-height: 540px;
}
.agent-debug__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 18px;
  width: 100%;
  height: 100%;
  min-height: 0;
}
@media (max-width: 1080px) {
  .agent-debug__layout {
    grid-template-columns: 1fr;
  }
}

/* ---------- 左右两栏外框 ---------- */
.agent-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 16px;
  background: var(--brand-soft-2);
}
.agent-pane--chat {
  background: #fff;
}

.agent-pane__head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--line);
  font-size: 15px;
}
.agent-pane__icon {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: var(--radius-sm);
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
}
.agent-pane__title {
  font-weight: 700;
  color: var(--text);
  flex: 0 0 auto;
}
.agent-pane__meta {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
}
.tip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--muted);
  cursor: help;
  transition: color .15s;
}
.tip-icon:hover {
  color: var(--brand);
}
.tip-icon :deep(svg) {
  width: 15px;
  height: 15px;
}
.tip-body {
  max-width: 260px;
  font-size: 13px;
  line-height: 1.7;
  color: #3D4B60;
}

/* ---------- 左：提示词 ---------- */
.prompt-box {
  flex: 1;
  min-height: 0;
  margin-top: 15px;
}
.prompt-box :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 320px;
  border-radius: var(--radius);
  font-size: 13.5px;
  line-height: 1.75;
  font-family: inherit;
  padding: 14px;
}
.agent-pane__foot {
  margin-top: 13px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.pane-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

/* ---------- 右：对话 ---------- */
.agent-chat__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.agent-chat__status {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  font-size: 13px;
}
.agent-chat__status + .md-body {
  margin-top: 10px;
}

/* ---------- markdown 渲染（与应用调试页一致，支持 GFM 表格） ---------- */
.md-body {
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--text);
  word-break: break-word;
}
.md-body :deep(> *:first-child) { margin-top: 0; }
.md-body :deep(> *:last-child) { margin-bottom: 0; }
.md-body :deep(p) { margin: 0 0 10px; }
.md-body :deep(strong) { font-weight: 700; }
.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3),
.md-body :deep(h4) {
  margin: 14px 0 8px;
  font-weight: 700;
  line-height: 1.4;
}
.md-body :deep(h1) { font-size: 18px; }
.md-body :deep(h2) { font-size: 16px; }
.md-body :deep(h3) { font-size: 15px; }
.md-body :deep(ul),
.md-body :deep(ol) { margin: 0 0 10px; padding-left: 20px; }
.md-body :deep(li) { margin: 3px 0; }
.md-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--line);
  margin: 12px 0;
}
.md-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 10px;
  font-size: 13px;
}
.md-body :deep(th),
.md-body :deep(td) {
  border: 1px solid var(--line);
  padding: 6px 10px;
  text-align: left;
}
.md-body :deep(th) {
  background: var(--brand-soft-2);
  font-weight: 700;
}
.agent-chat__error {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius);
  background: #FFF4F4;
  border: 1px solid #FBD9D9;
  color: var(--danger, #E45656);
  font-size: 13px;
  line-height: 1.7;
}
.agent-chat__input {
  border-top: 1px solid var(--line);
  padding-top: 13px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.agent-chat__input :deep(.el-input__wrapper) {
  height: 40px;
  padding: 0 14px;
}
.agent-chat__input :deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
  line-height: 40px;
}
.agent-chat__input :deep(.el-input__prefix) {
  display: flex;
  align-items: center;
}
.send-btn {
  flex: 0 0 auto;
  height: 40px;
}
</style>
