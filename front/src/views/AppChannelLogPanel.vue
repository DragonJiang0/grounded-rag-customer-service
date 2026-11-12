<!-- ============================================================
    AppChannelLogPanel.vue —— 发布渠道 · 聊天记录日志。
    工具栏：按渠道过滤下拉 + 时间范围筛选。
    列表点击某行 -> 打开「对话详情」弹窗（参考调试页面的对话样式）。

    接口：POST /app/setting/chatHistory/{appId}
      body: { page, size, channel, startTime, endTime }
      返回: { records:[{ id, conversationId, appId, channel, createdAt, history }],
              total, current, size }
      history 为 JSON 数组字符串，元素角色含 user / assistant。

    渠道枚举（与后端约定一致）：
      0 调试  1 API访问  2 免登陆窗口  3 账号密码登录窗口
      4 微信个人号  5 飞书机器人  6 钉钉机器人
      7 企业微信机器人  8 微信公众号
    ============================================================ -->
<template>
  <div class="log-wrap">
    <!-- 工具栏：渠道过滤 + 时间筛选 -->
    <div class="log-toolbar">
      <el-select v-model="channel" size="large" class="log-type" placeholder="全部渠道"
                 clearable @change="onFilterChange">
        <el-option label="全部渠道" :value="''" />
        <el-option v-for="c in CHANNEL_OPTIONS" :key="c.value"
                   :label="c.label" :value="c.value" />
      </el-select>

      <el-date-picker v-model="range" type="datetimerange" size="large"
                      class="log-range" popper-class="channel-log-picker" unlink-panels
                      range-separator="至"
                      start-placeholder="开始时间"
                      end-placeholder="结束时间"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      @change="onFilterChange" />

      <span class="log-count">共 {{ total }} 条</span>
    </div>

    <!-- 聊天记录表格 -->
    <el-table v-if="records.length" v-loading="loading" :data="records" class="log-table"
              @row-click="openDetail"
              :header-cell-style="{ background: '#F5F9FF', color: '#3D4B60' }">
      <el-table-column label="会话ID" width="300">
        <template #default="{ row }">
          <span class="conv-id">{{ row.conversationId || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="渠道" width="150">
        <template #default="{ row }">
          <el-tag effect="plain" type="info">{{ channelLabel(row.channel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标记" width="150">
        <template #default="{ row }">{{ row.tagName ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="标题" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.title || '—' }}</template>
      </el-table-column>
      <el-table-column label="客户端已删除" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.customerDeleted" effect="plain" type="danger">已删除</el-tag>
          <el-tag v-else effect="plain" type="success">未删除</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="对话轮数" width="100">
        <template #default="{ row }">{{ messageCount(row) }} 轮</template>
      </el-table-column>
    </el-table>

    <EmptyHint v-if="!records.length && !loading" :icon="Document" text="该条件下暂无聊天记录" />

    <el-pagination v-if="total" class="list-pager" background
                   layout="total, prev, pager, next"
                   :total="total" :page-size="size" :current-page="current"
                   @current-change="onPageChange" />

    <!-- 对话详情抽屉（右侧展开，仅对话记录，可滚动） -->
    <el-drawer v-model="detailVisible" title="对话记录" direction="rtl" size="960px"
               class="channel-log-drawer" @closed="activeLog = null">
      <div v-if="activeLog" class="log-chat">
        <div v-for="(m, i) in activeMessages" :key="i" class="log-chat-item">
          <MessageBubble :role="m.role === 'user' ? 'user' : 'bot'"
                         :avatar="agent.avatar"
                         :bot-icon="Service"
                         :user-icon="User">
            <div class="md-body" v-html="toHtml(m.content)"></div>
          </MessageBubble>
          <div v-if="m.createTime" class="msg-time">{{ m.createTime }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Document, Service, User } from '@element-plus/icons-vue'
import { marked } from 'marked'
import EmptyHint from '@/components/EmptyHint.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import { chatHistoryApi } from '@/api/publishChannel'
import { errorMessage } from '@/api/http'
import { useAgent } from '@/stores/useAgent'

const props = defineProps({
  /** 所属应用 ID */
  appId: { type: [Number, String], default: null },
  /** 当前 Tab 是否处于激活状态，激活时重新拉取聊天记录 */
  active: { type: Boolean, default: true },
})

/** 渠道枚举：value 与后端约定一致 */
const CHANNEL_OPTIONS = [
  { value: 0, label: '调试' },
  { value: 1, label: 'API访问' },
  { value: 2, label: '免登陆窗口' },
  { value: 3, label: '账号密码登录窗口' },
  { value: 4, label: '微信个人号' },
  // 以下 4 个渠道暂未开放，筛选下拉先隐藏；启用时一并放开
  // { value: 5, label: '飞书机器人' },
  // { value: 6, label: '钉钉机器人' },
  // { value: 7, label: '企业微信机器人' },
  // { value: 8, label: '微信公众号' },
]

function channelLabel(channel) {
  const hit = CHANNEL_OPTIONS.find(c => c.value === channel)
  return hit ? hit.label : '未知'
}

const { agent, load: loadAgent } = useAgent()

/* ---------- 筛选条件 ---------- */
const channel = ref('')
const range = ref([])

/* ---------- 分页数据 ---------- */
const records = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)

/** markdown 文本 -> html，空内容返回空串 */
function toHtml(text) {
  if (!text) return ''
  return marked.parse(String(text))
}

/** 解析 history JSON 字符串，失败时返回空数组 */
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

/** 消息条数 */
function messageCount(row) {
  return parseHistory(row.history).length
}

async function load() {
  if (!props.appId) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const [startTime, endTime] = range.value || []
    const page = await chatHistoryApi.page(props.appId, {
      page: current.value,
      size: size.value,
      channel: channel.value === '' ? undefined : channel.value,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
    })
    records.value = page?.records || []
    total.value = page?.total || 0
    current.value = page?.current || current.value
    size.value = page?.size || size.value
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载聊天记录失败'))
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  current.value = 1
  load()
}

function onPageChange(page) {
  current.value = page
  load()
}

/* ---------- 详情弹窗 ---------- */
const detailVisible = ref(false)
const activeLog = ref(null)

function openDetail(row) {
  activeLog.value = row
  detailVisible.value = true
}

/** 当前会话的消息列表 */
const activeMessages = computed(() => parseHistory(activeLog.value?.history))

watch(() => props.appId, () => {
  current.value = 1
  load()
})

/** 每次切换到「日志」Tab 时重新拉取聊天记录 */
watch(() => props.active, (val) => {
  if (val) {
    current.value = 1
    load()
  }
})

onMounted(async () => {
  loadAgent()
  // 若初始就在日志 Tab，才立即加载；否则等激活时再拉
  if (props.active) {
    await load()
  }
})
</script>

<style scoped>
.log-wrap {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/* ---------- 工具栏 ---------- */
.log-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  width: 50%;
}
.log-type {
  width: 170px;
}
/* 时间选择器：锁死 flex，固定宽度，不随工具栏剩余空间被撑大 */
.log-range {
  flex: 0 0 auto;
  width: 240px;
}
.log-range :deep(.el-date-editor.el-input__wrapper) {
  width: 240px;
}
.log-range :deep(.el-range-input) {
  font-size: 12px;
}
.log-range :deep(.el-range-separator) {
  padding: 0 4px;
}
.log-count {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
  font-weight: 500;
}

/* ---------- 表格 ---------- */
/* 列表整体字号为默认的 120%：默认 14px -> 16.8px，表头/单元格/标签一并跟随 */
.log-table {
  width: 100%;
  font-size: 16.8px;
}
.log-table :deep(.el-table__cell) {
  padding: 10px 0;
  font-size: 16.8px;
}
.log-table :deep(.el-table__row) {
  cursor: pointer;
}
/* 表头字号同步放大 */
.log-table :deep(.el-table__header .el-table__cell) {
  font-size: 16.8px;
}
/* 单元格内的标签（渠道、客户端已删除）按比例放大 */
.log-table :deep(.el-tag) {
  font-size: 14.5px;
  height: 26px;
  padding: 0 10px;
}
.conv-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16.8px;
  color: #4B5A70;
}

/* ---------- 详情抽屉：对话记录可滚动 ---------- */
.log-chat {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.log-chat-item {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
/* 创建时间：与气泡对齐，用户侧靠右 */
.log-chat-item:has(.msg.user) .msg-time {
  text-align: right;
}
.msg-time {
  font-size: 16px;
  color: var(--muted);
  padding: 0 4px;
}
.log-chat :deep(.msg) {
  max-width: 100%;
}
.log-chat :deep(.bubble) {
  font-size: 13.5px;
  word-break: break-word;
}

/* ---------- markdown 渲染 ---------- */
.md-body {
  font-size: 16px;
  line-height: 1.75;
  color: var(--text);
  word-break: break-word;
}
.md-body :deep(> *:first-child) {
  margin-top: 0;
}
.md-body :deep(> *:last-child) {
  margin-bottom: 0;
}
.md-body :deep(p) {
  margin: 0 0 10px;
}
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
.md-body :deep(ol) {
  margin: 0 0 10px;
  padding-left: 20px;
}
.md-body :deep(li) {
  margin: 3px 0;
}
.md-body :deep(code) {
  background: var(--brand-soft);
  color: var(--brand-dark);
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12.5px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.md-body :deep(pre) {
  background: #0F172A;
  color: #E2E8F0;
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  margin: 0 0 10px;
  overflow: auto;
}
.md-body :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
  font-size: 12.5px;
}
.md-body :deep(blockquote) {
  margin: 0 0 10px;
  padding: 4px 12px;
  border-left: 3px solid var(--brand-soft-3, var(--brand));
  background: var(--brand-soft-2);
  color: var(--muted);
}
.md-body :deep(a) {
  color: var(--brand);
  text-decoration: none;
}
.md-body :deep(a:hover) {
  text-decoration: underline;
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
.md-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--line);
  margin: 12px 0;
}
</style>

<!-- 弹出面板与抽屉挂载在 body 上（teleport），scoped 样式无法命中，用非 scoped 块限定类名 -->
<style>
/* 隐藏日期范围面板顶部的 4 个输入框（开始/结束 的日期与时间），只保留日历 */
.channel-log-picker .el-date-range-picker__time-header {
  display: none;
}
/* 对话抽屉内容区：仅对话记录，超出高度时内部滚动 */
.channel-log-drawer .el-drawer__body {
  overflow-y: auto;
}
</style>
