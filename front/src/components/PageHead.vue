<!-- ============================================================
     PageHead.vue —— 页面标题 + 说明。
     标题后可选一个「?」图标，悬浮展示补充说明（tip / tip 插槽）。
     ============================================================ -->
<template>
  <div class="page-head">
    <div>
      <h2 class="page-title">
        {{ title }}
        <el-tooltip v-if="tip || $slots.tip" placement="right" effect="light">
          <template #content>
            <div class="page-tip-content"><slot name="tip">{{ tip }}</slot></div>
          </template>
          <el-icon class="page-tip-icon" :size="16"><QuestionFilled /></el-icon>
        </el-tooltip>
      </h2>
      <p v-if="desc || $slots.desc" class="page-desc">
        <slot name="desc">{{ desc }}</slot>
      </p>
    </div>
  </div>
</template>

<script setup>
import { QuestionFilled } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  /** 悬浮提示文案，也可用 #tip 插槽自定义内容 */
  tip: { type: String, default: '' },
})
</script>
