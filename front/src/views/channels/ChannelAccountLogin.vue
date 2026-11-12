<!-- ============================================================
    ChannelAccountLogin.vue —— 「账号密码登录窗口」渠道的配置区。
    「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
    数据全部来自接口：
      - 列表：GET  /app/setting/passwordWindowList/{appId}（纯数组，创建时间倒序）
      - 新增：POST /app/setting/createPasswordWindow/{appId}
              body: { name, username }，返回 string（windowKey，非统一包装）
    创建后密码由服务端初始化为 1234llm，窗口默认启用；
    同一应用内窗口名与登录账号不可重复。
    ============================================================ -->
<template>
  <!-- 登录地址：由当前地址 + /chat/login/{appKey} 拼装，始终可见、可复制 -->
  <div class="login-url-row">
    <span class="login-url-label">登录地址</span>
    <div class="key-box link-box">
      <span class="key-text">{{ loginUrl }}</span>
      <el-button type="primary" plain size="small" @click="copyText(loginUrl, '登录地址')">
        <el-icon class="btn-icon"><CopyDocument /></el-icon>复制
      </el-button>
    </div>
  </div>

  <div class="channel-add-bar">
    <el-button type="primary" @click="openCreate">
      <el-icon class="btn-icon"><Plus /></el-icon>新增账户
    </el-button>
  </div>

  <el-table v-if="records.length" v-loading="loading" :data="records"
            row-key="id" class="window-table" size="default">
    <el-table-column prop="name" label="窗口名称" min-width="150" show-overflow-tooltip />

    <el-table-column prop="username" label="登录账号" min-width="150" show-overflow-tooltip />

    <el-table-column label="启用 / 禁用" width="120">
      <template #default="{ row }">
        <el-switch :model-value="row.status === 1"
                   :loading="togglingId === row.id"
                   @change="onToggleStatus(row)" />
      </template>
    </el-table-column>

    <el-table-column prop="createdAt" label="创建时间" width="180" />

    <el-table-column label="最后使用时间" width="180">
      <template #default="{ row }">{{ row.lastUsedAt || '—' }}</template>
    </el-table-column>

    <el-table-column label="操作" width="120" fixed="right">
      <template #default="{ row }">
        <el-tooltip content="重置密码" placement="top">
          <el-button text type="primary" size="small" class="icon-action"
                     @click="openReset(row)">
            <el-icon :size="17"><RefreshLeft /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="删除" placement="top">
          <el-button text type="danger" size="small" class="icon-action"
                     @click="openRemove(row)">
            <el-icon :size="17"><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </template>
    </el-table-column>
  </el-table>

  <div v-else v-loading="loading" class="record-empty">
    <div class="empty-text">尚未配置「账号密码登录窗口」</div>
    <div class="empty-tip">该渠道仅允许配置一个接入</div>
  </div>

  <!-- 删除确认：需手动输入名称，名称一致才可点「确认删除」 -->
  <el-dialog :model-value="!!removingRow" title="删除账号密码登录窗口" width="460px"
             :close-on-click-modal="false" @update:model-value="cancelRemove">
    <template v-if="removingRow">
      <p class="del-line delete-warn">
        删除后该账号将无法再登录对话窗口，且不可恢复。
      </p>
      <p class="del-line">
        请输入窗口名称 <b class="del-name">{{ removingRow.name }}</b> 以确认：
      </p>
      <el-input v-model="confirmName" size="large" clearable
                :placeholder="removingRow.name"
                @keyup.enter="canConfirmRemove && confirmRemove()" />
    </template>

    <template #footer>
      <el-button size="large" @click="cancelRemove">取消</el-button>
      <el-button type="danger" size="large" :disabled="!canConfirmRemove"
                 :loading="removing" @click="confirmRemove">
        确认删除
      </el-button>
    </template>
  </el-dialog>

  <!-- 重置密码确认 -->
  <el-dialog :model-value="!!resettingRow" title="重置登录密码" width="460px"
             :close-on-click-modal="false" @update:model-value="cancelReset">
    <template v-if="resettingRow">
      <p class="del-line">
        将把窗口 <b class="del-name">{{ resettingRow.name }}</b> 的登录密码重置为默认口令
        <b class="del-name">1234llm</b>，且不会改变当前的启用/禁用状态。
      </p>
    </template>

    <template #footer>
      <el-button size="large" @click="cancelReset">取消</el-button>
      <el-button type="primary" size="large" :loading="resetting" @click="confirmReset">
        确认重置
      </el-button>
    </template>
  </el-dialog>

  <!-- 新增账号密码登录窗口 -->
  <el-dialog v-model="formVisible" title="新增账号密码登录窗口" width="560px"
             @closed="resetForm">
    <el-form ref="formRef" :model="draft" :rules="RULES" label-width="96px"
             class="window-form" @submit.prevent>
      <el-form-item label="窗口名称" prop="name">
        <el-input v-model="draft.name" placeholder="如：员工工作台" maxlength="64"
                  show-word-limit clearable />
      </el-form-item>
      <el-form-item label="登录账号" prop="username">
        <el-input v-model="draft.username" placeholder="登录使用的账号" maxlength="64"
                  show-word-limit clearable />
      </el-form-item>
      <el-form-item>
        <div class="pwd-tip">
          初始密码由服务端统一设为 <b>1234llm</b>，请提醒使用者首次登录后修改。
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">确定新增</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, reactive, watch } from 'vue'
import { Plus, Delete, RefreshLeft, CopyDocument } from '@element-plus/icons-vue'
import { publishChannelApi } from '@/api/publishChannel'
import { errorMessage } from '@/api/http'

const props = defineProps({
  modelValue: { type: Array, required: true },
  channel: { type: Object, default: null },
  /** 所属应用 ID：列表查询与新增接入都作为路径参数传给后端 */
  appId: { type: [Number, String], default: null },
  /** 所属应用的 AppKey：用于拼装账号密码登录窗口地址 */
  appKey: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

/* ---------- 登录地址：当前域名（含端口）+ /chat/login/{appKey} ---------- */
const loginUrl = computed(() => `${window.location.origin}/chat/login/${props.appKey || ''}`)

async function copyText(text, label = '内容') {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制`)
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/* ---------- 列表：数据源为接口，本地只做只读渲染 ---------- */
const records = ref([])
const loading = ref(false)

/** 拉取当前应用的账号密码窗口列表 */
async function loadList() {
  if (!props.appId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    const list = await publishChannelApi.passwordWindowList(props.appId)
    records.value = Array.isArray(list) ? list : []
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载账号密码窗口失败'))
  } finally {
    loading.value = false
  }
}

// 挂载即拉取；appId 变化（切换应用）时重新拉
onMounted(loadList)
watch(() => props.appId, loadList)

/* ---------- 启用 / 禁用 ---------- */
/** 正在切换状态的记录 ID，用于给对应开关加 loading */
const togglingId = ref(null)

/**
 * 切换启用/禁用：接口按 ID 反转状态，成功后重新拉取列表以刷新状态。
 * 失败时开关仍绑定 row.status，不会出现误显示。
 * @param {{ id:number }} row
 */
async function onToggleStatus(row) {
  if (!row?.id) return
  togglingId.value = row.id
  try {
    await publishChannelApi.togglePasswordWindowStatus(row.id)
    ElMessage.success('窗口状态已切换')
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '切换窗口状态失败'))
  } finally {
    togglingId.value = null
  }
}

/* ---------- 表单校验 ---------- */
const RULES = {
  name: [
    { required: true, message: '请输入窗口名称', trigger: 'blur' },
    { max: 64, message: '名称不能超过 64 个字符', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 64, message: '账号不能超过 64 个字符', trigger: 'blur' },
  ],
}

/* ---------- 新增弹窗 ---------- */
const formRef = ref(null)
const formVisible = ref(false)
const saving = ref(false)
const draft = reactive({ name: '', username: '' })

function openCreate() {
  resetForm()
  formVisible.value = true
}

function resetForm() {
  Object.assign(draft, { name: '', username: '' })
  formRef.value?.clearValidate()
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!props.appId) {
    return ElMessage.error('缺少应用 ID，无法新增登录窗口')
  }

  saving.value = true
  try {
    await publishChannelApi.createPasswordWindow(props.appId, {
      name: draft.name.trim(),
      username: draft.username.trim(),
    })
    formVisible.value = false
    ElMessage.success('账号密码登录窗口已创建')
    // 列表以接口为准，重新拉取
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '新增账号密码登录窗口失败'))
  } finally {
    saving.value = false
  }
}

/* ---------- 删除：校核名称 ---------- */
/** 待删除的记录，非空时展示确认弹窗 */
const removingRow = ref(null)
const confirmName = ref('')
const removing = ref(false)

/** 输入的名称必须与窗口名完全一致才能删除 */
const canConfirmRemove = computed(
  () => !!removingRow.value && confirmName.value.trim() === removingRow.value.name,
)

function openRemove(row) {
  removingRow.value = row || null
  confirmName.value = ''
}

function cancelRemove() {
  removingRow.value = null
  confirmName.value = ''
}

async function confirmRemove() {
  if (!canConfirmRemove.value) return

  const target = removingRow.value
  if (!target?.id) {
    return ElMessage.error('缺少窗口 ID，无法删除')
  }

  removing.value = true
  try {
    await publishChannelApi.deletePasswordWindow(target.id)
    ElMessage.success('账号密码登录窗口已删除')
    cancelRemove()
    // 列表以接口为准，重新拉取
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除账号密码登录窗口失败'))
  } finally {
    removing.value = false
  }
}

/* ---------- 重置密码 ---------- */
/** 待重置密码的记录，非空时展示确认弹窗 */
const resettingRow = ref(null)
const resetting = ref(false)

function openReset(row) {
  resettingRow.value = row || null
}

function cancelReset() {
  resettingRow.value = null
}

async function confirmReset() {
  const target = resettingRow.value
  if (!target?.id) {
    return ElMessage.error('缺少窗口 ID，无法重置密码')
  }

  resetting.value = true
  try {
    await publishChannelApi.resetPasswordWindowPassword(target.id)
    ElMessage.success('密码已重置为 1234llm')
    cancelReset()
  } catch (e) {
    ElMessage.error(errorMessage(e, '重置密码失败'))
  } finally {
    resetting.value = false
  }
}
</script>

<style scoped>
/* ---------- 登录地址行 ---------- */
.login-url-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
.login-url-label {
  flex: 0 0 auto;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text);
}
.key-text {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16.8px;
  color: #3D4B60;
  word-break: break-all;
  user-select: all;
}
.key-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
}
.link-box {
  flex: 1;
  min-width: 0;
  padding: 6px 10px;
}

/* ---------- 表单：标签与输入框左右排列 ---------- */
.window-form :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.window-form :deep(.el-form-item__label) {
  flex: 0 0 auto;
  justify-content: flex-start;
  padding-right: 16px;
}
.window-form :deep(.el-form-item__content > *) {
  flex: 0 0 90%;
  max-width: 90%;
  min-width: 0;
}
.window-form :deep(.el-input__wrapper) {
  height: 38px;
}
.window-form :deep(.el-input__inner) {
  height: 38px;
  line-height: 38px;
}

/* 初始密码提示 */
.pwd-tip {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--muted);
}
.pwd-tip b {
  color: var(--brand-dark);
}

/* ---------- 列表 ---------- */
.window-table {
  margin-top: 16px;
}
/* 操作列的图标按钮 */
.window-table :deep(.icon-action) {
  padding: 0 6px;
  height: 26px;
}
.window-table :deep(.icon-action .el-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 删除确认弹窗（与「免登陆窗口」一致） ---------- */
.del-line {
  margin: 0 0 12px;
  font-size: 13.5px;
  line-height: 1.7;
  color: #4B5A70;
}
/* 风险提示用红色，与正文区分 */
.delete-warn {
  color: var(--danger);
}
/* 需输入的名称：底色块 + 加粗，便于对照 */
.del-name {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--brand-soft-2);
  color: var(--text);
  font-weight: 700;
  word-break: break-all;
}

.record-empty {
  margin-top: 16px;
  padding: 32px 0;
  text-align: center;
}
.empty-text {
  font-size: 14px;
  color: var(--text);
}
.empty-tip {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--muted);
}
</style>
