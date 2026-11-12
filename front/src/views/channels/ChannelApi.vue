  <!-- ============================================================
   ChannelApi.vue —— 「API 访问」渠道的配置区。
   「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
   v-model 绑定该渠道下的记录数组 [{ id, type, name, fields }]，
   fields: { name, apiKey, expireAt, lastUsedAt }。

   数据保存在上层（AppChannelPanel -> AppEditView），本组件不落库，
   仅做纯前端操作：新增 / 编辑 / 删除 / 复制。
   ============================================================ -->
<template>
  <!-- 操作区 -->
  <div class="channel-add-bar">
    <el-button type="primary" @click="openCreate">
      <el-icon class="btn-icon"><Plus /></el-icon>新增接入
    </el-button>
  </div>

  <!-- 已配置的接入记录：数据来自 GET /apiList/{appId}，按创建顺序升序 -->
  <el-table v-if="records.length" v-loading="loading" :data="records"
            row-key="id" class="api-table" size="default">
    <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />

    <el-table-column label="API Key" min-width="260">
      <template #default="{ row }">
        <span class="api-key">{{ row.apiKey || '—' }}</span>
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
    <div class="empty-text">尚未配置「API 访问」</div>
    <div class="empty-tip">可添加多个接入</div>
  </div>

  <!-- 新增 / 编辑弹窗 -->
  <el-dialog v-model="formVisible" :title="editIndex === null ? '新增 API 访问' : '编辑 API 访问'"
             width="560px" @closed="resetForm">
    <el-form ref="formRef" :model="draft" :rules="RULES" label-width="80px"
             class="api-form" @submit.prevent>
      <el-form-item label="名称" prop="name">
        <el-input v-model="draft.name" placeholder="如：订单服务" maxlength="64"
                  show-word-limit clearable />
      </el-form-item>

      <el-form-item label="过期时间">
        <el-date-picker v-model="draft.expireAt" type="date" class="form-control"
                        value-format="YYYY-MM-DD" placeholder="不填表示长期有效" clearable />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ editIndex === null ? '确定新增' : '保存修改' }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 新增成功后展示 API Key，可一键复制 -->
  <el-dialog v-model="keyVisible" title="接入创建成功" width="560px" @closed="resetForm">
    <div class="key-tip">请妥善保存 API Key，关闭后仅能在列表中查看。</div>
    <div class="key-box">
      <span class="key-text">{{ createdKey }}</span>
      <el-button type="primary" plain @click="copyKey(createdKey)">
        <el-icon class="btn-icon"><CopyDocument /></el-icon>复制
      </el-button>
    </div>
    <template #footer>
      <el-button type="primary" @click="keyVisible = false">我知道了</el-button>
    </template>
  </el-dialog>

  <!-- 删除确认：需手动输入名称，名称一致才可点「确认删除」 -->
  <el-dialog :model-value="!!removingRow" title="删除接入" width="460px"
             :close-on-click-modal="false" @update:model-value="cancelRemove">
    <template v-if="removingRow">
      <p class="del-line delete-warn">
        删除后该接入的 API Key 将立即失效，且不可恢复。
      </p>
      <p class="del-line">
        请输入接入名称 <b class="del-name">{{ removingRow.name }}</b> 以确认：
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

/** 拉取当前应用的 API 接入列表 */
async function loadList() {
  if (!props.appId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    records.value = (await publishChannelApi.apiList(props.appId)) || []
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载 API 接入失败'))
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
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 64, message: '名称不能超过 64 个字符', trigger: 'blur' },
  ],
}

/* ---------- 新增 / 编辑弹窗 ---------- */
const formRef = ref(null)
const formVisible = ref(false)
/** 新增提交中，避免重复点击 */
const saving = ref(false)
/** null 表示新增，否则为被编辑记录的下标 */
const editIndex = ref(null)
const draft = reactive({ name: '', expireAt: '' })

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(index) {
  const row = records.value[index]
  editIndex.value = index
  Object.assign(draft, {
    name: row?.name || '',
    // 接口返回 "yyyy-MM-dd HH:mm:ss"，日期选择器只要日期部分
    expireAt: (row?.expireAt || '').slice(0, 10),
  })
  formVisible.value = true
}

function resetForm() {
  editIndex.value = null
  Object.assign(draft, { name: '', expireAt: '' })
  formRef.value?.clearValidate()
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const name = draft.name.trim()
  const expireAt = draft.expireAt || ''

  if (editIndex.value === null) {
    // 新增：API Key 由后端生成，成功后弹窗展示
    if (!props.appId) {
      return ElMessage.error('缺少应用 ID，无法新增接入')
    }

    saving.value = true
    try {
      const apiKey = await publishChannelApi.createApi(props.appId, {
        name,
        // 日期选择器只到「天」，后端要求 yyyy-MM-dd HH:mm:ss，补足时刻
        expireAt: expireAt ? `${expireAt} 23:59:59` : null,
      })

      formVisible.value = false
      // 新增后展示明文 Key，方便直接复制
      createdKey.value = apiKey
      keyVisible.value = true
      // 列表以接口为准，重新拉取，避免本地拼装与后端不一致
      await loadList()
    } catch (e) {
      ElMessage.error(errorMessage(e, '新增接入失败'))
    } finally {
      saving.value = false
    }
  } else {
    // 编辑：只改名称与过期时间，所属应用与 API Key 不变
    const row = records.value[editIndex.value]
    if (!row?.id) {
      return ElMessage.error('缺少接入 ID，无法保存')
    }

    saving.value = true
    try {
      await publishChannelApi.editApi({
        id: row.id,
        name,
        // 日期选择器只到「天」，后端要求 yyyy-MM-dd HH:mm:ss；清空则传 null 改回长期有效
        expireAt: expireAt ? `${expireAt} 23:59:59` : null,
      })

      formVisible.value = false
      ElMessage.success('接入已保存')
      // 列表以接口为准，重新拉取
      await loadList()
    } catch (e) {
      ElMessage.error(errorMessage(e, '保存接入失败'))
    } finally {
      saving.value = false
    }
  }
}

/* ---------- 新增结果的 API Key 展示 ---------- */
const keyVisible = ref(false)
const createdKey = ref('')

async function copyKey(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('API Key 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

/* ---------- 删除：校核名称 ---------- */
/** 待删除的记录，非空时展示确认弹窗 */
const removingRow = ref(null)
const confirmName = ref('')
const removing = ref(false)

/** 输入的名称必须与接入名完全一致才能删除 */
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
    return ElMessage.error('缺少接入 ID，无法删除')
  }

  removing.value = true
  try {
    await publishChannelApi.deleteApi(target.id)
    ElMessage.success('接入已删除')
    cancelRemove()
    // 列表以接口为准，重新拉取
    await loadList()
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除接入失败'))
  } finally {
    removing.value = false
  }
}
</script>

<style scoped>
/* ---------- 表单：标签与输入框左右排列 ---------- */
.api-form :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
/* 标签与输入框分居两端，中间留出空隙 */
.api-form :deep(.el-form-item__label) {
  flex: 0 0 auto;
  justify-content: flex-start;
  padding-right: 16px;
}
/* 输入控件占据右侧全部剩余空间，标签与控件各靠一边 */
.api-form :deep(.el-form-item__content > *) {
  flex: 1 1 auto;
  min-width: 0;
}

/* 高度放大到默认（32px）的 1.2 倍 ≈ 38px */
.api-form :deep(.el-input__wrapper) {
  height: 38px;
}
.api-form :deep(.el-input__inner) {
  height: 38px;
  line-height: 38px;
}
.api-form :deep(.el-date-editor) {
  height: 38px;
}
.api-form :deep(.el-date-editor .el-input__wrapper) {
  padding-left: 30px;
}
/* 清空 / 后缀图标垂直居中 */
.api-form :deep(.el-input__prefix),
.api-form :deep(.el-input__suffix) {
  display: flex;
  align-items: center;
}

/* ---------- 列表 ---------- */
.api-table {
  margin-top: 16px;
}
/* API Key 用等宽字体，方便核对 */
.api-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 16.8px;
  color: #3D4B60;
  word-break: break-all;
}

/* 操作列的图标按钮：去掉多余内边距，两个图标紧凑对齐 */
.api-table :deep(.icon-action) {
  padding: 0 6px;
  height: 26px;
}
/* 按钮内图标垂直居中 */
.api-table :deep(.icon-action .el-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 删除确认弹窗（与「应用管理」的删除弹窗一致） ---------- */
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

/* ---------- 创建成功后的 Key 展示 ---------- */
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
.key-text {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  color: var(--text);
  word-break: break-all;
  user-select: all;
}
</style>
