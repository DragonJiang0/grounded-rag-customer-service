/* ============================================================
   useGroups.js —— 语料分组：左表单 / 右列表。
   分组是配置型小列表，一次性从 /groups/all 取全量，
   搜索在页面本地过滤，不再走后端分页。
   ============================================================ */

import { computed, reactive, ref } from 'vue'
import { groupApi } from '@/api/group'
import { errorMessage } from '@/api/http'

/** 后端返回的全量分组 */
const records = ref([])
const keyword = ref('')
const loading = ref(false)

/** 本地搜索过滤后的分组 */
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return records.value
  return records.value.filter(g => (g.name || '').toLowerCase().includes(kw))
})
const total = computed(() => filtered.value.length)

const editId = ref(null)
const form = reactive({ name: '' })

async function load() {
  loading.value = true
  try {
    records.value = (await groupApi.all()) || []
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载分组失败'))
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editId.value = row.id
  form.name = row.name
}

function resetForm() {
  editId.value = null
  form.name = ''
}

async function save() {
  const name = form.name.trim()
  if (!name) {
    return ElMessage.warning('请输入分组名称')
  }
  if (name.length > 64) {
    return ElMessage.warning('分组名称不能超过 64 个字符')
  }
  try {
    if (editId.value === null) {
      // /groups/add 成功时返回空响应体，不做任何解析
      await groupApi.create({ name })
      ElMessage.success('分组已创建')
    } else {
      // /groups/edit 成功时返回更新后的分组，直接用返回值刷新当前行
      const updated = await groupApi.update(editId.value, { name })
      if (updated) {
        const idx = records.value.findIndex(g => g.id === updated.id)
        if (idx > -1) records.value.splice(idx, 1, updated)
      }
      ElMessage.success('分组已保存')
    }
    resetForm()
    await load()
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存分组失败'))
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除分组「${row.name}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    // /groups/delete/{id}：路径带 id，无请求体，成功 200 且响应体为空
    await groupApi.remove(row.id)
    ElMessage.success('分组已删除')
    if (editId.value === row.id) {
      resetForm()
    }
    await load()
  } catch (e) {
    // 分组被语料引用时后端返回 409，这里直接把 detail 展示出来
    ElMessage.error(errorMessage(e, '删除分组失败'))
  }
}

/** 下拉选择用的全量分组（与列表共用同一份数据） */
const allGroups = computed(() => records.value)

export function useGroups() {
  return {
    records, filtered, allGroups, total, keyword, loading,
    load, loadAll: load, editId, form, openEdit, resetForm, save, remove,
  }
}
