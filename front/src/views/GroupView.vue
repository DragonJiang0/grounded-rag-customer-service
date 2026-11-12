<!-- ============================================================
     GroupView.vue —— 语料分组：左表单 / 右列表。
     列表一次性拉全量（/groups/all），搜索在本地过滤，
     行数多时用虚拟滚动渲染。
     ============================================================ -->
<template>
  <PageHead title="语料分组"
            tip="分组用于归类语料：左侧编辑分组信息，右侧搜索并管理分组列表。
一个应用可绑定多个分组，删除分组前请先确认组内语料已迁移或删除。" />

  <div class="group-layout">
    <!-- 左：表单 -->
    <div class="card group-form-card">
      <PanelTitle :icon="EditPen" :title="editId === null ? '新增分组' : '编辑分组'" />

      <el-form label-position="top" size="large" @submit.prevent>
        <el-form-item label="分组名称">
          <el-input v-model="form.name" size="large" class="form-control"
                    maxlength="20" show-word-limit clearable
                    placeholder="请输入分组名称，如：账户安全"
                    @keyup.enter="save" />
        </el-form-item>
      </el-form>

      <el-button type="primary" size="large" class="add-group-btn" @click="save">
        <el-icon class="btn-icon" :size="18"><component :is="editId === null ? Plus : Check" /></el-icon>
        {{ editId === null ? '新增分组' : '保存修改' }}
      </el-button>

      <el-button v-if="editId !== null" text size="large" class="cancel-edit" @click="resetForm">
        取消编辑
      </el-button>
    </div>

    <!-- 右：列表 -->
    <div v-loading="loading" class="card group-list-card">
      <PanelTitle :icon="Files" title="分组列表">
        <template #extra>共 {{ total }} 个</template>
      </PanelTitle>

      <el-input v-model="keyword" size="large" class="form-control" clearable
                placeholder="搜索分组名称">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <!-- 虚拟列表：只渲染可视区域内的行 -->
      <el-scrollbar v-if="filtered.length" ref="scrollbarRef" class="group-list-scroll"
                    @scroll="onScroll">
        <div class="group-list" :style="{ height: `${filtered.length * ROW_HEIGHT}px` }">
          <div v-for="item in visibleRows" :key="item.row.id"
               class="group-virtual-row"
               :style="{ transform: `translateY(${item.top}px)`, height: `${ROW_HEIGHT}px` }">
            <div :class="['group-record', 'group-record-row', { active: item.row.id === editId }]">
              <span class="group-name">{{ item.row.name }}</span>
              <div class="row-actions">
                <el-button plain size="large" @click="openEdit(item.row)">
                  <el-icon class="btn-icon"><Edit /></el-icon>编辑
                </el-button>
                <el-button plain type="danger" size="large" @click="remove(item.row)">
                  <el-icon class="btn-icon"><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <EmptyHint v-else-if="!loading" :icon="Search" text="没有匹配的分组" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { EditPen, Plus, Check, Files, Search, Edit, Delete } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import EmptyHint from '@/components/EmptyHint.vue'
import { useGroups } from '@/stores/useGroups'

const { filtered, total, keyword, loading, load, editId, form, openEdit, resetForm, save, remove } = useGroups()

/* ---------- 虚拟滚动 ---------- */
/** 单行高度（含行间距），与 ROW_GAP 一起决定 translateY 的步长 */
const ROW_HEIGHT = 66
/** 上下各多渲染几行，滚动时不会看到空白 */
const OVERSCAN = 5

const scrollbarRef = ref(null)
const scrollTop = ref(0)
const viewportHeight = ref(0)

const startIndex = computed(() =>
  Math.max(0, Math.floor(scrollTop.value / ROW_HEIGHT) - OVERSCAN))
const endIndex = computed(() =>
  Math.min(filtered.value.length, Math.ceil((scrollTop.value + viewportHeight.value) / ROW_HEIGHT) + OVERSCAN))

const visibleRows = computed(() =>
  filtered.value.slice(startIndex.value, endIndex.value).map((row, i) => ({
    row,
    top: (startIndex.value + i) * ROW_HEIGHT,
  })))

function onScroll({ scrollTop: top }) {
  scrollTop.value = top
}

/** 内容或视口尺寸变化时同步一次视口高度 */
function syncViewport() {
  const wrap = scrollbarRef.value?.wrapRef
  viewportHeight.value = wrap?.clientHeight || 0
  if (scrollTop.value > (wrap?.scrollHeight || 0)) {
    scrollTop.value = 0
  }
}

watch([filtered, () => keyword.value], () => {
  // 搜索后回到顶部，避免停留在越界的偏移上
  scrollTop.value = 0
  scrollbarRef.value?.setScrollTop(0)
})

onMounted(async () => {
  await load()
  await nextTick()
  syncViewport()
  window.addEventListener('resize', syncViewport)
})
</script>

<style scoped>
.group-list-scroll {
  /* 列表区域固定高度，超出后自身滚动，触发虚拟渲染 */
  height: calc(100vh - 320px);
  min-height: 240px;
  margin-top: 16px;
}
.group-list {
  position: relative;
}
.group-virtual-row {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  box-sizing: border-box;
  padding-bottom: 10px;
}
.group-record-row {
  height: 100%;
}
.group-list-scroll :deep(.el-scrollbar__view) {
  padding-right: 4px;
}
</style>
