/* ============================================================
   useEngineCorpus.js —— 对话匹配引擎的语料副本。

   匹配仍在浏览器本地完成（后端只提供 CRUD），所以这里要把全量语料取回来。
   语料被增删改后调用 invalidate()，下次访问会重新拉取。
   ============================================================ */

import { computed, ref } from 'vue'
import { corpusApi } from '@/api/corpus'

const items = ref([])
const loaded = ref(false)

/**
 * 语料 id -> 问题文本数组（第一条为标准问，其余为相似问）。
 * 接口的 questions 是对象数组（{ id, corpusId, groupId, question }），
 * 这里统一抽成纯文本，供列表、抽屉与匹配复用。
 */
const questionMap = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    map.set(it.id, textOf(it))
  }
  return map
})

/** 把一条语料的 questions 抽成纯文本数组 */
function textOf(item) {
  return (item.questions || []).map(q => (typeof q === 'string' ? q : q?.question)).filter(Boolean)
}

/** 取某条语料的问题文本，供列表与匹配复用 */
function questionsOf(id) {
  return questionMap.value.get(id) || []
}

/** 一次拉取的分页容量，用于分页接口兜底时把全量翻完 */
const FALLBACK_SIZE = 500

/**
 * 分页接口兜底：按 id 升序逐页取，直到取满 total。
 * 正常路径只走一次请求（总数不超过一页时）。
 */
async function fetchAllByPage() {
  const all = []
  let page = 1
  for (;;) {
    const data = await corpusApi.page({ page, size: FALLBACK_SIZE })
    const records = data?.records || []
    all.push(...records)
    const total = data?.total ?? all.length
    if (!records.length || all.length >= total) {
      break
    }
    page += 1
  }
  return all
}

async function ensureLoaded(force = false) {
  if (loaded.value && !force) {
    return items.value
  }
  items.value = await fetchAllByPage()
  loaded.value = true
  return items.value
}

function invalidate() {
  loaded.value = false
}

export function useEngineCorpus() {
  return { items, loaded, ensureLoaded, invalidate, questionsOf, textOf }
}
