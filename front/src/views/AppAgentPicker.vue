<!-- ============================================================
    AppAgentPicker.vue —— 多 Agent 选择器（AppEditView 的子组件）。
    样式与 AppGroupPicker 保持一致：可搜索 + 区域滚动 + 底部统计。
    Agent 元数据里没有 id，名称即唯一标识，v-model 绑定已选名称数组。
    ============================================================ -->
<template>
  <div class="agent-field">
    <!-- 加载中 -->
    <div v-if="loading" v-loading="true" class="agent-empty agent-loading">
      正在加载 Agent…
    </div>

    <!-- 无 Agent -->
    <div v-else-if="!agents.length" class="agent-empty">
      暂无已注册的 Agent，请先到「自定义Agent」页面查看
    </div>

    <div v-else class="agent-panel">
      <el-scrollbar class="agent-panel-body" max-height="320px">
        <div class="agent-picker">
          <div v-for="a in filteredSorted" :key="a.name"
               :class="['agent-option', { picked: isPicked(a.name) }]"
               :title="a.description || a.name"
               @click="toggle(a.name)">
            <el-icon class="pick-icon" :size="16">
              <component :is="isPicked(a.name) ? CircleCheckFilled : CirclePlus" />
            </el-icon>
            <div class="pick-body">
              <div class="pick-label">{{ a.name }}</div>
              <div class="pick-desc">{{ a.description || '暂无描述' }}</div>
            </div>
            <span class="pick-count" :title="`${a.goals.length} 个目标 / ${a.actions.length} 个动作`">
              {{ a.goals.length }}/{{ a.actions.length }}
            </span>
          </div>
        </div>

        <div v-if="!filteredSorted.length" class="agent-no-match">
          没有匹配「{{ keyword }}」的 Agent
        </div>
      </el-scrollbar>

      <div class="agent-panel-foot">
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
  /** 已选 Agent 名称数组 */
  modelValue: { type: Array, default: () => [] },
  /** 全量 Agent，元素为 { name, description, goals, actions } */
  agents: { type: Array, default: () => [] },
  /** Agent 列表是否加载中 */
  loading: { type: Boolean, default: false },
  /** 搜索关键词，由父级标签行上的输入框控制 */
  keyword: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

function isPicked(name) {
  return props.modelValue.includes(name)
}

/**
 * 先按关键词过滤（名称 / 描述都参与），再把已选项排到最前。
 * 排序在过滤之后做，这样搜索时已选中的也会出现在结果顶部。
 */
const filteredSorted = computed(() => {
  const kw = (props.keyword || '').trim().toLowerCase()
  const list = kw
    ? props.agents.filter(a =>
        (a.name || '').toLowerCase().includes(kw) ||
        (a.description || '').toLowerCase().includes(kw))
    : props.agents

  const picked = []
  const rest = []
  for (const a of list) {
    ;(isPicked(a.name) ? picked : rest).push(a)
  }
  return [...picked, ...rest]
})

function toggle(name) {
  const next = [...props.modelValue]
  const i = next.indexOf(name)
  if (i > -1) {
    next.splice(i, 1)
  } else {
    next.push(name)
  }
  emit('update:modelValue', next)
}

function clear() {
  emit('update:modelValue', [])
}
</script>

<style scoped>
.agent-empty {
  width: 100%;
  padding: 18px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
}
/* 加载态：给 v-loading 遮罩留出高度 */
.agent-loading {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-style: solid;
  background: var(--brand-soft-2);
}

.agent-field {
  width: 100%;
}

.agent-panel {
  width: 100%;
}
/* 已选统计放在选择区下方，不占用标签行 */
.agent-panel-foot {
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

/* 滚动区：高度固定，Agent 再多也不撑开页面 */
.agent-panel-body {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
  padding: 12px;
}
.agent-panel-body :deep(.el-scrollbar__view) {
  padding-bottom: 2px;
}

/* 与语料分组同一套网格：宽屏下自动铺满 */
.agent-picker {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px;
  width: 100%;
}
.agent-option {
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
.agent-option:hover {
  border-color: #BCD6F7;
  background: var(--brand-soft-2);
}
.agent-option.picked {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand-dark);
}
.pick-icon {
  flex: 0 0 auto;
  color: #B9CDE6;
}
.agent-option.picked .pick-icon {
  color: var(--brand);
}
/* 名称 + 描述两行，名称过长省略 */
.pick-body {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pick-label {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pick-desc {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agent-option.picked .pick-desc {
  color: #5A7391;
}
/* 右侧目标/动作数量，仅作简要信息，不参与选择 */
.pick-count {
  flex: 0 0 auto;
  font-size: 11.5px;
  font-weight: 400;
  color: var(--muted);
  padding: 1px 6px;
  border-radius: 20px;
  background: var(--brand-soft-2);
}
.agent-option.picked .pick-count {
  background: #fff;
  color: var(--brand-dark);
}
.agent-no-match {
  padding: 26px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
