/* ============================================================
   useCorpus.js —— 语料：列表走服务端分页，增删改直接落库。
   ============================================================ */

import { reactive, ref } from 'vue'
import { corpusApi } from '@/api/corpus'
import { errorMessage } from '@/api/http'
import { useEngineCorpus } from './useEngineCorpus'

const records = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)

/** 查询条件：所属分组 / 关键词（标准问）/ 答案 */
const query = reactive({ groupId: null, keyword: '', answer: '' })

/** null 表示新增，否则为正在编辑的语料 id */
const editId = ref(null)
/** 抽屉是否展开 */
const drawerVisible = ref(false)
const form = reactive({ name: '', synonyms: [], groupId: null, answer: '' })

const engineCorpus = useEngineCorpus()
let searchTimer = null

async function load() {
  loading.value = true
  try {
    // 后端入参为 page / size / groupId / keywords[] / answer
    const page = await corpusApi.page({
      page: current.value,
      size: size.value,
      groupId: query.groupId ?? undefined,
      // 关键词按空白拆分，多个关键词之间是「或」
      keywords: query.keyword.trim() ? query.keyword.trim().split(/\s+/).filter(Boolean) : undefined,
      answer: query.answer.trim() || undefined,
    })
    const data = page || {}
    // 返回结构与 Corpus 完全一致，直接落库，不做字段映射
    records.value = data.records || []
    total.value = data.total ?? 0
    // 后端会把越界页码回落，这里跟随返回值
    current.value = data.current ?? current.value
    size.value = data.size ?? size.value
  } catch (e) {
    ElMessage.error(errorMessage(e, '加载语料失败'))
  } finally {
    loading.value = false
  }
}

/* ------------------------------------------------------------ 展示辅助 */

/**
 * 语料的问题文本：接口的 questions 是 { id, corpusId, groupId, question } 对象数组，
 * 统一抽成纯文本（第一条即标准问），列表与抽屉都从这里取。
 */
function questionsOf(row) {
  return (row.questions || []).map(q => (typeof q === 'string' ? q : q?.question)).filter(Boolean)
}

/**
 * 分组 id 转分组展示名。
 * Corpus 只带 groupId，分组名由「分组列表」接口提供，这里读一份注册表做映射，
 * 不直接依赖 useGroups，避免两个 store 互相引用。
 */
const groupNames = ref([])

/** 由 useGroups 在加载全量分组后回填，保证分组名可用于展示 */
function setGroupNames(list) {
  groupNames.value = list || []
}

function categoryName(groupId) {
  if (groupId === null || groupId === undefined) {
    return '未分组'
  }
  const g = groupNames.value.find(x => x.id === groupId)
  return g ? g.name : String(groupId)
}

/** 条件变更：回到第 1 页再查 */
function search() {
  current.value = 1
  load()
}

/** 关键词 / 答案输入防抖，避免每敲一个字都请求 */
function onFilterInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(search, 300)
}

function resetQuery() {
  query.groupId = null
  query.keyword = ''
  query.answer = ''
  search()
}

function onPageChange(page) {
  current.value = page
  load()
}

function onSizeChange(pageSize) {
  size.value = pageSize
  current.value = 1
  load()
}

/* ------------------------------------------------------------ 抽屉表单 */

/** 点击「新增语料」：清空表单并打开抽屉 */
function openCreate() {
  resetForm()
  drawerVisible.value = true
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    // 列表已带 questions，直接抽成标签（第一条为标准问）
    synonyms: questionsOf(row),
    groupId: row.groupId ?? null,
    answer: row.answer,
  })
  drawerVisible.value = true
}

function resetForm() {
  editId.value = null
  Object.assign(form, { name: '', synonyms: [], groupId: null, answer: '' })
}

/** 关闭抽屉（右上角 × / 遮罩 / 取消按钮） */
function closeDrawer() {
  drawerVisible.value = false
  resetForm()
}

async function save() {
  const name = form.name.trim()
  const answer = form.answer.trim()
  if (!name) {
    return ElMessage.warning('请输入名称')
  }
  if (!answer) {
    return ElMessage.warning('请输入答案')
  }
  if (form.groupId === null || form.groupId === undefined) {
    return ElMessage.warning('请选择所属分组')
  }

  // 标签按录入顺序即为 questions：第一条是标准问，其余是相似问
  const questions = form.synonyms.map(s => s.trim()).filter(Boolean)
  const payload = { name, answer, groupId: form.groupId, questions }

  try {
    if (editId.value === null) {
      // /corpus/add：成功 200，无响应体
      await corpusApi.create(payload)
      ElMessage.success('语料已新增')
    } else {
      // /corpus/edit：questions 为全量覆盖
      await corpusApi.update(editId.value, payload)
      ElMessage.success('语料已保存')
    }
    closeDrawer()
    engineCorpus.invalidate()
    await load()
  } catch (e) {
    ElMessage.error(errorMessage(e, '保存语料失败'))
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除语料「${row.name || row.answer}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await corpusApi.remove(row.id)
    ElMessage.success('语料已删除')
    if (editId.value === row.id) {
      closeDrawer()
    }
    // 删掉当前页最后一条时往前退一页
    if (records.value.length === 1 && current.value > 1) {
      current.value -= 1
    }
    engineCorpus.invalidate()
    await load()
  } catch (e) {
    ElMessage.error(errorMessage(e, '删除语料失败'))
  }
}

/**
 * 列表里的启用 / 停用开关。
 * 接口是「相互切换」语义：不传目标值，以返回的状态为准回填。
 */
async function toggleEnabled(row) {
  const previous = row.enabled
  // 先乐观切换，拿到返回值后再对齐，失败则回滚
  row.enabled = !previous
  try {
    const next = await corpusApi.toggleEnabled(row.id)
    row.enabled = typeof next === 'boolean' ? next : !previous
    engineCorpus.invalidate()
    ElMessage.success(row.enabled ? '已启用' : '已停用')
  } catch (e) {
    row.enabled = previous
    ElMessage.error(errorMessage(e, '操作失败'))
  }
}

export function useCorpus() {
  return {
    records, total, current, size, loading,
    query, load, search, onFilterInput, resetQuery, onPageChange, onSizeChange,
    editId, drawerVisible, form, openCreate, openEdit, closeDrawer, save, remove, toggleEnabled,
    categoryName, setGroupNames, questionsOf,
  }
}
