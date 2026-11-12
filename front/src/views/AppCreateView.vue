<!-- ============================================================
    AppCreateView.vue —— 创建应用。
    只有「创建应用」一个表单：名称 / 描述 / 语料分组 / 多Agent。
    渠道等配置在创建完成后，到编辑页的「发布渠道」里维护。
    ============================================================ -->
<template>
  <div class="card create-card">
    <div class="create-title">创建应用</div>

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

    <div class="form-foot">
      <el-button size="large" @click="goBack">取消</el-button>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">
        <el-icon class="btn-icon"><Check /></el-icon>创建
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, QuestionFilled, Search } from '@element-plus/icons-vue'
import AppGroupPicker from './AppGroupPicker.vue'
import AppAgentPicker from './AppAgentPicker.vue'
import { appApi } from '@/api/app'
import { errorMessage } from '@/api/http'
import { useGroups } from '@/stores/useGroups'
import { useAgentPlatform } from '@/stores/useAgentPlatform'

const router = useRouter()
const { allGroups, loading: groupsLoading, loadAll: loadAllGroups } = useGroups()
// 与「自定义Agent」页共用同一份已注册 Agent 列表
const { agents, loading: agentsLoading, load: loadAgents } = useAgentPlatform()

const form = reactive({
  name: '',
  description: '',
  corpusGroupIds: [],
  /** 是否启用多 Agent：不选按关闭处理，与后端 AddAppDto 默认一致 */
  multiAgentEnabled: false,
  /** 参与选择的 Agent 名称数组，对应接口的 agents 字段 */
  agentNames: [],
})

/** 分组搜索关键词，输入框在字段标签行上，由父级持有 */
const groupKeyword = ref('')
/** Agent 搜索关键词，同上 */
const agentKeyword = ref('')

const submitting = ref(false)

async function submit() {
  if (!form.name.trim()) {
    return ElMessage.warning('请输入应用名称')
  }
  if (!form.corpusGroupIds.length) {
    return ElMessage.warning('请至少选择一个语料分组')
  }
  if (form.multiAgentEnabled && !form.agentNames.length) {
    return ElMessage.warning('启用多 Agent 后请至少选择一个 Agent')
  }

  submitting.value = true
  const name = form.name.trim()
  try {
    // /apps/add 成功返回空响应体，不做任何解析
    await appApi.create({
      name,
      description: form.description.trim(),
      corpusGroupIds: [...form.corpusGroupIds],
      multiAgentEnabled: form.multiAgentEnabled,
      agents: [...form.agentNames],
    })
    ElMessage.success('应用已创建')
    // 创建接口不返回 id，重新拉列表按名称找出新应用，直接进入编辑页
    const created = await findAppByName(name)
    if (created?.id) {
      router.replace(`/app/edit/${created.id}`)
    } else {
      router.replace('/app')
    }
  } catch (e) {
    // 400 校验失败 / 500 业务失败（名称重复等）统一取 detail 展示
    ElMessage.error(errorMessage(e, '创建应用失败'))
  } finally {
    submitting.value = false
  }
}

/** 创建后按名称从列表里找新应用（名称全局唯一） */
async function findAppByName(name) {
  try {
    const list = (await appApi.listAll()) || []
    // 名称唯一，取最后一个匹配项以防历史同名数据
    return [...list].reverse().find(a => a.name === name)
  } catch {
    // 列表拉取失败不阻断，退回列表页
    return null
  }
}

function goBack() {
  router.push('/app')
}

// 分组来自 /groups/all，与「语料分组」页共用同一份 store 数据
onMounted(() => {
  loadAllGroups()
  loadAgents()
})
</script>

<style scoped>
.create-card {
  width: 100%;
}

/* 创建页只有这一块表单，标题就放在卡片内 */
.create-title {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
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

/* 底部操作区 */
.form-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
</style>
