<!-- ============================================================
    AppDebugPanel.vue —— 应用编辑页「调试」。
    左右分栏：左侧为提示词（包装返回信息的模板），右侧为对话框。
    提示词沿用系统设置里的默认模板，可直接编辑并在对话中预览。
    ============================================================ -->
<template>
  <div class="debug-wrap">
    <div class="debug-layout">
      <!-- 左：提示词 -->
      <section class="debug-pane prompt-pane">
        <div class="pane-head">
          <span class="pane-icon"><el-icon :size="17"><MagicStick /></el-icon></span>
          <span class="pane-title">提示词</span>
          <el-tooltip placement="top" effect="light">
            <template #content>
              <div class="pane-tip-content">
                提示词可为空，如果为空，系统直接返回语料，不走大模型。

              </div>
            </template>
            <el-icon class="pane-title-tip"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>

        <el-input v-model="prompt" type="textarea" resize="none" class="prompt-input"
                  :placeholder="PROMPT_PLACEHOLDER" />

        <div class="pane-foot">
          <el-tooltip placement="top" effect="light">
            <template #content>
              <div class="pane-tip-content">
                <标准答案>：根据问题查询到的标准答案<br />
                <当前时间>：当前时间<br />
                <系统关键词>：系统内所有的关键词
              </div>
            </template>
            <span class="pane-tip">
              <el-icon><QuestionFilled /></el-icon>
              内置变量
            </span>
          </el-tooltip>
          <div class="pane-foot-actions">
            <el-button type="danger" size="large" :loading="deleting" :disabled="saving"
                       @click="deletePrompt">
              <el-icon class="btn-icon"><Delete /></el-icon>删除
            </el-button>
            <el-button type="primary" size="large" :loading="saving" @click="savePrompt">
              <el-icon class="btn-icon"><Check /></el-icon>保存
            </el-button>
          </div>
        </div>
      </section>

      <!-- 右：对话框 -->
      <section class="debug-pane chat-pane">
        <div class="pane-head chat-head">
          <span class="pane-icon"><el-icon :size="17"><ChatDotRound /></el-icon></span>
          <span class="pane-title">{{ appName || '客服助手' }}</span>
          <!-- 锚点包住按钮与面板：面板从按钮下方长出，左边缘与按钮左边缘对齐 -->
          <div class="chat-options-anchor">
            <el-button ref="optionsToggleRef" size="small" plain class="chat-options-toggle"
                       :class="{ 'is-open': showOptions }"
                       @click.stop="toggleOptions">
              <el-icon class="btn-icon"><Setting /></el-icon>模型参数
              <el-icon class="toggle-arrow"><ArrowDown /></el-icon>
            </el-button>

            <!-- 模型参数调试：从按钮下方展开（点击按钮或页面其他区域收起） -->
            <div ref="optionsPanelRef" v-show="showOptions" class="chat-options">
              <!-- temperature：关闭时值为 null（不覆盖），开启后才有值 -->
              <div class="opt-row">
                <div class="opt-label">
                  <span>temperature</span>
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        采样温度，取值范围 0.0 ~ 2.0<br />
                        数值越小输出越稳定、越确定；关闭表示不覆盖，使用模型默认值
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <!-- 取值范围 0.0 ~ 2.0，步长 0.1 -->
                <el-input-number v-model="chatOptions.temperature" :min="0" :max="2" :step="0.1"
                                 :precision="1" controls-position="right" class="opt-control"
                                 :disabled="!enabled.temperature" placeholder="不覆盖" />
                <el-switch v-model="enabled.temperature" class="opt-switch"
                           @change="onOptionToggle('temperature')" />
              </div>

              <!-- topP：关闭时值为 null（不覆盖），开启后才有值 -->
              <div class="opt-row">
                <div class="opt-label">
                  <span>topP</span>
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        nucleus 采样，取值范围 0.3 ~ 0.9<br />
                        与 temperature 一般只调其一；关闭表示不覆盖
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <!-- 取值范围 0.3 ~ 0.9，步长 0.1 -->
                <el-input-number v-model="chatOptions.topP" :min="0.3" :max="0.9" :step="0.1"
                                 :precision="1" controls-position="right" class="opt-control"
                                 :disabled="!enabled.topP" placeholder="不覆盖" />
                <el-switch v-model="enabled.topP" class="opt-switch"
                           @change="onOptionToggle('topP')" />
              </div>

              <div class="opt-row">
                <div class="opt-label">
                  <span>maxOutputTokens</span>
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        最大输出 token 数，取值范围 4096 ~ 409600<br />
                        留空表示不覆盖，使用模型默认值
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <!-- 取值范围 4096 ~ 409600，步长 1024 -->
                <el-input-number v-model="chatOptions.maxOutputTokens"
                                 :min="4096" :max="409600" :step="1024"
                                 controls-position="right" class="opt-control"
                                 placeholder="不覆盖" />
              </div>

              <div class="opt-foot">
                <span class="opt-foot-hint"> </span>
                <div class="opt-foot-actions">
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        保存会影响该应用已发布的渠道，它们会同步使用新参数；<br />
                        调试不发布，不要点击保存
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                  <el-button type="primary" size="small" :loading="savingOptions"
                             @click="saveChatOptions">保存</el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 记忆参数：与模型参数并列，面板从按钮下方展开 -->
          <div class="chat-options-anchor">
            <el-button ref="memoryToggleRef" size="small" plain class="chat-options-toggle"
                       :class="{ 'is-open': showMemory }"
                       @click.stop="toggleMemory">
              <el-icon class="btn-icon"><Notebook /></el-icon>记忆
              <el-icon class="toggle-arrow"><ArrowDown /></el-icon>
            </el-button>

            <div ref="memoryPanelRef" v-show="showMemory" class="chat-options memory-options">
              <!-- 是否开启记忆：关闭时不下发记忆参数，本次调试不带上下文 -->
              <div class="opt-row">
                <div class="opt-label">
                  <span>开启记忆</span>
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        开启后会携带当前会话的历史上下文；<br />
                        关闭表示本次调试不带记忆，每次都是全新的对话
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <div class="opt-control opt-control--switch">
                  <el-switch v-model="memoryEnabled" />
                </div>
              </div>

              <!-- 记忆轮数：仅在开启记忆时显示；禁用时隐藏但保留当前取值，不影响下次开启 -->
              <div v-if="memoryEnabled" class="opt-row">
                <div class="opt-label">
                  <span>记忆轮数</span>
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        保留的历史问答轮数，一轮 = 用户提问 + 模型回复<br />
                        数值越大上下文越长，取值不小于 1
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <el-input-number v-model="memoryRounds" :min="1" :max="100" :step="1"
                                 controls-position="right" class="opt-control" />
              </div>

              <div class="opt-foot">
                <span class="opt-foot-hint"> </span>
                <div class="opt-foot-actions">
                  <el-tooltip placement="top" effect="light">
                    <template #content>
                      <div class="pane-tip-content">
                        保存会影响该应用已发布的渠道，它们会同步使用新配置；<br />
                        调试不发布，仅需本次调试生效时可直接改，不必保存
                      </div>
                    </template>
                    <el-icon class="opt-tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                  <el-button type="primary" size="small" :loading="savingMemory"
                             @click="saveMemory">保存</el-button>
                </div>
              </div>
            </div>
          </div>

          <span class="chat-count">共 {{ messages.length }} 条</span>
          <el-button size="small" plain class="chat-clear" :disabled="loading"
                     @click="reset">清空</el-button>
        </div>

        <div ref="chatBody" class="chat-body">
          <MessageBubble v-for="(m, i) in messages" :key="i" :role="m.role"
                         :bot-icon="Service">
            <template v-if="m.role === 'user'">{{ m.text }}</template>
            <template v-else>
              <div v-if="m.loading" class="status-line">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div v-if="m.md" class="md-body" v-html="m.md"></div>
            </template>
          </MessageBubble>

          <EmptyHint v-if="!messages.length" :icon="ChatDotRound"
                     text="输入用户问题开始调试" />
        </div>

        <div class="chat-input">
          <el-input v-model="input" placeholder="输入用户问题，回车发送…"
                    clearable @keyup.enter="send">
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
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ArrowDown, ChatDotRound, Search, Promotion, MagicStick, QuestionFilled, Check, Delete, Service, Loading, Setting, Notebook }
  from '@element-plus/icons-vue'
import { marked } from 'marked'
import EmptyHint from '@/components/EmptyHint.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import { useEngineCorpus } from '@/stores/useEngineCorpus'
import { appApi } from '@/api/app'
import { appDebugApi } from '@/api/appDebug'
import { errorMessage } from '@/api/http'
import { settings as defaultSettings } from '@/data/config'

const props = defineProps({
  /** 所属应用 ID（预留：后续可切换为按应用范围做匹配） */
  appId: { type: [Number, String], default: null },
  /** 所属应用名称，用作对话框标题 */
  appName: { type: String, default: '' },
  /** 该应用绑定的语料分组，调试时只在这些分组内做匹配 */
  groupIds: { type: Array, default: () => [] },
})

const engineCorpus = useEngineCorpus()

/** 默认模板只作为输入框提示（placeholder）展示，不真正填入内容 */
const PROMPT_PLACEHOLDER = defaultSettings.wrapPrompt

/** 提示词：留空由用户自行填写，进入页面后以接口返回为准 */
const prompt = ref('')
const input = ref('')
/** 本次页面会话的任务 ID：进入页面时用 uuid 生成一次，全程调试请求共用 */
const taskId = ref('')
const messages = ref([])
const chatBody = ref(null)
const saving = ref(false)
/** 删除提示词请求进行中 */
const deleting = ref(false)
/** 调试请求进行中：输入与发送禁用，避免并发事件流串消息 */
const loading = ref(false)
/** 当前事件流的 AbortController，切换/卸载时中断 */
let controller = null

/** 模型参数面板是否展开 */
const showOptions = ref(false)
/** 模型参数面板根节点，用于判断点击是否落在面板内部 */
const optionsPanelRef = ref(null)
/** 「模型参数」按钮根节点，用于判断点击是否落在按钮上 */
const optionsToggleRef = ref(null)

/** 记忆参数面板是否展开 */
const showMemory = ref(false)
/** 记忆参数面板根节点，用于判断点击是否落在面板内部 */
const memoryPanelRef = ref(null)
/** 「记忆」按钮根节点，用于判断点击是否落在按钮上 */
const memoryToggleRef = ref(null)

/** 是否开启会话记忆：关闭时不下发记忆相关参数；初值与后端一致（默认关闭） */
const memoryEnabled = ref(false)
/** 会话记忆保留的问答轮数，仅在开启记忆时下发生效；初值与后端一致 */
const memoryRounds = ref(1)

/** 生成 uuid：优先用原生 crypto.randomUUID，非安全上下文下退化为时间戳 + 随机数 */
function createTaskId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`
}

/**
 * 模型对话参数，字段与 /app/setting/info/{appId} 返回的 chatOptions 一致。
 * null 表示不覆盖该参数，回落到模型默认值。
 */
const chatOptions = reactive({
  temperature: null,
  topP: null,
  maxOutputTokens: null,
})

/**
 * temperature / topP 的启用开关：关闭时对应参数强制为 null（不覆盖），
 * 开启时才把值写回 chatOptions。maxOutputTokens 不设开关。
 */
const enabled = reactive({
  temperature: false,
  topP: false,
})

/** 各参数启用时的兜底默认值，避免开启后仍为 null 无法编辑（须落在各自取值范围内） */
const OPTION_FALLBACK = {
  temperature: 0.7,
  topP: 0.9,
}

/** 切换启用状态：开启时补默认值，关闭时把值置为 null */
function onOptionToggle(key) {
  chatOptions[key] = enabled[key] ? OPTION_FALLBACK[key] : null
}

/** 把接口返回的 chatOptions 灌进本地表单：有值即视为启用，null / 缺失则关闭并置 null */
function applyChatOptions(options) {
  for (const key of ['temperature', 'topP']) {
    const value = options?.[key] ?? null
    chatOptions[key] = value
    enabled[key] = value !== null
  }
  chatOptions.maxOutputTokens = options?.maxOutputTokens ?? null

  // 记忆参数与模型参数同源：后端返回 memoryEnabled / memoryRounds，未配置时回落到默认值
  memoryEnabled.value = options?.memoryEnabled ?? false
  memoryRounds.value = options?.memoryRounds ?? 1
}

/** 模型参数保存请求进行中 */
const savingOptions = ref(false)

/**
 * 保存模型参数：生效范围是该应用所有渠道，不只是当前调试页。
 * 未启用的参数统一提交 null（不覆盖），与接口返回格式保持一致。
 */
async function saveChatOptions() {
  if (!props.appId) {
    return ElMessage.warning('缺少应用 ID，无法保存')
  }

  try {
    await ElMessageBox.confirm(
      '保存后该应用的模型参数将立即生效，已发布的渠道也会同步使用新参数；当前页面的调试不受影响。',
      '确认保存模型参数？',
      { type: 'warning', confirmButtonText: '保存', cancelButtonText: '取消' },
    )
  } catch {
    // 取消保存，不做任何处理
    return
  }

  savingOptions.value = true
  try {
    await appApi.saveChatOptions({
      appId: Number(props.appId),
      temperature: chatOptions.temperature,
      topP: chatOptions.topP,
      maxOutputTokens: chatOptions.maxOutputTokens,
    })
    ElMessage.success('模型参数已保存')
    // 保存成功后收起参数面板，回到干净的对话框视图
    showOptions.value = false
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存模型参数失败'))
  } finally {
    savingOptions.value = false
  }
}

/** 记忆配置保存请求进行中 */
const savingMemory = ref(false)

/**
 * 保存对话记忆配置：只更新 memoryEnabled / memoryRounds，不影响模型参数。
 * 请求体三个字段平铺，成功后无响应体，看 HTTP 200 即可。
 */
async function saveMemory() {
  if (!props.appId) {
    return ElMessage.warning('缺少应用 ID，无法保存')
  }

  try {
    await ElMessageBox.confirm(
      '保存后该应用的对话记忆配置将立即生效，已发布的渠道也会同步使用新配置；当前页面的调试不受影响。',
      '确认保存记忆配置？',
      { type: 'warning', confirmButtonText: '保存', cancelButtonText: '取消' },
    )
  } catch {
    // 取消保存，不做任何处理
    return
  }

  savingMemory.value = true
  try {
    await appApi.saveChatMemory({
      appId: Number(props.appId),
      memoryEnabled: memoryEnabled.value,
      memoryRounds: memoryRounds.value,
    })
    ElMessage.success('记忆配置已保存')
    // 保存成功后收起面板，回到干净的对话框视图
    showMemory.value = false
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存记忆配置失败'))
  } finally {
    savingMemory.value = false
  }
}

/** 点击「模型参数」按钮：展开 / 收起 */
function toggleOptions() {
  showOptions.value = !showOptions.value
}

/** 点击「记忆」按钮：展开 / 收起 */
function toggleMemory() {
  showMemory.value = !showMemory.value
}

/**
 * 点击面板与按钮之外的任意区域都收起面板。
 * 按钮本身交给各自 toggle 处理（click-outside 会跳过它，避免开了立刻又被关掉）。
 */
function onClickOutside(event) {
  const target = event.target
  if (showOptions.value
    && !optionsPanelRef.value?.contains(target)
    && !optionsToggleRef.value?.contains(target)) {
    showOptions.value = false
  }
  if (showMemory.value
    && !memoryPanelRef.value?.contains(target)
    && !memoryToggleRef.value?.contains(target)) {
    showMemory.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
})

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
  if (!text) {
    return
  }
  if (loading.value) {
    return
  }

  if (!props.appId) {
    return ElMessage.warning('缺少应用 ID，无法调试')
  }

  messages.value.push({ role: 'user', text })
  input.value = ''

  // 机器人占位气泡：loading 控制加载图标；raw 累积原始文本，md 为渲染后的 html
  messages.value.push({ role: 'bot', loading: true, md: '', raw: '' })
  // 必须取回 push 后数组里的响应式代理对象来改；
  // 直接用 push 前的原始对象修改会绕过 Vue 响应式，界面不会更新。
  const bot = messages.value[messages.value.length - 1]
  loading.value = true
  scrollBottom()

  const ac = new AbortController()
  controller = ac
  /** 收尾：无论正常结束、报错还是停止，都必须先摘掉加载图标 */
  const finish = () => {
    bot.loading = false
    loading.value = false
    if (controller === ac) {
      controller = null
    }
    scrollBottom()
  }

  try {
    await appDebugApi.debug(
      {
        appId: Number(props.appId),
        taskId: taskId.value,
        prompt: prompt.value,
        question: text,
        // 记忆参数与模型参数同级：轮数始终按当前取值下发，是否启用记忆由 memoryEnabled 决定
        memoryEnabled: memoryEnabled.value,
        memoryRounds: memoryRounds.value,
      },
      // 模型参数作为独立属性单独向后传递：null 表示不覆盖，回落到模型默认值
      { ...chatOptions },
      {
        // status：开始调试 / 调试完成，仅触发滚动跟随
        status: () => {
          scrollBottom()
        },
        // message：后端下发的是「增量片段」，前端自行累加到 raw 再整体渲染（打字机效果）
        message: (data) => {
          const content = pickText(data)
          if (content) {
            bot.raw += content
            bot.md = toHtml(bot.raw)
          }
          scrollBottom()
        },
        // done：最终答复，用它整体替换 message 阶段的内容（done 无内容时保留已有渲染）
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

    // 流正常结束但没有 done 事件时，兜底文案
    if (!bot.md) {
      bot.md = toHtml('暂无返回内容')
    }
  } catch (e) {
    // 停止 / 清空导致的主动中断不算错误
    if (e?.name !== 'AbortError') {
      bot.md = toHtml(`**调试失败**：${errorMessage(e, '调试请求失败')}`)
      ElMessage.error(errorMessage(e, '调试请求失败'))
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
  // 先摘图标，abort 后即便读取流抛出的不是 AbortError 也不会残留转圈
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'bot') {
    last.loading = false
  }
  loading.value = false
  controller.abort()
}

function reset() {
  // 中断进行中的事件流，避免旧事件继续写入已清空的消息列表
  if (controller) {
    controller.abort()
    controller = null
  }
  loading.value = false
  messages.value = []
}

/** 进入页面时按 appId 拉取已配置的提示词与模型参数，未配置则保持为空 */
async function loadPrompt() {
  if (!props.appId) {
    return
  }
  try {
    const data = await appApi.settingInfo(props.appId)
    prompt.value = data?.content || ''
    // chatOptions 可能为 null（未配置），此时全部按「不覆盖」处理
    applyChatOptions(data?.chatOptions)
  } catch (e) {
    // 应用不存在等异常直接提示，不阻断调试
    ElMessage.error(errorMessage(e, '加载提示词失败'))
  }
}

/**
 * 删除提示词：调接口清空该应用已配置的提示词，成功后本地编辑框同步置空。
 * 删除后问答直接返回命中的标准答案，不再走提示词包装。
 */
async function deletePrompt() {
  if (!props.appId) {
    return ElMessage.warning('缺少应用 ID，无法删除')
  }

  try {
    await ElMessageBox.confirm(
      '删除后该应用将不再使用提示词包装答案，问答时直接返回命中语料的标准答案。',
      '确认删除提示词？',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    // 取消删除，不做任何处理
    return
  }

  deleting.value = true
  try {
    await appApi.deleteSetting(Number(props.appId))
    prompt.value = ''
    ElMessage.success('提示词已删除')
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除提示词失败'))
  } finally {
    deleting.value = false
  }
}

/** 保存提示词：按 appId 保存，同一应用重复保存会覆盖更新 */
async function savePrompt() {
  const content = prompt.value
  if (!content) {
    return ElMessage.warning('请输入提示词')
  }
  if (!props.appId) {
    return ElMessage.warning('缺少应用 ID，无法保存')
  }

  saving.value = true
  try {
    await appApi.saveSetting({
      appId: Number(props.appId),
      content,
    })
    ElMessage.success('提示词已保存')
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存提示词失败'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  // 页面加载即生成 taskId，本次会话内所有调试请求都带同一个 ID
  taskId.value = createTaskId()
  await Promise.all([engineCorpus.ensureLoaded(true), loadPrompt()])
  scrollBottom()
})

// 离开页面时中断未完成的调试流
onBeforeUnmount(() => {
  controller?.abort()
  controller = null
})
</script>

<style scoped>
/*
 * 撑开父级 section-body 的 9 成以上高度。
 * 父级只有 min-height（无确定高度），百分比高度无法解析，
 * 因此用视口高度推算：100vh 减去顶栏 64px、内容区上下 padding 48px、
 * 卡片 padding 40px、顶部入口行约 66px，再乘 0.95 得到 95% 的高度。
 */
.debug-wrap {
  display: flex;
  align-items: stretch;
  height: calc((100vh - 64px - 48px - 40px - 66px) * 0.95);
  min-height: 520px;
}
.debug-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  gap: 20px;
  align-items: stretch;
  width: 100%;
  /* 撑满 .debug-wrap 的整块高度，左右两栏随之等高 */
  height: 100%;
  min-height: 0;
}
@media (max-width: 1080px) {
  .debug-layout {
    grid-template-columns: 1fr;
  }
}

/* 左右两栏共用外框 */
.debug-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 16px;
  background: var(--brand-soft-2);
}

.pane-head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
  font-size: 15px;
}
.pane-icon {
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
.pane-title {
  font-weight: 700;
  color: var(--text);
}
/* 标题后的「?」提示图标，悬浮展示变量说明 */
.pane-title-tip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--muted);
  cursor: help;
  transition: color .15s;
}
.pane-title-tip:hover {
  color: var(--brand);
}
.pane-title-tip :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ---------- 左侧：提示词 ---------- */
.prompt-input {
  flex: 1;
  min-height: 0;
  margin-top: 16px;
}
.prompt-input :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 320px;
  border-radius: var(--radius);
  font-size: 13.5px;
  line-height: 1.75;
  font-family: inherit;
  padding: 14px;
}
.pane-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}
.pane-tip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 12.5px;
  cursor: help;
}
.pane-tip:hover {
  color: var(--brand);
}
.pane-tip-content {
  max-width: 260px;
  font-size: 13px;
  line-height: 1.7;
  color: #3D4B60;
}
/* 底部操作区：删除 + 保存 并排靠右 */
.pane-foot-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

/* ---------- 右侧：对话框 ---------- */
.chat-pane {
  background: #fff;
}
.chat-head .chat-count {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
  font-weight: 500;
}
.chat-head .chat-clear {
  flex: 0 0 auto;
}
/* 模型参数入口按钮：紧贴应用名右侧；高度在 small 基础上提高约 2 成 */
.chat-head .chat-options-toggle {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 29px;
  padding: 0 12px;
  font-size: 13.5px;
}
.chat-head .chat-options-toggle .btn-icon {
  font-size: 15px;
}
.chat-head .chat-options-toggle .toggle-arrow {
  font-size: 12px;
  transition: transform .2s;
}
.chat-head .chat-options-toggle.is-open .toggle-arrow {
  transform: rotate(180deg);
}

/* ---------- 模型参数面板：从「模型参数」按钮下方展开 ---------- */
/* 锚点紧贴按钮宽度，面板左边缘自然与按钮左边缘对齐 */
.chat-options-anchor {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}
.chat-options {
  /* 与按钮同宽起点：面板左边缘对齐按钮左边缘，向右延伸，故用绝对定位脱离文档流宽度约束 */
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 20;
  width: 420px;
  max-width: calc(100vw - 80px);
  margin-top: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
  box-shadow: var(--shadow, 0 6px 18px rgba(15, 23, 42, .12));
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 展开动画：从按钮位置向下「长出」的效果 */
  transform-origin: top left;
  animation: options-drop .18s ease-out;
}
@keyframes options-drop {
  from {
    opacity: 0;
    transform: translateY(-6px) scaleY(.94);
  }
  to {
    opacity: 1;
    transform: translateY(0) scaleY(1);
  }
}
.opt-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.opt-label {
  flex: 0 0 130px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.opt-tip {
  color: var(--muted);
  cursor: help;
  font-size: 13px;
  transition: color .15s;
}
.opt-tip:hover {
  color: var(--brand);
}
.opt-control {
  flex: 1 1 auto;
  min-width: 0;
  /* 限制输入框最大宽度，让后面的开关紧跟在输入框右侧（不贴面板最右） */
  max-width: 180px;
}
/* 纯开关行：开关紧跟标签左对齐，不占满整行 */
.opt-control--switch {
  flex: 0 0 auto;
  max-width: none;
}
/* 记忆参数面板：比模型参数面板窄一些 */
.memory-options {
  width: 360px;
}
.opt-control :deep(.el-input__wrapper) {
  background: #fff;
}
/* 启用开关：紧跟输入框右侧 */
.opt-switch {
  flex: 0 0 auto;
  margin-left: 2px;
}
.opt-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}
.opt-foot-hint {
  color: var(--muted);
  font-size: 12.5px;
}
/* 底部右侧：保存影响范围提示（问号悬浮） + 保存按钮 */
.opt-foot-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}
.chat-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-input {
  border-top: 1px solid var(--line);
  padding-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}
/* 输入框与右侧 large 按钮等高（40px），垂直居中对齐 */
.chat-input :deep(.el-input__wrapper) {
  height: 40px;
  padding: 0 14px;
}
.chat-input :deep(.el-input__inner) {
  height: 40px;
  font-size: 14px;
  line-height: 40px;
}
.chat-input :deep(.el-input__prefix) {
  display: flex;
  align-items: center;
}
/* 发送按钮与「提示词-保存」保持同一尺寸（large），并固定高度对齐输入框 */
.send-btn {
  flex: 0 0 auto;
  height: 40px;
}

.hit-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 700;
  font-size: 13.5px;
  margin-bottom: 9px;
  color: var(--ok);
}
.hit-title--miss {
  color: var(--warn);
}
.reply-box {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 12px 14px;
  font-size: 13.5px;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 事件流状态行：开始调试 / 调试完成 */
.status-line {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  font-size: 13px;
}
/* 状态行与正文之间留白，正文存在时才需要 */
.status-line + .md-body {
  margin-top: 10px;
}

/* ---------- markdown 渲染 ---------- */
.md-body {
  font-size: 13.5px;
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
