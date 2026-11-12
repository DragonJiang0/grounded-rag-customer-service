<!-- ============================================================
    AppGroupPicker.vue —— 语料分组多选器（AppCreateView 的子组件）。
    常展开的卡片区：可搜索 + 区域滚动，分组再多页面高度也稳定。
    通过 v-model 绑定已选分组 id 数组。
    ============================================================ -->
<template>
  <div class="group-field">
    <!-- 加载中 -->
    <div v-if="loading" v-loading="true" class="group-empty group-loading">
      正在加载分组…
    </div>

    <!-- 无分组 -->
    <div v-else-if="!groups.length" class="group-empty">
      暂无分组，请先到「语料分组」页面创建
    </div>

    <div v-else class="group-panel">
      <el-scrollbar class="group-panel-body" max-height="320px">
        <div class="group-picker">
          <div v-for="g in filteredSorted" :key="g.id"
               :class="['group-option', { picked: isPicked(g.id) }]"
               @click="toggle(g.id)">
            <el-icon class="pick-icon" :size="16">
              <component :is="isPicked(g.id) ? CircleCheckFilled : CirclePlus" />
            </el-icon>
            <span class="pick-label">{{ g.name }}</span>
          </div>
        </div>

        <div v-if="!filteredSorted.length" class="group-no-match">
          没有匹配「{{ keyword }}」的分组
        </div>
      </el-scrollbar>

      <div class="group-panel-foot">
        <span class="picked-count">已选 {{ modelValue.length }} 个</span>
        <span v-if="modelValue.length" class="picked-link" @click="clear">清空</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, CirclePlus } from '@element-plus/icons-vue'

const props = defineProps({
  /** 已选分组 id 数组 */
  modelValue: { type: Array, default: () => [] },
  /** 全量分组，元素为 { id, name } */
  groups: { type: Array, default: () => [] },
  /** 分组列表是否加载中 */
  loading: { type: Boolean, default: false },
  /** 搜索关键词，由父级标签行上的输入框控制 */
  keyword: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

function isPicked(id) {
  return props.modelValue.includes(id)
}

/**
 * 先按关键词过滤，再把已选项排到最前。
 * 排序在过滤之后做，这样搜索时已选中的也会出现在结果顶部。
 */
const filteredSorted = computed(() => {
  const kw = (props.keyword || '').trim().toLowerCase()
  const list = kw
    ? props.groups.filter(g => (g.name || '').toLowerCase().includes(kw))
    : props.groups

  const picked = []
  const rest = []
  for (const g of list) {
    ;(isPicked(g.id) ? picked : rest).push(g)
  }
  return [...picked, ...rest]
})

function toggle(id) {
  const next = [...props.modelValue]
  const i = next.indexOf(id)
  if (i > -1) {
    next.splice(i, 1)
  } else {
    next.push(id)
  }
  emit('update:modelValue', next)
}

function clear() {
  emit('update:modelValue', [])
}
</script>

<style scoped>
.group-empty {
  width: 100%;
  padding: 18px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
}
/* 加载态：给 v-loading 遮罩留出高度 */
.group-loading {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-style: solid;
  background: var(--brand-soft-2);
}

/* 字段整体：选择区 + 底部统计 */
.group-field {
  width: 100%;
}

.group-panel {
  width: 100%;
}
/* 已选统计放在选择区下方，不占用标签行 */
.group-panel-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}
.picked-count {
  font-size: 12.5px;
  color: var(--brand-dark);
}
.picked-link {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--muted);
  cursor: pointer;
  transition: color .15s;
}
.picked-link:hover {
  color: var(--danger);
}

/* 滚动区：高度固定，分组再多也不撑开页面 */
.group-panel-body {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
  padding: 12px;
}
.group-panel-body :deep(.el-scrollbar__view) {
  padding-bottom: 2px;
}

/* 列宽自适应，宽屏下自动铺满 */
.group-picker {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 10px;
  width: 100%;
}
.group-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #4B5A70;
  transition: border-color .15s, background .15s, color .15s;
}
.group-option:hover {
  border-color: #BCD6F7;
  background: var(--brand-soft-2);
}
.group-option.picked {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-weight: 600;
}
.pick-icon {
  flex: 0 0 auto;
  color: #B9CDE6;
}
.group-option.picked .pick-icon {
  color: var(--brand);
}
.pick-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-no-match {
  padding: 26px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
