<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import {
  ChatDotRound,
  Delete,
  Loading,
  Lock,
  Plus,
  Promotion,
  Search,
  Service,
  SwitchButton,
  User,
  VideoPause,
} from '@element-plus/icons-vue'
import { request, requestStream, setAccesskey } from '../utils/request'
import { getAppid, getToken } from '../utils/auth'
import { changePassword, logout } from '../api/account'
import NotFound from '../components/NotFound.vue'

// ---------- 模式配置 ----------
// mode: 'anonymous' 免密（/window/*） | 'logined' 带鉴权（/loginedwindow/*）
const props = defineProps({
  mode: {
    type: String,
    default: 'anonymous',
  },
  // 免密模式下从路由路径传入的 accesskey
  accesskey: {
    type: String,
    default: '',
  },
})

// 接口前缀：两套接口返回与逻辑完全一致，仅前缀不同
const API = props.mode === 'logined' ? '/loginedwindow' : '/window'

// 生成 UUID（优先使用浏览器原生实现）
function uuid() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ---------- accesskey（免密模式从路由路径获取；登录模式由 auth 注入） ----------
const accesskey = ref(props.accesskey || '')
if (props.mode === 'anonymous') {
  // 注入到请求工具，后续所有接口调用自动携带 Authorization: Bearer <accesskey>
  setAccesskey(accesskey.value)
} else {
  // 登录模式：从 localStorage 读取 token 注入请求头
  setAccesskey(getToken())
}

// 未拿到 accesskey 时展示 404 页面
const notFound = ref(false)
// 非 404 的加载失败详情（null 表示标准 404 文案）
const pageError = ref(null)
// 窗口信息校验完成前不渲染主体，避免闪烁
const ready = ref(false)

// ---------- 窗口信息 ----------
const assistantName = ref('基于AI的确定性可拓展智能问答系统')

async function fetchWindowInfo() {
  // 免密模式必须带 accesskey；登录模式必须已登录（localStorage 有 token）
  if (props.mode === 'anonymous' && !accesskey.value) {
    notFound.value = true
    return
  }
  if (props.mode === 'logined' && !getToken()) {
    // 未登录直接展示 404，不发起请求
    notFound.value = true
    return
  }
  try {
    // accesskey 已通过 request 工具以 Header 形式注入，无需再传参数
    const data = await request(`${API}/info`)
    if (!data || !data.name) throw new Error('窗口不存在')
    assistantName.value = data.name
    ready.value = true
    // 窗口校验通过后再拉取历史会话
    await fetchHistory()
  } catch (err) {
    console.error('[window/info]', err)
    // 请求层只清理失效凭证、不跳转，因此 401 也会落到这里按加载失败处理
    if (props.mode === 'anonymous' || err?.status === 404) {
      // 免密模式：查不到窗口一律按 404 处理；登录模式仅 404 视为页面不存在
      notFound.value = true
      return
    }
    // 登录模式的其他错误（500/403/网络等）：展示真实原因，不再误显示 404
    notFound.value = true
    const status = err?.status ? `（HTTP ${err.status}）` : ''
    pageError.value = {
      text: '页面加载失败',
      desc: `${err?.message || '服务异常，请稍后重试。'}${status}`,
    }
  }
}

onMounted(() => {
  fetchWindowInfo()
})

// ---------- 退出登录（仅登录模式展示） ----------
const router = useRouter()

async function handleLogout() {
  const appkey = getAppid()
  await logout()
  // 登出后回到登录页，保留 appkey 便于重新登录
  router.replace(appkey ? `/login/${appkey}` : '/login')
}

/** 账号设置下拉菜单：修改密码 / 退出登录 */
function handleAccountCommand(command) {
  if (command === 'password') {
    openPwdDialog()
  } else if (command === 'logout') {
    handleLogout()
  }
}

// ---------- 修改密码（仅登录模式展示） ----------
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
/** 修改密码表单：与新密码长度约束 6-128 字符对应 */
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const pwdFormRef = ref(null)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '新密码长度需为 6-128 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

/** 重置表单并打开弹窗 */
function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdVisible.value = true
  nextTick(() => pwdFormRef.value?.clearValidate())
}

async function submitPwdChange() {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  // 新旧密码相同由后端校验，前端提前提示减少一次请求
  if (pwdForm.oldPassword === pwdForm.newPassword) {
    ElMessage.error('新密码不能与原密码相同')
    return
  }
  pwdSubmitting.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    pwdVisible.value = false
    ElMessage.success('密码修改成功')
  } catch (err) {
    // 400 原密码错误/新旧相同/参数不合法，404 账号不存在，401 登录态失效（仅提示，不跳转）
    ElMessage.error(err?.message || '密码修改失败，请稍后重试')
    console.error('[loginedwindow/changePassword]', err)
  } finally {
    pwdSubmitting.value = false
  }
}

// ---------- 对话历史 ----------
const conversations = reactive([])

const activeId = ref(null)
const keyword = ref('')

/** 解析 history JSON 字符串为消息数组，失败返回空数组 */
function parseHistory(history) {
  if (!history) return []
  if (Array.isArray(history)) return history
  try {
    const data = JSON.parse(history)
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

/** 解析后端时间字符串为 Date，失败返回 null */
function parseDate(text) {
  if (!text) return null
  const date = new Date(String(text).replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? null : date
}

/** 记录列表展示时间：近 7 天用相对描述，超过则用日期 */
function formatListTime(text) {
  const date = parseDate(text)
  if (!date) return ''
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const dayMs = 24 * 60 * 60 * 1000
  const p = (n) => String(n).padStart(2, '0')
  const timeText = `${p(date.getHours())}:${p(date.getMinutes())}`

  // 未来时间直接按日期展示
  if (diff < 0) {
    return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
  }
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} 分钟前`
  if (diff < dayMs) return `${Math.floor(diff / (60 * 60 * 1000))} 小时前`

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayDiff = Math.round((startOfToday - startOfDate) / dayMs)
  if (dayDiff === 1) return `昨天 ${timeText}`
  if (dayDiff < 7) return `${dayDiff} 天前`
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
}

/** 把后端返回的历史记录映射为页面会话结构 */
function mapHistoryRow(row) {
  const list = parseHistory(row.history)
  return {
    id: row.conversationId || `history-${row.id}`,
    title: row.title || '未命名对话',
    time: formatListTime(row.createdAt),
    rawTime: row.createdAt || '',
    createdId: row.id,
    taskId: row.conversationId || uuid(),
    messages: list.map((m, i) => {
      const isUser = m.role === 'user'
      const content = m.content ?? ''
      return {
        id: `${row.id}-${i}`,
        role: isUser ? 'user' : 'assistant',
        content,
        // 助手消息预渲染 markdown，历史记录直接复用，避免模板里重复解析
        html: isUser ? '' : toHtml(content),
        time: m.createTime || m.createdAt || '',
      }
    }),
  }
}

/** 拉取当前窗口的免密历史会话（channel=2），按时间倒序 */
async function fetchHistory() {
  try {
    const data = await request(`${API}/history`)
    const list = Array.isArray(data) ? data : []
    const mapped = list.map(mapHistoryRow)
    conversations.length = 0
    mapped.forEach((item) => conversations.push(item))
    // 默认选中最近一条
    activeId.value = conversations[0]?.id ?? null
    scrollToBottom()
  } catch (err) {
    // 历史拉取失败不阻断主流程
    console.error('[window/history]', err)
  }
}

const filteredConversations = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return conversations
  return conversations.filter((c) => c.title.includes(kw))
})

const activeConversation = computed(
  () => conversations.find((c) => c.id === activeId.value) || null
)

const messages = computed(() => activeConversation.value?.messages ?? [])

// ---------- 输入 ----------
const input = ref('')
const sending = ref(false)
const bodyRef = ref(null)
/** 当前事件流的 AbortController，切换/卸载时中断 */
let controller = null

onBeforeUnmount(() => {
  controller?.abort()
  controller = null
})

/** markdown 文本 -> html，空内容返回空串 */
function toHtml(text) {
  if (!text) return ''
  return marked.parse(String(text))
}

/** 从事件数据里取内容字段，兼容 text / content / message / answer 等命名 */
function pickText(data) {
  if (data == null) return ''
  if (typeof data === 'string') return data
  return data.text ?? data.content ?? data.message ?? data.answer ?? data.result ?? ''
}

function scrollToBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function selectConversation(id) {
  activeId.value = id
  scrollToBottom()
}

function createConversation() {
  const taskId = uuid()
  const conv = {
    id: taskId,
    title: '新的对话',
    time: '刚刚',
    messages: [],
    taskId,
  }
  conversations.unshift(conv)
  activeId.value = taskId
  input.value = ''
  scrollToBottom()
  return conv
}

/** 删除进行中的会话 id 集合，避免重复点击 */
const deletingIds = reactive(new Set())

/**
 * 删除历史会话：先调后端软删除接口，成功后再移除本地记录。
 * 接口：POST {API}/history/delete，body { conversationId }，成功返回空响应体。
 * 应用归属由后端从当前窗口（Authorization: Bearer <windowKey>）还原，前端不再传 appKey；
 * 后端按「appId + conversationId」把该会话的所有分段软删除（customer_deleted=true），
 * 因此 conversationId 用会话标识 taskId，而不是单条记录主键。
 * 本地新建、尚未落库的会话没有 conversationId，只做本地移除。
 */
async function removeConversation(id) {
  const index = conversations.findIndex((c) => c.id === id)
  if (index === -1) return
  const conv = conversations[index]
  // 已落库的历史会话才有会话标识，需先请求后端软删除
  const conversationId = conv?.createdId != null ? conv?.taskId : null
  if (conversationId) {
    if (deletingIds.has(id)) return
    deletingIds.add(id)
    try {
      await request(`${API}/history/delete`, {
        method: 'POST',
        body: { conversationId },
      })
    } catch (err) {
      // 删除失败不摘除本地记录，避免与服务端状态不一致
      const msg = err?.message || '删除失败，请稍后重试'
      ElMessage.error(msg)
      console.error('[window/history/delete]', err)
      return
    } finally {
      deletingIds.delete(id)
    }
  }
  conversations.splice(index, 1)
  if (activeId.value === id) {
    activeId.value = conversations[Math.min(index, conversations.length - 1)]?.id ?? null
  }
  ElMessage.success('已删除对话')
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || sending.value) return

  let conv = activeConversation.value
  if (!conv) {
    conv = createConversation()
  }
  if (conv.title === '新的对话') conv.title = text.slice(0, 12)
  // 无 taskId 时补一个（历史/异常场景兜底）
  if (!conv.taskId) conv.taskId = uuid()

  conv.messages.push({ id: Date.now(), role: 'user', content: text })
  input.value = ''
  scrollToBottom()

  // 助手占位消息
  sending.value = true
  const replyId = Date.now() + 1
  conv.messages.push({
    id: replyId,
    role: 'assistant',
    content: '',
    html: '',
    loading: true,
    status: '',
  })
  // 必须取回 push 后数组里的响应式代理对象来改；
  // 直接用 push 前的原始对象修改会绕过 Vue 响应式，界面不会更新。
  const reply = conv.messages[conv.messages.length - 1]
  scrollToBottom()

  // 当前事件流的 AbortController，用于「停止」
  const ac = new AbortController()
  controller = ac

  try {
    await requestStream(`${API}/chat/stream`, {
      body: { message: text, taskId: conv.taskId },
      signal: ac.signal,
      handlers: {
        // 开场：回传会话标识 taskId
        taskId: (data) => {
          const id = pickText(data)
          if (id) conv.taskId = id
        },
        // 阶段提示：仅更新状态文案，不覆盖正文
        status: (data) => {
          reply.status = pickText(data) || reply.status
          reply.loading = true
          scrollToBottom()
        },
        // 流式累积正文：后端下发增量片段，前端累加到 content 后整体渲染（打字机效果）
        message: (data) => {
          const chunk = pickText(data)
          if (chunk) {
            reply.content += chunk
            reply.html = toHtml(reply.content)
          }
          scrollToBottom()
        },
        // 最终答复：整体替换 message 阶段的内容（done 无内容时保留已累积的渲染）
        done: (data) => {
          const finalText = pickText(data)
          if (finalText) {
            reply.content = finalText
            reply.html = toHtml(finalText)
          }
          reply.loading = false
          reply.status = ''
          scrollToBottom()
        },
        // 异常
        error: (data) => {
          const msgText = pickText(data) || '对话出现异常'
          reply.content = msgText
          reply.html = toHtml(msgText)
          reply.loading = false
          reply.status = ''
          reply.error = true
          ElMessage.error(msgText)
          scrollToBottom()
        },
      },
    })
    // 流正常结束但没有任何内容时，兜底文案
    if (!reply.html) {
      reply.content = '暂无返回内容'
      reply.html = toHtml(reply.content)
    }
  } catch (err) {
    // 主动停止导致的 AbortError 不算错误
    if (err?.name !== 'AbortError') {
      reply.error = true
      const failText = '对话请求失败，请稍后重试。'
      reply.content = failText
      reply.html = toHtml(failText)
      ElMessage.error('对话请求失败')
      console.error('[window/chat/stream]', err)
    }
  } finally {
    reply.loading = false
    reply.status = ''
    if (controller === ac) controller = null
    sending.value = false
    scrollToBottom()
  }
}

/** 停止当前流式对话：中断事件流并摘掉加载状态，保留已渲染内容 */
function stopMessage() {
  if (controller) {
    controller.abort()
    controller = null
  }
  const conv = activeConversation.value
  const last = conv?.messages?.[conv.messages.length - 1]
  if (last?.role === 'assistant') last.loading = false
  sending.value = false
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <!-- 校验中 -->
  <div v-if="!ready && !notFound" class="page-loading">
    <el-icon class="is-loading"><Loading /></el-icon>
  </div>

  <!-- 未拿到 accesskey 或查询不到记录：404 页面 -->
  <NotFound
    v-else-if="notFound"
    :text="pageError?.text"
    :desc="pageError?.desc"
  />

  <div v-else class="chat-pc">
    <!-- 左侧：对话历史 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-icon class="logo"><ChatDotRound /></el-icon>
        <span class="logo-text">{{ assistantName }}</span>
      </div>

      <el-input
        v-model="keyword"
        class="search"
        placeholder="搜索对话"
        :prefix-icon="Search"
        clearable
      />

      <div class="history-title">对话历史</div>

      <el-scrollbar class="history-list">
        <el-empty
          v-if="filteredConversations.length === 0"
          description="暂无对话"
          :image-size="70"
        />
        <div
          v-for="item in filteredConversations"
          :key="item.id"
          class="history-item"
          :class="{ active: item.id === activeId }"
          @click="selectConversation(item.id)"
        >
          <div class="history-main">
            <div class="history-name">{{ item.title }}</div>
            <div class="history-time">{{ item.time }}</div>
          </div>
          <el-button
            class="history-del"
            type="danger"
            link
            :icon="Delete"
            :loading="deletingIds.has(item.id)"
            @click.stop="removeConversation(item.id)"
          />
        </div>
      </el-scrollbar>

      <div class="sidebar-footer">
        <el-button type="primary" :icon="Plus" class="new-btn" @click="createConversation">
          新建对话
        </el-button>
      </div>
    </aside>

    <!-- 右侧：对话主区 -->
    <main class="main">
      <header class="main-header">
        <div class="header-main">
          <div class="main-title">{{ activeConversation?.title || '未选择对话' }}</div>
          <div class="main-subtitle">基于AI的确定性可拓展智能问答系统</div>
        </div>
        <el-dropdown v-if="mode === 'logined'" trigger="click" @command="handleAccountCommand">
          <el-button class="logout-btn" link :icon="SwitchButton">
            账号设置
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <div ref="bodyRef" class="message-body">
        <el-empty
          v-if="messages.length === 0"
          description="开始你的第一个问题吧"
          :image-size="120"
        />
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="msg.role"
        >
          <div class="message-row" :class="msg.role">
            <div class="msg-avatar">
              <el-icon :size="23">
                <Service v-if="msg.role === 'assistant'" />
                <User v-else />
              </el-icon>
            </div>
            <div class="bubble" :class="{ 'bubble-error': msg.error }">
              <span v-if="msg.loading" class="loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                {{ msg.status || '正在思考…' }}
              </span>
              <!-- 加载提示与正文同时展示：message 增量边收边渲染，done 到达后整体替换 -->
              <template v-if="msg.role === 'assistant'">
                <div v-if="msg.html" class="md-body" v-html="msg.html"></div>
              </template>
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>
          <div v-if="msg.time" class="msg-time">{{ msg.time }}</div>
        </div>
      </div>

      <footer class="composer">
        <div class="composer-box">
          <el-input
            v-model="input"
            class="composer-input"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 5 }"
            resize="none"
            placeholder="请输入你的问题，Enter 发送 / Shift + Enter 换行"
            @keydown="handleKeydown"
          />
          <el-button
            v-if="sending"
            class="composer-send"
            type="danger"
            circle
            :icon="VideoPause"
            @click="stopMessage"
          />
          <el-button
            v-else
            class="composer-send"
            type="primary"
            circle
            :icon="Promotion"
            :disabled="!input.trim()"
            @click="sendMessage"
          />
        </div>
      </footer>
    </main>

    <!-- 修改密码 -->
    <el-dialog
      v-model="pwdVisible"
      title="修改密码"
      width="380px"
      :close-on-click-modal="!pwdSubmitting"
      :close-on-press-escape="!pwdSubmitting"
      :show-close="!pwdSubmitting"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-position="top"
        @submit.prevent="submitPwdChange"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（6-128 个字符）"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            autocomplete="new-password"
            @keydown.enter="submitPwdChange"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="pwdSubmitting" @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPwdChange">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-pc {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

/* ---------- 校验中 ---------- */
.page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;
  font-size: 28px;
  color: #409eff;
}

/* ---------- 侧边栏 ---------- */
/* 尺寸整体为原值的 120%：宽度、内边距、列表项与字号同步放大 */
.sidebar {
  display: flex;
  flex-direction: column;
  width: 336px;
  flex-shrink: 0;
  padding: 19px;
  background: #ffffff;
  border-right: 1px solid #e4e7ed;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 19px;
}

.logo {
  font-size: 26px;
  color: #409eff;
}

.logo-text {
  font-size: 19px;
  font-weight: 600;
  color: #303133;
}

.search {
  margin-bottom: 19px;
}

.history-title {
  font-size: 16px;
  color: #909399;
  margin-bottom: 10px;
}

.history-list {
  flex: 1;
  min-height: 0;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 7px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.history-item:hover {
  background: #f2f6fc;
}

.history-item.active {
  background: #ecf5ff;
}

.history-main {
  flex: 1;
  min-width: 0;
}

.history-name {
  font-size: 17px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item.active .history-name {
  color: #409eff;
  font-weight: 600;
}

.history-time {
  font-size: 14px;
  color: #c0c4cc;
  margin-top: 2px;
}

.history-del {
  visibility: hidden;
}

.history-item:hover .history-del {
  visibility: visible;
}

.sidebar-footer {
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.new-btn {
  width: 100%;
  height: 46px;
  font-size: 17px;
  border-radius: 999px;
}

/* ---------- 主区 ---------- */
.main {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.header-main {
  min-width: 0;
}

.logout-btn {
  flex-shrink: 0;
  font-size: 13px;
  color: #909399;
}

.logout-btn:hover {
  color: #f56c6c;
}

.main-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.main-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.message-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 29px 29px 144px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 936px;
}

.message-item.user {
  align-self: flex-end;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 13px;
}

.message-row.user {
  flex-direction: row-reverse;
}

/* 时间显示在气泡下方，与气泡对齐：用户侧靠右 */
.msg-time {
  font-size: 19px;
  color: #a8b0bd;
  padding: 0 5px;
}

.message-item.user .msg-time {
  text-align: right;
}

/* 头像：浅色底 + 浅色描边，图标为深色；机器人/用户仅底色与描边色不同 */
.msg-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 43px;
  height: 43px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #f7f9fc;
  border: 1px solid #e4e7ed;
  color: #2b323d;
}

.message-row.assistant .msg-avatar {
  background: #ecf5ff;
  border-color: #c9ddff;
}

.message-row.user .msg-avatar {
  background: #f2f5fa;
  border-color: #dce4ee;
}

/* 气泡：与头像一致走浅色风格，仅底色/描边区分角色 */
.bubble {
  padding: 13px 17px;
  border-radius: 12px;
  font-size: 17px;
  line-height: 1.65;
  word-break: break-word;
  background: #f7f9fc;
  color: #2b323d;
  border: 1px solid #e4e7ed;
}

/* 机器人气泡：浅蓝底 + 浅蓝描边，左上角小圆角贴近头像 */
.message-row.assistant .bubble {
  background: #f2f7ff;
  border-color: #dbe8ff;
  border-top-left-radius: 5px;
}

/* 用户气泡为纯文本，保留换行；助手气泡走 markdown 渲染，不能用 pre-wrap */
.message-row.user .bubble {
  white-space: pre-wrap;
  background: #f2f5fa;
  border-color: #dce4ee;
  border-top-right-radius: 5px;
}

.bubble-error {
  background: #fef0f0;
  border-color: #fde2e2;
  color: #f56c6c;
}

.loading {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #909399;
}

/* 加载提示与流式正文同时展示时的间距 */
.loading + .md-body {
  margin-top: 8px;
}

/* ---------- markdown 渲染 ---------- */
.md-body {
  font-size: 17px;
  line-height: 1.7;
  color: inherit;
  word-break: break-word;
}

.md-body :deep(> *:first-child) {
  margin-top: 0;
}

.md-body :deep(> *:last-child) {
  margin-bottom: 0;
}

.md-body :deep(p) {
  margin: 0 0 12px;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3),
.md-body :deep(h4) {
  margin: 17px 0 10px;
  font-weight: 700;
  line-height: 1.4;
}

.md-body :deep(h1) { font-size: 22px; }
.md-body :deep(h2) { font-size: 19px; }
.md-body :deep(h3) { font-size: 18px; }

.md-body :deep(ul),
.md-body :deep(ol) {
  margin: 0 0 12px;
  padding-left: 24px;
}

.md-body :deep(li) {
  margin: 4px 0;
}

.md-body :deep(code) {
  background: #f0f2f5;
  color: #c7254e;
  border-radius: 5px;
  padding: 1px 6px;
  font-size: 15px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.md-body :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 14px 17px;
  margin: 0 0 12px;
  overflow: auto;
}

.md-body :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
  font-size: 15px;
}

.md-body :deep(blockquote) {
  margin: 0 0 12px;
  padding: 5px 14px;
  border-left: 4px solid #dcdfe6;
  background: #f7f8fa;
  color: #909399;
}

.md-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.md-body :deep(a:hover) {
  text-decoration: underline;
}

.md-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 12px;
  font-size: 16px;
}

.md-body :deep(th),
.md-body :deep(td) {
  border: 1px solid #ebeef5;
  padding: 7px 12px;
  text-align: left;
}

.md-body :deep(th) {
  background: #f5f7fa;
  font-weight: 700;
}

.md-body :deep(hr) {
  border: none;
  border-top: 1px solid #ebeef5;
  margin: 12px 0;
}

.composer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 0 29px 29px;
  pointer-events: none;
}

.composer-box {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  width: 100%;
  max-width: 936px;
  padding: 10px 10px 10px 19px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 29px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  pointer-events: auto;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.composer-box:focus-within {
  border-color: #409eff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.18);
}

.composer-input {
  flex: 1;
  min-width: 0;
}

.composer-input :deep(.el-textarea__inner) {
  padding: 7px 0;
  border: none;
  box-shadow: none;
  background: transparent;
  font-size: 17px;
  line-height: 1.6;
  overflow-y: hidden;
  scrollbar-width: none;
}

.composer-input :deep(.el-textarea__inner)::-webkit-scrollbar {
  display: none;
}

.composer-send {
  flex-shrink: 0;
  width: 43px;
  height: 43px;
  font-size: 19px;
}
</style>
