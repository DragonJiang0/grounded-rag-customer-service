<!-- ============================================================
    AppEditView.vue —— 编辑应用（父组件）。
    只负责：顶部两个入口的切换 + 两个保存动作。
    两块内容分别聚合自：
      · AppGroupPicker   语料分组多选
      · AppChannelPanel  原生 / 第三方渠道
    两块各自校验、各自提交，切换不联动。
    ============================================================ -->
<template>
  <div v-loading="loading" class="card edit-card">
    <!-- 顶部一行：两个入口横向排列，同时充当标题 -->
    <div class="edit-head">
      <div v-for="s in SECTIONS" :key="s.key"
           :class="['head-item', { active: activeTab === s.key }]"
           @click="activeTab = s.key">
        <el-icon class="head-icon" :size="18"><component :is="s.icon" /></el-icon>
        <span class="head-title">{{ s.label }}</span>
      </div>
    </div>

    <!-- ---------- 应用配置 ---------- -->
    <div v-show="activeTab === 'config'" class="section-body">
      <el-form label-position="top" size="large" @submit.prevent>
        <el-form-item label="应用名称">
          <el-input v-model="form.name" size="large" class="form-control" clearable
                    maxlength="64" show-word-limit
                    placeholder="如：官网在线客服" />
        </el-form-item>

        <el-form-item label="应用描述">
          <el-input v-model="form.description" type="textarea" :rows="2"
                    maxlength="500" show-word-limit
                    placeholder="用途说明，选填" />
        </el-form-item>

        <el-form-item label="AppKey" >
          <el-input disabled :model-value="form.appKey" size="large" class="form-control" readonly>
            <template #append>
              <el-button @click="copyAppKey">复制</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <template #label>
            <!-- 标签行：左侧字段名 + 问号提示，右侧搜索框，同一行不重叠 -->
            <div class="label-row">
              <span class="label-left">
                <span class="label-required">语料分组</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div class="label-tip-content">
                      必选，可多选；命中时在这些分组内查找语料
                    </div>
                  </template>
                  <el-icon class="label-tip-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>

              <el-input v-model="groupKeyword" size="small" class="group-search"
                        clearable placeholder="搜索分组…">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </template>

          <AppGroupPicker v-model="form.corpusGroupIds" :groups="allGroups"
                          :loading="groupsLoading" :keyword="groupKeyword" />
        </el-form-item>

        <el-form-item>
          <template #label>
            <!-- 多 Agent 开关：放在字段标签行右侧，与语料分组的搜索框同一位置 -->
            <div class="label-row">
              <span class="label-left">
                <span>多Agent</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div class="label-tip-content">
                      启用后按用户问题在下面选中的 Agent 间挑选执行；<br />
                      关闭时固定走默认 Agent
                    </div>
                  </template>
                  <el-icon class="label-tip-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>

              <el-switch v-model="form.multiAgentEnabled" active-text="启用"
                         inactive-text="禁用" class="agent-switch" />
            </div>
          </template>

          <!-- Agent 候选列表：仅在启用时展示，样式与语料分组一致 -->
          <div v-if="form.multiAgentEnabled" class="agent-field">
            <div class="agent-field-head">
              <span class="agent-field-title">参与选择的 Agent</span>
              <el-input v-model="agentKeyword" size="small" class="group-search"
                        clearable placeholder="搜索 Agent…">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <AppAgentPicker v-model="form.agentNames" :agents="agents"
                            :loading="agentsLoading" :keyword="agentKeyword" />
          </div>
          <div v-else class="agent-off-hint">
            当前未启用多 Agent，对话固定由默认 Agent 处理
          </div>
        </el-form-item>
      </el-form>

      <div class="section-foot">
        <el-button size="large" @click="goBack">返回</el-button>
        <el-button type="primary" size="large" :loading="savingConfig" @click="saveConfig">
          <el-icon class="btn-icon"><Check /></el-icon>保存配置
        </el-button>
      </div>
    </div>

    <!-- ---------- 调试 ---------- -->
    <div v-show="activeTab === 'debug'" class="section-body">
      <AppDebugPanel :app-id="appId" :app-name="form.name" :group-ids="form.corpusGroupIds" />
    </div>

    <!-- ---------- 发布渠道 ---------- -->
    <div v-show="activeTab === 'channel'" class="section-body">
      <AppChannelPanel v-model="channelForm" :app-id="appId" :app-key="form.appKey" />
    </div>

    <!-- ---------- 日志 ---------- -->
    <div v-show="activeTab === 'log'" class="section-body">
      <AppChannelLogPanel :app-id="appId" :active="activeTab === 'log'" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, Setting, Promotion, MagicStick, QuestionFilled, Search, Document } from '@element-plus/icons-vue'
import AppGroupPicker from './AppGroupPicker.vue'
import AppAgentPicker from './AppAgentPicker.vue'
import AppChannelPanel from './AppChannelPanel.vue'
import AppChannelLogPanel from './AppChannelLogPanel.vue'
import AppDebugPanel from './AppDebugPanel.vue'
import { appApi } from '@/api/app'
import { errorMessage } from '@/api/http'
import { useGroups } from '@/stores/useGroups'
import { useAgentPlatform } from '@/stores/useAgentPlatform'

const route = useRoute()
const router = useRouter()
const { allGroups, loading: groupsLoading, loadAll: loadAllGroups } = useGroups()
// 与「自定义Agent」页共用同一份已注册 Agent 列表
const { agents, loading: agentsLoading, load: loadAgents } = useAgentPlatform()

const loading = ref(false)
const savingConfig = ref(false)

/** 路由里的应用 ID，新增 API 接入时作为路径参数 */
const appId = computed(() => Number(route.params.id) || null)

/** 分组搜索关键词，输入框在字段标签行上，由父级持有 */
const groupKeyword = ref('')
/** Agent 搜索关键词，同上 */
const agentKeyword = ref('')

/* ---------- 顶部入口，同时也是标题（调试在应用配置左侧） ---------- */
const SECTIONS = [
  { key: 'debug',   label: '调试',     icon: MagicStick },
  { key: 'config',  label: '应用配置', icon: Setting },
  { key: 'channel', label: '发布渠道', icon: Promotion },
  { key: 'log',     label: '日志',     icon: Document },
]

/** 两块内容各自独立，切换不触发校验或保存 */
const activeTab = ref('debug')

/* ---------- 表单 ---------- */
const form = reactive({
  appKey: '',
  name: '',
  description: '',
  corpusGroupIds: [],
  /** 是否启用多 Agent，来自 app.multiAgentEnabled，未配置按关闭处理 */
  multiAgentEnabled: false,
  /** 参与选择的 Agent 名称数组，对应接口的 agents 字段（按绑定顺序） */
  agentNames: [],
})

/** 渠道状态交给 AppChannelPanel 维护 */
const channelForm = reactive({
  channels: [],
})

/* ---------- 加载应用详情 ---------- */
async function load() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const app = await appApi.detail(id)

    Object.assign(form, {
      appKey: app?.appKey || '',
      name: app?.name || '',
      description: app?.description || '',
      // corpusGroups 是对象数组（按绑定顺序），表单里只保留 id
      corpusGroupIds: (app?.corpusGroups || []).map(g => g.id),
      multiAgentEnabled: app?.multiAgentEnabled === true,
      // agents 是 Agent 名称数组（按绑定顺序），直接作为已选列表
      agentNames: Array.isArray(app?.agents) ? [...app.agents] : [],
    })
    // 详情接口不含 publishChannels，渠道状态先置空
    Object.assign(channelForm, { channels: [] })
  } catch (e) {
    // 应用不存在返回 500，原因在 detail 里
    ElMessage.error(errorMessage(e, '加载应用失败'))
    router.push('/app')
  } finally {
    loading.value = false
  }
}

/* ---------- 保存：应用配置 ---------- */
async function saveConfig() {
  if (!form.name.trim()) {
    return ElMessage.warning('请输入应用名称')
  }
  if (!form.corpusGroupIds.length) {
    return ElMessage.warning('请至少选择一个语料分组')
  }
  if (form.multiAgentEnabled && !form.agentNames.length) {
    return ElMessage.warning('启用多 Agent 后请至少选择一个 Agent')
  }

  savingConfig.value = true
  try {
    // /apps/edit 除 id 外全量覆盖，agentNames 必须一并提交，否则 Agent 绑定会被清空
    await appApi.update({
      id: Number(route.params.id),
      name: form.name.trim(),
      description: form.description.trim(),
      corpusGroupIds: [...form.corpusGroupIds],
      multiAgentEnabled: form.multiAgentEnabled,
      agents: [...form.agentNames],
    })
    ElMessage.success('应用配置已保存')
    // 分组绑定是按提交顺序重建的，重新拉一次保证顺序与后端一致
    await load()
  } catch (e) {
    // 400 字段校验带 errors、500 业务失败（名称重复 / 分组不存在）都读 detail
    ElMessage.error(errorMessage(e, '保存应用配置失败'))
  } finally {
    savingConfig.value = false
  }
}

/* 发布渠道接口未就绪，暂不提供保存入口；channelForm 仍持有编辑中的渠道数据 */

async function copyAppKey() {
  try {
    await navigator.clipboard.writeText(form.appKey)
    ElMessage.success('AppKey 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

function goBack() {
  router.push('/app')
}

onMounted(async () => {
  loadAllGroups()
  loadAgents()
  await load()
})
</script>

<style scoped>
/* 卡片占满内容区剩余宽度 */
.edit-card {
  width: 100%;
}

/* ---------- 顶部一行两个入口 ---------- */
.edit-head {
  display: flex;
  align-items: center;
  gap: 34px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 24px;
}
.head-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 12px;
  /* 用下划线指示选中，与标题行同一水平线 */
  margin-bottom: -17px;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  color: var(--muted);
  transition: color .15s, border-color .15s;
}
.head-item:hover {
  color: var(--brand-dark);
}
.head-item.active {
  color: var(--brand-dark);
  border-bottom-color: var(--brand);
}
.head-icon {
  flex: 0 0 auto;
}
.head-title {
  font-size: 16px;
  font-weight: 600;
}
.head-item.active .head-title {
  font-weight: 700;
}

/*
 * 内容区：两个入口共用同一下限高度，切换时不跳动。
 * 用 min-height 而非 height —— 内容更多时允许自然撑开，
 * 避免底部操作区被固定高度截断（按钮看不见）。
 * 注意：不要给子元素加 flex:1，否则表单被拉伸，
 * 多余空间会落到按钮上方形成大片空隙。
 */
.section-body {
  min-height: 570px;
  display: flex;
  flex-direction: column;
}

/* 必填标签旁的补充说明 */
.label-required::after {
  content: '*';
  color: var(--danger);
  margin-left: 4px;
}
/* 字段标签行：左侧名称 + 问号，右侧搜索框，同一行内互不遮挡 */
.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.label-left {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}
.group-search {
  width: 220px;
}
/* 高度提升到默认 small 尺寸（24px）的 1.5 倍 */
.group-search :deep(.el-input__wrapper) {
  height: 36px;
  padding: 0 12px;
}
.group-search :deep(.el-input__inner) {
  height: 36px;
  font-size: 14px;
  line-height: 36px;
}
/* 前缀图标随高度放大并垂直居中 */
.group-search :deep(.el-input__prefix) {
  display: flex;
  align-items: center;
}
.group-search :deep(.el-input__prefix .el-icon) {
  font-size: 16px;
}

/*
 * 必填标签后的「?」提示图标。
 * 不用 vertical-align 对齐（在 label 的行盒里会让命中区错位），
 * 改为固定尺寸 + 自撑内边距，保证悬浮区域稳定可点。
 */
.label-tip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-left: 2px;
  color: var(--muted);
  cursor: help;
  transition: color .15s;
}
.label-tip-icon:hover {
  color: var(--brand);
}
.label-tip-icon :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ---------- 多 Agent ---------- */
/* 开关放在标签行右侧，与语料分组的搜索框同一位置习惯 */
.agent-switch {
  flex: 0 0 auto;
}
/* 启用后的 Agent 选择区：上方一行筛选，下方是与语料分组同款的滚动列表 */
.agent-field {
  width: 100%;
}
.agent-field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}
.agent-field-title {
  font-size: 13px;
  font-weight: 600;
  color: #3D4B60;
}
/* 未启用时的占位说明，高度与选择区留白接近，切换时不跳太多 */
.agent-off-hint {
  width: 100%;
  padding: 14px 18px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
  color: var(--muted);
  font-size: 13px;
}

/* 卡片底部操作区：始终贴内容区底部 */
.section-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  flex: 0 0 auto;
}
</style>
