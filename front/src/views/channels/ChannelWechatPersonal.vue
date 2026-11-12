<!-- ============================================================
    ChannelWechatPersonal.vue —— 「微信个人号」渠道的配置区。
    「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
    已接入的微信号全部来自接口，本组件只负责渲染与发起扫码：
      - 列表：GET /app/setting/botCredentialList/{appId}
      - 扫码：POST /bot/qrcode + 轮询 POST /bot/qrcode/status
    ============================================================ -->
<template>
  <div class="channel-add-bar">
    <el-button type="primary" @click="openDialog">
      <el-icon class="btn-icon"><Plus /></el-icon>新增接入
    </el-button>
  </div>

  <!-- 已绑定的微信号：数据来自 botCredentialList/{appId}，最新绑定在前 -->
  <el-table v-if="records.length" v-loading="loading" :data="records"
            row-key="id" class="bot-table" size="default">
    <el-table-column label="扫码人" min-width="140" show-overflow-tooltip>
      <template #default="{ row }">{{ row.scannerName || '—' }}</template>
    </el-table-column>

    <el-table-column prop="ilinkBotId" label="机器人 ID" min-width="200" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="id-text">{{ row.ilinkBotId || '—' }}</span>
      </template>
    </el-table-column>

    <el-table-column prop="ilinkUserId" label="扫码人 wxid" min-width="200" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="id-text">{{ row.ilinkUserId || '—' }}</span>
      </template>
    </el-table-column>

    <el-table-column label="状态" width="130">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
          {{ row.status === 1 ? '已登录可用' : '未登录 / 已失效' }}
        </el-tag>
      </template>
    </el-table-column>

    <el-table-column label="更新时间" width="180">
      <template #default="{ row }">{{ row.updatedAt || '—' }}</template>
    </el-table-column>

    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ row }">
        <el-tooltip content="编辑扫码人" placement="top">
          <el-button text type="primary" size="small" class="icon-action"
                     @click="openEditScanner(row)">
            <el-icon :size="17"><Edit /></el-icon>
          </el-button>
        </el-tooltip>
      </template>
    </el-table-column>
  </el-table>

  <div v-else v-loading="loading" class="record-empty">
    <div class="empty-text">尚未配置「微信个人号」</div>
    <div class="empty-tip">点击「新增接入」扫码绑定微信号</div>
  </div>

  <!-- 微信个人号专属弹窗：扫码登录（二维码由后端下发） -->
  <el-dialog v-model="dialogVisible" title="新增微信个人号" width="560px"
             :close-on-click-modal="false" @closed="stopPolling">
    <div class="qr-box">
      <!-- 二维码图片 -->
      <div v-loading="qrLoading" class="qr-canvas">
        <img v-if="qrImgSrc" :src="qrImgSrc" class="qr-img" alt="扫码登录二维码" />
        <div v-else-if="qrError" class="qr-placeholder qr-error">
          {{ qrError }}
        </div>
        <div v-else-if="!qrLoading" class="qr-placeholder">二维码加载中…</div>
      </div>

      <!-- 状态与操作 -->
      <div class="qr-side">
        <div class="qr-step">{{ qrStepText }}</div>
        <div class="qr-tip">请使用要接入的微信扫码，并在手机上确认登录。</div>
        <div class="qr-meta">
          <span>剩余有效时间：{{ countdownText }}</span>
        </div>

        <div v-if="qrPayload" class="qr-link">
          <el-input :model-value="qrPayload" readonly size="small" />
          <el-button size="small" @click="copyPayload">复制链接</el-button>
        </div>

        <div class="qr-actions">
          <el-button size="default" :loading="qrLoading" @click="refreshQrcode">
            刷新二维码
          </el-button>
        </div>

        <div v-if="qrError" class="qr-err-msg">{{ qrError }}</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 编辑扫码人：按凭证 ID 更新 scannerName，空串表示清空 -->
  <el-dialog v-model="scannerVisible" title="编辑扫码人" width="460px"
             :close-on-click-modal="false" @closed="resetScanner">
    <el-form ref="scannerFormRef" :model="scannerDraft" :rules="SCANNER_RULES"
             label-width="80px" class="scanner-form" @submit.prevent>
      <el-form-item label="扫码人" prop="scannerName">
        <el-input v-model="scannerDraft.scannerName" placeholder="扫码人展示名，留空则清空"
                  maxlength="64" show-word-limit clearable />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="scannerVisible = false">取消</el-button>
      <el-button type="primary" :loading="scannerSaving" @click="submitScanner">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Plus, Edit } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { botLoginApi } from '@/api/publishChannel'
import { errorMessage } from '@/api/http'

const props = defineProps({
  /** 渠道定义（label / desc / multi 等） */
  channel: { type: Object, default: null },
  /** 所属应用 ID：列表查询放路径，扫码绑定时放 body */
  appId: { type: [Number, String], default: null },
})

/* ---------- 列表：数据源为接口，本地只做只读渲染 ---------- */
const records = ref([])
const loading = ref(false)

/** 拉取当前应用已绑定的微信个人号，最新绑定在前 */
async function loadList() {
  if (!props.appId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    records.value = (await botLoginApi.credentialList(props.appId)) || []
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载微信个人号失败'))
  } finally {
    loading.value = false
  }
}

// 挂载即拉取；appId 变化（切换应用）时重新拉
onMounted(loadList)
watch(() => props.appId, loadList)

const dialogVisible = ref(false)

/* ---------- 扫码登录：二维码获取与轮询 ---------- */
/** 轮询间隔 */
const POLL_INTERVAL = 2000
/** 二维码有效期（秒），后端 Bot 默认 scanTimeoutSeconds = 300 */
const QR_TTL_SECONDS = 300

const qrLoading = ref(false)
const qrError = ref('')
/** 后端返回的轮询令牌 */
const qrToken = ref('')
/** 二维码内容 / 链接（同时提供文本复制，便于用其它设备扫码） */
const qrPayload = ref('')
/** 渲染成图片的 dataURL */
const qrImgSrc = ref('')
/** 剩余有效秒数 */
const remainSeconds = ref(0)

let pollTimer = null
let tickTimer = null

const countdownText = computed(() => {
  const s = remainSeconds.value
  if (s <= 0) return '已过期'
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

const qrStepText = computed(() => {
  if (qrError.value) return '二维码获取失败'
  if (!qrToken.value) return '正在获取二维码…'
  if (remainSeconds.value <= 0) return '二维码已过期，请刷新'
  return '等待扫码确认'
})

/** 把二维码内容渲染成图片，后端可能直出图片 URL，此时直接使用 */
async function renderQr(content) {
  qrImgSrc.value = ''
  if (!content) return
  if (/^(https?:)?\/\//.test(content) && /\.(png|jpe?g|gif|webp)(\?|$)/i.test(content)) {
    qrImgSrc.value = content
    return
  }
  try {
    qrImgSrc.value = await QRCode.toDataURL(content, { width: 240, margin: 1 })
  } catch {
    qrError.value = '二维码渲染失败，可复制下方链接扫码'
  }
}

async function fetchQrcode() {
  stopPolling()
  qrError.value = ''
  qrLoading.value = true
  qrToken.value = ''
  qrPayload.value = ''
  qrImgSrc.value = ''
  try {
    const data = await botLoginApi.qrcode(props.appId)
    const token = data?.qrcode || ''
    const content = data?.qrcodeImgContent || ''
    if (!token) throw new Error('missing qrcode')
    qrToken.value = token
    qrPayload.value = content
    await renderQr(content)
    startTimers()
  } catch (e) {
    qrError.value = errorMessage(e, '获取二维码失败，请确认后端已登录 Bot 管理端')
  } finally {
    qrLoading.value = false
  }
}

/** 倒计时 + 轮询扫码结果 */
function startTimers() {
  remainSeconds.value = QR_TTL_SECONDS
  tickTimer = setInterval(() => {
    if (remainSeconds.value > 0) remainSeconds.value -= 1
    if (remainSeconds.value <= 0) stopPolling(true)
  }, 1000)

  pollTimer = setInterval(async () => {
    if (!qrToken.value) return
    try {
      const status = await botLoginApi.qrcodeStatus({ qrcode: qrToken.value })
      const state = String(status?.status || '').toLowerCase()
      // 后端 QrcodeStatus.status 为字符串：confirmed / expired / waiting
      if (state.includes('confirm') || state.includes('success')) {
        const botId = status?.ilinkBotId || status?.ilinkUserId || ''
        ElMessage.success(botId ? `登录成功：${botId}` : '登录成功')
        stopPolling()
        dialogVisible.value = false
        // 绑定成功后列表以接口为准，重新拉取
        loadList()
      } else if (state.includes('expire') || state.includes('invalid')) {
        stopPolling(true)
      }
    } catch {
      // 轮询失败不打断，等下一轮或超时
    }
  }, POLL_INTERVAL)
}

/** onlyExpire：仅停止并提示过期，不清状态 */
function stopPolling(expired = false) {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  if (tickTimer) { clearInterval(tickTimer); tickTimer = null }
  if (expired) remainSeconds.value = 0
}

function refreshQrcode() {
  fetchQrcode()
}

async function copyPayload() {
  if (!qrPayload.value) return
  try {
    await navigator.clipboard.writeText(qrPayload.value)
    ElMessage.success('二维码链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/** 打开弹窗时自动拉取二维码 */
function openDialog() {
  dialogVisible.value = true
  fetchQrcode()
}

onBeforeUnmount(stopPolling)

/* ---------- 编辑扫码人 ---------- */
/** 最长 64 字，允许空串（表示清空扫码人） */
const SCANNER_RULES = {
  scannerName: [{ max: 64, message: '扫码人不能超过 64 个字符', trigger: 'blur' }],
}

const scannerFormRef = ref(null)
const scannerVisible = ref(false)
const scannerSaving = ref(false)
/** 正在编辑的凭证，保存时取其中的 id 作为路径参数 */
const scannerRow = ref(null)
const scannerDraft = reactive({ scannerName: '' })

function openEditScanner(row) {
  scannerRow.value = row || null
  Object.assign(scannerDraft, { scannerName: row?.scannerName || '' })
  scannerVisible.value = true
}

function resetScanner() {
  scannerRow.value = null
  Object.assign(scannerDraft, { scannerName: '' })
  scannerFormRef.value?.clearValidate()
}

async function submitScanner() {
  try {
    await scannerFormRef.value?.validate()
  } catch {
    return
  }

  const target = scannerRow.value
  if (!target?.id) {
    return ElMessage.error('缺少凭证 ID，无法保存扫码人')
  }

  scannerSaving.value = true
  try {
    await botLoginApi.updateBotCredentialScanner(target.id, {
      // 空串表示清空；去掉首尾空格避免误存空白
      scannerName: scannerDraft.scannerName.trim(),
    })
    scannerVisible.value = false
    ElMessage.success('扫码人已保存')
    // 列表以接口为准，重新拉取
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存扫码人失败'))
  } finally {
    scannerSaving.value = false
  }
}
</script>

<style scoped>
/* ---------- 已绑定微信号表格 ---------- */
.bot-table {
  margin-top: 16px;
}
/* 操作列的图标按钮 */
.bot-table :deep(.icon-action) {
  padding: 0 6px;
  height: 26px;
}
.bot-table :deep(.icon-action .el-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 编辑扫码人弹窗 ---------- */
.scanner-form :deep(.el-input__wrapper) {
  height: 38px;
}
.scanner-form :deep(.el-input__inner) {
  height: 38px;
  line-height: 38px;
}
/* 机器人 ID / wxid 用等宽字体，方便核对 */
.id-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16.8px;
  color: #3D4B60;
  word-break: break-all;
  user-select: all;
}

/* ---------- 扫码登录弹窗 ---------- */
.qr-box {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.qr-canvas {
  flex: 0 0 240px;
  width: 240px;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #fff;
  overflow: hidden;
}
.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  /* 二维码不做抗锯齿缩放，保证边缘清晰可扫 */
  image-rendering: pixelated;
}
.qr-placeholder {
  padding: 0 16px;
  text-align: center;
  font-size: 12.5px;
  color: var(--muted);
}
.qr-error {
  color: var(--danger);
  line-height: 1.7;
}
.qr-side {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.qr-step {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
}
.qr-tip {
  font-size: 12.5px;
  line-height: 1.7;
  color: #4B5A70;
}
.qr-meta {
  font-size: 12.5px;
  color: var(--muted);
}
.qr-link {
  display: flex;
  gap: 8px;
  align-items: center;
}
.qr-actions {
  margin-top: 4px;
}
.qr-err-msg {
  font-size: 12.5px;
  color: var(--danger);
  line-height: 1.7;
  word-break: break-all;
}
</style>
