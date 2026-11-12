<!-- ============================================================
     CorpusView.vue —— 语料管理：顶部条件筛选 + 满宽列表（服务端分页），
     新增 / 编辑通过右侧抽屉完成。
     ============================================================ -->
<template>
  <PageHead title="语料管理"
            tip="语料是「确定性命中」的匹配库：用户提问与标准问 / 相似问完全或高度一致时，直接返回对应答案。
标准问：该条语料的核心问法；相似问：同一语义的其它说法（每行一个）；答案：命中后返回的回复内容。
停用后的语料不再参与匹配，但仍保留在列表中。" />

  <div v-loading="loading" class="card">
    <!-- 筛选条件 -->
    <div class="corpus-filter">
      <!-- 分组多时可输入关键字过滤 -->
      <el-select v-model="query.groupId" class="filter-item" size="large" filterable
                 placeholder="所属分组" clearable @change="search">
        <el-option v-for="g in allGroups" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>

      <el-input v-model="query.keyword" class="filter-item" size="large" clearable
                placeholder="问题" @input="onFilterInput" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-input v-model="query.answer" class="filter-item" size="large" clearable
                placeholder="答案" @input="onFilterInput" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-button size="large" @click="resetQuery">重置</el-button>
    </div>

    <!-- 新增按钮（位于搜索条件下方） -->
    <div class="corpus-toolbar">
      <el-button type="primary" size="large" @click="openCreate">
        <el-icon class="btn-icon" :size="18"><Plus /></el-icon>新增语料
      </el-button>
    </div>



    <!-- 列表：第一行答案（单行省略 + 悬浮查看全文），第二行元信息 + 问题标签 -->
    <div v-if="records.length" class="group-list">
      <div v-for="c in records" :key="c.id"
           :class="['group-record', 'corpus-record', { active: c.id === editId, disabled: !c.enabled }]">
        <!-- 第一行：名称（加粗）+ 答案，整行从头显示，放不下省略，悬浮看全文 -->
        <el-tooltip :content="`${c.name || ''}　${c.answer}`.trim()" placement="top"
                    :show-after="300" popper-class="corpus-answer-tip">
          <div class="corpus-answer">
            <span class="corpus-name">{{ c.name }}</span>
            <span class="corpus-answer-text">{{ c.answer }}</span>
          </div>
        </el-tooltip>

        <!-- 第二行：分组 / 状态 / 更新时间 + 问题标签 -->
        <div class="corpus-meta corpus-meta--row">
          <el-tag size="small" type="info" effect="plain">{{ categoryName(c.groupId) }}</el-tag>
          <el-switch :model-value="c.enabled" size="small"
                     @change="() => toggleEnabled(c)"></el-switch>
          <span class="corpus-sub">{{ c.enabled ? '启用' : '停用' }}</span>
          <span class="corpus-sub">·</span>
          <span class="corpus-sub">#{{ c.id }}</span>
          <span class="corpus-sub">·</span>
          <span class="corpus-sub">更新于 {{ c.updatedAt }}</span>

          <!-- 问题清单：按顺序循环取色，用颜色区分多条问法 -->
          <span class="corpus-sub corpus-q-label">问题</span>
          <span v-for="(q, i) in questionsOf(c)" :key="`${c.id}-${i}`"
                :class="['corpus-q-tag', `corpus-q-tag--${i % QUESTION_COLORS}`]"
                :title="q">{{ q }}</span>
          <span v-if="!questionsOf(c).length" class="corpus-sub">（未设置）</span>
        </div>

        <div class="row-actions">
          <el-button plain size="large" @click="openEdit(c)">
            <el-icon class="btn-icon"><Edit /></el-icon>编辑
          </el-button>
          <el-button plain type="danger" size="large" @click="remove(c)">
            <el-icon class="btn-icon"><Delete /></el-icon>删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination v-if="total" class="list-pager" background
                   layout="total, sizes, prev, pager, next"
                   :total="total" :page-size="size" :current-page="current"
                   :page-sizes="[10, 20, 50]"
                   @current-change="onPageChange" @size-change="onSizeChange" />

    <EmptyHint v-if="!records.length && !loading" :icon="Search" text="没有匹配的语料" />
  </div>

  <!-- 新增 / 编辑抽屉 -->
  <el-drawer v-model="drawerVisible" size="960px" :title="editId === null ? '新增语料' : '编辑语料'"
             @closed="resetForm">
    <div class="drawer-body">
      <el-form label-position="top" size="large" @submit.prevent>
        <el-form-item label="所属分组">
          <el-select v-model="form.groupId" size="large" class="form-control"
                     placeholder="请选择分组" clearable>
            <el-option v-for="g in allGroups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="名称">
          <el-input v-model="form.name" size="large" class="form-control" clearable
                    maxlength="64" show-word-limit
                    placeholder="语料名称，如：登录密码修改" />
        </el-form-item>

        <el-form-item label="问题">
          <div class="syn-editor">
            <div v-if="form.synonyms.length" class="syn-tags">
              <el-tag v-for="(s, i) in form.synonyms" :key="`${s}-${i}`" closable
                      disable-transitions @close="removeSynonym(i)">
                {{ s }}
              </el-tag>
            </div>
            <el-input v-model="synInput" size="large" class="form-control" clearable
                      placeholder="输入后按回车添加，如：怎么修改登录密码"
                      @keyup.enter="addSynonym"
                      @blur="addSynonym" />
          </div>
        </el-form-item>

        <el-form-item label="答案">
          <el-input v-model="form.answer" type="textarea" :rows="20"
                    placeholder="命中后返回的标准答案" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button size="large" @click="closeDrawer">取消</el-button>
      <el-button type="primary" size="large" @click="save">
        <el-icon class="btn-icon" :size="18"><component :is="editId === null ? Plus : Check" /></el-icon>
        {{ editId === null ? '新增语料' : '保存修改' }}
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { Plus, Check, Search, Edit, Delete } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import EmptyHint from '@/components/EmptyHint.vue'
import { useCorpus } from '@/stores/useCorpus'
import { useGroups } from '@/stores/useGroups'

const {
  records, total, current, size, loading,
  query, load, search, onFilterInput, resetQuery, onPageChange, onSizeChange,
  editId, drawerVisible, form, openCreate, openEdit, closeDrawer, resetForm, save, remove, toggleEnabled,
  categoryName, setGroupNames, questionsOf,
} = useCorpus()

const { allGroups, loadAll: loadAllGroups } = useGroups()

/* ---------- 标准问标签输入 ---------- */
/** 输入框临时值，回车后转成标签 */
const synInput = ref('')

/** 问题标签的配色循环数：颜色在 CSS 里按 corpus-q-tag--0/1/2 定义 */
const QUESTION_COLORS = 3

function addSynonym() {
  const val = synInput.value.trim()
  if (!val) return
  if (!form.synonyms.includes(val)) {
    form.synonyms.push(val)
  }
  synInput.value = ''
}

function removeSynonym(index) {
  form.synonyms.splice(index, 1)
}

/** 抽屉关闭（表单重置）时清空输入框 */
watch(drawerVisible, v => {
  if (!v) synInput.value = ''
})

// 语料接口只返回 groupId，分组名要靠分组列表映射；分组变化时顺带刷新列表标签
watch(allGroups, list => {
  setGroupNames(list)
  if (records.value.length) load()
})

onMounted(async () => {
  await loadAllGroups()
  setGroupNames(allGroups.value)
  load()
})
</script>

<style scoped>
/* 顶部筛选栏 */
.corpus-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-item {
  width: 220px;
}

/* 搜索条件下方的新增按钮 */
.corpus-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-top: 14px;
  margin-bottom: 6px;
}

/* 抽屉内边距与底部按钮对齐 */
.drawer-body {
  padding: 0 4px;
}

/* 标准问：标签 + 输入 */
.syn-editor {
  width: 100%;
}
.syn-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

/* ---------- 列表行：答案一行 + 元信息/问题标签一行 ---------- */
.corpus-record {
  position: relative;
  display: block;
  /* 右侧给操作区让位，避免答案与标签压到按钮下面 */
  padding-right: 190px;
}

/* 第一行：名称 + 答案，单行从头显示，超出省略（全文由 tooltip 承载） */
.corpus-answer {
  font-size: 14.5px;
  line-height: 1.6;
  color: var(--text);
  /* 单行截断，保证行高一致 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: default;
}
/* 名称加粗，与后面的答案拉开层级 */
.corpus-name {
  font-weight: 700;
  color: var(--text);
}
/* 答案不加粗，比名称浅一档；与名称之间留出间距 */
.corpus-answer-text {
  font-weight: 400;
  color: #4B5A70;
  margin-left: 10px;
}
/* 名称缺失时不留下悬空间距 */
.corpus-name:empty {
  display: none;
}

/* 第二行：元信息与问题标签同一行，超宽换行 */
.corpus-meta--row {
  flex-wrap: wrap;
  row-gap: 6px;
  margin-top: 8px;
}
.corpus-q-label {
  margin-left: 4px;
}
/* 问题标签：按顺序循环取色，文字在框内垂直居中、超长省略 */
.corpus-q-tag {
  display: inline-flex;
  align-items: center;
  box-sizing: border-box;
  max-width: 280px;
  height: 26px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1;
  /* 标签内容超长时省略，而不是撑破行 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: default;
}

/* 三色循环：1 蓝 / 2 绿 / 3 红 */
.corpus-q-tag--0 {
  color: #1E4B85;
  background: #E8F1FF;
  border-color: #BBD4F7;
}
.corpus-q-tag--1 {
  color: #1F6B4A;
  background: #E6F6EE;
  border-color: #B4E0C9;
}
.corpus-q-tag--2 {
  color: #97302B;
  background: #FDEDEC;
  border-color: #F3C4C0;
}

/* 答案悬浮提示：默认给足尺寸，长答案换行展示全文并限制最大高度 */
:global(.el-popper.corpus-answer-tip),
:global(.el-popper.corpus-answer-tip .el-popper__content) {
  /* 默认就是一个大浮层，短答案也保持同样的观感 */
  width: 640px;
  min-height: 120px;
  max-width: 90vw;
  max-height: 60vh;
  box-sizing: border-box;
  padding: 14px 16px;
  /* 仍可滚动，但不显示滚动条 */
  overflow: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  font-size: 13.5px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}
/* WebKit 内核下隐藏滚动条槽位 */
:global(.el-popper.corpus-answer-tip::-webkit-scrollbar),
:global(.el-popper.corpus-answer-tip .el-popper__content::-webkit-scrollbar) {
  width: 0;
  height: 0;
  display: none;
}
/* 操作区固定右上角，行高变化时位置稳定 */
.corpus-record .row-actions {
  position: absolute;
  top: 12px;
  right: 12px;
}
</style>
