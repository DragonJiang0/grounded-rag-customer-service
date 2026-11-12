<!-- ============================================================
    AgentDebugView.vue —— Agent 调试页（独立页面，不与应用调试页共用布局）。
    从「自定义Agent」列表的「调试」按钮进入，Agent 名称经 query 传递。
    页面本身只负责：标题条 + 挂载 AgentDebugPanel。
    ============================================================ -->
<template>
  <div class="agent-debug-page">
    <!-- 顶部：标题 -->
    <div class="page-bar">
      <div class="page-bar__main">
        <el-icon class="page-bar__icon" :size="18"><Promotion /></el-icon>
        <span class="page-bar__title">Agent 调试</span>
      </div>

      <el-tooltip placement="top" effect="light">
        <template #content>
          <div class="tip-body">
            调试用当前 Agent 的提示词与问答流程，不发布、不影响线上渠道。
          </div>
        </template>
        <el-icon class="tip-icon"><QuestionFilled /></el-icon>
      </el-tooltip>
    </div>

    <div class="card">
      <AgentDebugPanel :agent-name="agentName" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Promotion, QuestionFilled } from '@element-plus/icons-vue'
import AgentDebugPanel from './AgentDebugPanel.vue'

const route = useRoute()

/** 调试目标 Agent 名称：Agent 的唯一标识 */
const agentName = computed(() => String(route.query.name || ''))
</script>

<style scoped>
.agent-debug-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 顶部信息条：与卡片同宽，代替通用页头，Agent 调试页专属 */
.page-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: #fff;
}
.page-bar__main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.page-bar__icon {
  color: var(--brand);
  flex: 0 0 auto;
}
.page-bar__title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  flex: 0 0 auto;
}
.tip-icon {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--muted);
  cursor: help;
  transition: color .15s;
}
.tip-icon:hover {
  color: var(--brand);
}
.tip-icon :deep(svg) {
  width: 15px;
  height: 15px;
}
.tip-body {
  max-width: 260px;
  font-size: 13px;
  line-height: 1.7;
  color: #3D4B60;
}
</style>
