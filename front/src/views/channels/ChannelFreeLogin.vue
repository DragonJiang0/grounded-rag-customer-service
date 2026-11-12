<!-- ============================================================
    ChannelFreeLogin.vue —— 「免登陆窗口」渠道的配置区。
    「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
    数据全部来自接口，本组件只负责渲染与增改：
      - 列表：GET  /app-publish-channels/windowList/{appId}
      - 新增：POST /app-publish-channels/createWindow/{appId}（返回明文 windowKey）
      - 编辑：POST /app-publish-channels/editWindow（仅改名称）
    窗口地址由 windowKey 拼装，表格中始终可见、可直接复制。
    ============================================================ -->
<template>
  <!-- 操作区 -->
  <div class="channel-add-bar">
    <el-button type="primary" @click="openCreate">
      <el-icon class="btn-icon"><Plus /></el-icon>新增窗口
    </el-button>
  </div>

  <!-- 已配置的免密窗口：数据来自 windowList/{appId} -->
  <el-table v-if="records.length" v-loading="loading" :data="records"
            row-key="id" class="window-table" size="default">
    <el-table-column prop="name" label="窗口名称" min-width="150" show-overflow-tooltip />

    <el-table-column prop="windowKey" label="windowKey" min-width="180" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="key-text">{{ row.windowKey || '—' }}</span>
      </template>
    </el-table-column>

    <el-table-column label="窗口地址" min-width="320">
      <template #default="{ row }">
        <div class="key-box link-box">
          <span class="key-text">{{ linkOf(row) }}</span>
          <el-button type="primary" plain size="small" @click="copyText(linkOf(row), '窗口地址')">
            <el-icon class="btn-icon"><CopyDocument /></el-icon>复制
          </el-button>
        </div>
      </template>
    </el-table-column>

    <el-table-column label="过期时间" width="180">
      <template #default="{ row }">{{ row.expireAt || '长期有效' }}</template>
    </el-table-column>

    <el-table-column label="最后一次使用时间" width="180">
      <template #default="{ row }">{{ row.lastUsedAt || '—' }}</template>
    </el-table-column>

    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ $index, row }">
        <el-tooltip content="编辑" placement="top">
          <el-button text type="primary" size="small" class="icon-action"
                     @click="openEdit($index)">
            <el-icon :size="17"><Edit /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="删除" placement="top">
          <el-button text type="danger" size="small" class="icon-action"
                     @click="openRemove($index, row)">
            <el-icon :size="17"><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </template>
    </el-table-column>
  </el-table>

  <div v-else v-loading="loading" class="record-empty">
    <div class="empty-text">尚未配置「免登陆窗口」</div>
    <div class="empty-tip">可添加多个免密窗口</div>
  </div>

  <!-- 新增 / 编辑弹窗 -->
  <el-dialog v-model="formVisible"
             :title="editIndex === null ? '新增免登陆窗口' : '编辑免登陆窗口'"
             width="560px" @closed="resetForm">
    <el-form ref="formRef" :model="draft" :rules="RULES" label-width="96px"
             class="window-form" @submit.prevent>
      <el-form-item label="窗口名称" prop="name">
        <el-input v-model="draft.name" placeholder="如：官网悬浮窗" maxlength="64"
                  show-word-limit clearable />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ editIndex === null ? '确定新增' : '保存修改' }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 新增成功后展示免密链接，可一键复制 -->
  <el-dialog v-model="keyVisible" title="免密窗口创建成功" width="560px">
    <div class="key-tip">
      以下窗口地址可直接分享使用，列表中也会一直展示，可随时复制。
    </div>
    <div class="key-box">
      <span class="key-text">{{ createdLink }}</span>
      <el-button type="primary" plain @click="copyText(createdLink, '窗口地址')">
        <el-icon class="btn-icon"><CopyDocument /></el-icon>复制
      </el-button>
    </div>
    <template #footer>
      <el-button type="primary" @click="keyVisible = false">我知道了</el-button>
    </template>
  </el-dialog>

  <!-- 删除确认：需手动输入名称，名称一致才可点「确认删除」 -->
  <el-dialog :model-value="!!removingRow" title="删除免密窗口" width="460px"
             :close-on-click-modal="false" @update:model-value="cancelRemove">
    <template v-if="removingRow">
      <p class="del-line delete-warn">
        删除后该窗口的免密链接将立即失效，且不可恢复。
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
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus, CopyDocument, Edit, Delete } from '@element-plus/icons-vue'
import { publishChannelApi } from '@/api/publishChannel'
import { errorMessage } from '@/api/http'

const props = defineProps({
  /** 渠道定义（label / desc / multi 等） */
  channel: { type: Object, default: null },
  /** 所属应用 ID：列表查询与新增接入都作为路径参数传给后端 */
  appId: { type: [Number, String], default: null },
})

/* ---------- 列表：数据源为接口，本地只做只读渲染 ---------- */
const records = ref([])
const loading = ref(false)

/**
 * 窗口地址前缀：当前域名（含端口）+ /chat/chat/
 * 例：https://host:8080/app/edit/12 → https://host:8080/chat/chat/{key}
 */
function linkPrefix() {
  return `${window.location.origin}/chat/chat/`
}

/** 用列表返回的 windowKey 拼接完整窗口地址，始终可见 */
function linkOf(row) {
  return row?.windowKey ? `${linkPrefix()}${row.windowKey}` : '—'
}

/** 拉取当前应用的免密窗口列表 */
async function loadList() {
  if (!props.appId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    records.value = (await publishChannelApi.windowList(props.appId)) || []
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载免密窗口失败'))
  } finally {
    loading.value = false
  }
}

// 挂载即拉取；appId 变化（切换应用）时重新拉
onMounted(loadList)
watch(() => props.appId, loadList)

/* ---------- 表单校验 ---------- */
const RULES = {
  name: [
    { required: true, message: '请输入窗口名称', trigger: 'blur' },
    { max: 64, message: '名称不能超过 64 个字符', trigger: 'blur' },
  ],
}

/* ---------- 新增 / 编辑弹窗 ---------- */
const formRef = ref(null)
const formVisible = ref(false)
/** 提交中，避免重复点击 */
const saving = ref(false)
/** null 表示新增，否则为被编辑记录的下标 */
const editIndex = ref(null)
const draft = reactive({ name: '' })

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(index) {
  const row = records.value[index]
  editIndex.value = index
  Object.assign(draft, { name: row?.name || '' })
  formVisible.value = true
}

function resetForm() {
  editIndex.value = null
  Object.assign(draft, { name: '' })
  formRef.value?.clearValidate()
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const name = draft.name.trim()

  if (editIndex.value === null) {
    // 新增：windowKey 由后端生成，成功后弹窗展示完整免密链接
    if (!props.appId) {
      return ElMessage.error('缺少应用 ID，无法新增免密窗口')
    }

    saving.value = true
    try {
      const windowKey = await publishChannelApi.createWindow(props.appId, { name })

      formVisible.value = false
      // 展示完整链接，方便直接复制
      createdLink.value = windowKey ? `${linkPrefix()}${windowKey}` : ''
      keyVisible.value = true
      // 列表以接口为准，重新拉取（表格中的窗口地址也会一直展示）
      await loadList()
    } catch (e) {
      ElMessage.error(errorMessage(e, '新增免密窗口失败'))
    } finally {
      saving.value = false
    }
  } else {
    // 编辑：仅改名称
    const row = records.value[editIndex.value]
    if (!row?.id) {
      return ElMessage.error('缺少窗口 ID，无法保存')
    }

    saving.value = true
    try {
      await publishChannelApi.editWindow({ id: row.id, name })
      formVisible.value = false
      ElMessage.success('免密窗口已保存')
      await loadList()
    } catch (e) {
      ElMessage.error(errorMessage(e, '保存免密窗口失败'))
    } finally {
      saving.value = false
    }
  }
}

/* ---------- 新增结果的窗口地址展示 ---------- */
const keyVisible = ref(false)
const createdLink = ref('')

async function copyText(text, label = '内容') {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制`)
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
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

/**
 * @param index 记录下标
 * @param row   表格当前行（模板已提供，避免再次按下标取值）
 */
function openRemove(index, row) {
  removingRow.value = row || records.value[index] || null
  confirmName.value = ''
}

function cancelRemove() {
  removingRow.value = null
  confirmName.value = ''
}

async function confirmRemove() {
  if (!canConfirmRemove.value) return

  const target = removingRow.value
  if (!target.id) {
    return ElMessage.error('缺少窗口 ID，无法删除')
  }

  removing.value = true
  try {
    await publishChannelApi.deleteWindow(target.id)
    ElMessage.success('免密窗口已删除')
    cancelRemove()
    // 列表以接口为准，重新拉取
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除免密窗口失败'))
  } finally {
    removing.value = false
  }
}
</script>

<style scoped>
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
/* 输入控件占据右侧空间，整体缩窄到九成，给左侧标签留出位置 */
.window-form :deep(.el-form-item__content > *) {
  flex: 0 0 90%;
  max-width: 90%;
  min-width: 0;
}

/* 高度放大到默认（32px）的 1.2 倍 ≈ 38px */
.window-form :deep(.el-input__wrapper) {
  height: 38px;
}
.window-form :deep(.el-input__inner) {
  height: 38px;
  line-height: 38px;
}

/* ---------- 列表 ---------- */
.window-table {
  margin-top: 16px;
}
/* 免密标识 / 链接用等宽字体，方便核对 */
.key-text {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16.8px;
  color: #3D4B60;
  word-break: break-all;
  user-select: all;
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

/* ---------- 创建成功后的链接展示 ---------- */
.key-tip {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 12px;
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
  padding: 6px 10px;
}

/* ---------- 删除确认弹窗（与「API 访问」的删除弹窗一致） ---------- */
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
</style>
