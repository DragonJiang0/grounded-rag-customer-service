<!-- ============================================================
    AgentView.vue —— 自定义 Agent 列表。
    数据来自 GET /agent/list（manage），只含名称 / 描述 / goals / actions，
    供管理台展示与绑定选择；搜索在本地过滤。
    ============================================================ -->
<template>
  <PageHead title="自定义Agent"
            tip="列表来自后端 AgentPlatform 中已注册的 Agent 元数据，仅含名称、描述、目标（goals）与动作（actions）。
名称即 Agent 的唯一标识，可用于应用绑定。" />

  <div v-loading="loading" class="card">
    <PanelTitle :icon="MagicStick" title="Agent 列表">
      <template #extra>共 {{ total }} 个</template>
    </PanelTitle>

    <div v-if="agents.length" class="agent-grid">
      <div v-for="a in agents" :key="a.name" class="agent-card">
        <div class="agent-head">
          <div class="agent-avatar">{{ (a.name || '?').slice(0, 1) }}</div>
          <div class="agent-title-box">
            <div class="agent-name" :title="a.name">{{ a.name }}</div>
            <div class="agent-desc" :title="a.description">{{ a.description || '暂无描述' }}</div>
          </div>
        </div>

        <div class="agent-body">
          <div class="agent-section">
            <div class="agent-label">
              目标 Goals
              <span class="agent-count">{{ a.goals.length }}</span>
            </div>
            <div v-if="a.goals.length" class="chip-row">
              <!-- 只展示放得下的若干项，其余收进「+N」悬浮查看 -->
              <el-tooltip v-for="g in visibleChips(a.goals)" :key="g.name"
                          placement="top" effect="light" :show-after="200">
                <template #content>
                  <div class="chip-tip"><b>{{ g.name }}</b>{{ g.description || '暂无描述' }}</div>
                </template>
                <el-tag type="primary" effect="light" class="chip">{{ g.name }}</el-tag>
              </el-tooltip>
              <el-tooltip v-if="a.goals.length > CHIP_LIMIT"
                          placement="top" effect="light" :show-after="200">
                <template #content>
                  <div class="chip-tip">
                    <div v-for="g in restChips(a.goals)" :key="g.name">
                      <b>{{ g.name }}</b>{{ g.description || '暂无描述' }}
                    </div>
                  </div>
                </template>
                <el-tag type="primary" effect="plain" class="chip chip--more">
                  +{{ a.goals.length - CHIP_LIMIT }}
                </el-tag>
              </el-tooltip>
            </div>
            <div v-else class="is-off">未定义</div>
          </div>

          <div class="agent-section">
            <div class="agent-label">
              动作 Actions
              <span class="agent-count">{{ a.actions.length }}</span>
            </div>
            <div v-if="a.actions.length" class="chip-row">
              <el-tooltip v-for="x in visibleChips(a.actions)" :key="x.name"
                          placement="top" effect="light" :show-after="200">
                <template #content>
                  <div class="chip-tip"><b>{{ x.name }}</b>{{ x.description || '暂无描述' }}</div>
                </template>
                <el-tag type="info" effect="plain" class="chip">{{ x.name }}</el-tag>
              </el-tooltip>
              <el-tooltip v-if="a.actions.length > CHIP_LIMIT"
                          placement="top" effect="light" :show-after="200">
                <template #content>
                  <div class="chip-tip">
                    <div v-for="x in restChips(a.actions)" :key="x.name">
                      <b>{{ x.name }}</b>{{ x.description || '暂无描述' }}
                    </div>
                  </div>
                </template>
                <el-tag type="info" effect="plain" class="chip chip--more">
                  +{{ a.actions.length - CHIP_LIMIT }}
                </el-tag>
              </el-tooltip>
            </div>
            <div v-else class="is-off">未定义</div>
          </div>
        </div>

        <div class="agent-foot">
          <el-button plain size="large" @click="openDetail(a)">
            <el-icon class="btn-icon"><Document /></el-icon>详情
          </el-button>
          <el-button plain size="large" @click="goDebug(a)">
            <el-icon class="btn-icon"><Promotion /></el-icon>调试
          </el-button>
        </div>
      </div>
    </div>

    <EmptyHint v-else-if="!loading" :icon="MagicStick" text="暂无已注册的 Agent" />
  </div>

  <!-- 详情：goals / actions 的完整描述 -->
  <el-dialog :model-value="!!detailAgent" :title="detailAgent?.name || 'Agent 详情'" width="560px"
             @update:model-value="detailAgent = null">
    <template v-if="detailAgent">
      <p class="detail-desc">{{ detailAgent.description || '暂无描述' }}</p>

      <div class="detail-block">
        <div class="detail-title">目标 Goals（{{ detailAgent.goals.length }}）</div>
        <div v-if="detailAgent.goals.length" class="detail-list">
          <div v-for="g in detailAgent.goals" :key="g.name" class="detail-item">
            <div class="detail-item-name">{{ g.name }}</div>
            <div class="detail-item-desc">{{ g.description || '暂无描述' }}</div>
          </div>
        </div>
        <div v-else class="is-off">未定义</div>
      </div>

      <div class="detail-block">
        <div class="detail-title">动作 Actions（{{ detailAgent.actions.length }}）</div>
        <div v-if="detailAgent.actions.length" class="detail-list">
          <div v-for="x in detailAgent.actions" :key="x.name" class="detail-item">
            <div class="detail-item-name">{{ x.name }}</div>
            <div class="detail-item-desc">{{ x.description || '暂无描述' }}</div>
          </div>
        </div>
        <div v-else class="is-off">未定义</div>
      </div>
    </template>

    <template #footer>
      <el-button size="large" @click="detailAgent = null">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Document, Promotion } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import EmptyHint from '@/components/EmptyHint.vue'
import { useAgentPlatform } from '@/stores/useAgentPlatform'

const { agents, total, loading, load } = useAgentPlatform()
const router = useRouter()

const detailAgent = ref(null)

/** 卡片上每类最多直接展示的标签数，超出部分收进「+N」悬浮查看 */
const CHIP_LIMIT = 4

/** 卡片上直接展示的标签 */
function visibleChips(list) {
  return (list || []).slice(0, CHIP_LIMIT)
}

/** 超出展示上限、只在悬浮里出现的标签 */
function restChips(list) {
  return (list || []).slice(CHIP_LIMIT)
}

function openDetail(a) {
  detailAgent.value = a
}

/** 跳转到该 Agent 的调试页，名称通过 query 传递 */
function goDebug(a) {
  router.push({ path: '/agent/debug', query: { name: a.name } })
}

onMounted(load)
</script>

<style scoped>
.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.agent-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--brand-soft-2);
  transition: border-color .15s, background .15s, box-shadow .15s;
}
.agent-card:hover {
  border-color: #BCD6F7;
  background: #F0F7FF;
  box-shadow: var(--shadow);
}

.agent-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.agent-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-size: 16px;
  font-weight: 700;
}
.agent-title-box {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.agent-name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agent-desc {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-body {
  flex: 1;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.agent-section {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.agent-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--muted);
}
.agent-count {
  padding: 0 6px;
  border-radius: 20px;
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-size: 11.5px;
  font-weight: 700;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  /* 名称过长时省略，完整名称与描述在悬浮提示里看 */
  max-width: 100%;
  overflow: hidden;
}
.chip :deep(.el-tag__content) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chip--more {
  cursor: default;
}

/* 悬浮提示内容：名称加粗，描述换行缩略 */
.chip-tip {
  max-width: 240px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #3D4B60;
  white-space: pre-line;
}
.chip-tip b {
  display: block;
  color: var(--text);
}
.chip-tip > div + div {
  margin-top: 6px;
}
.is-off {
  color: var(--muted);
  font-size: 12.5px;
  font-weight: 600;
}

.agent-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  display: flex;
  gap: 8px;
}
.agent-foot .el-button {
  flex: 1;
}

/* ---------- 详情弹窗 ---------- */
.detail-desc {
  margin: 0 0 16px;
  font-size: 13.5px;
  line-height: 1.72;
  color: #4B5A70;
}
.detail-block + .detail-block {
  margin-top: 16px;
}
.detail-title {
  font-size: 13px;
  font-weight: 700;
  color: #3D4B60;
  margin-bottom: 8px;
}
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-item {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--brand-soft-2);
}
.detail-item-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text);
}
.detail-item-desc {
  margin-top: 4px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #4B5A70;
  white-space: pre-line;
}
</style>
