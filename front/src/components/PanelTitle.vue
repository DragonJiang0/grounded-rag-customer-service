<!-- ============================================================
     PanelTitle.vue —— 卡片内的小标题：图标 + 标题 +（可选）右侧统计。
     ============================================================ -->
<template>
  <div class="panel-title">
    <el-icon :size="18"><component :is="icon" /></el-icon>
    <b>{{ title }}</b>
    <span v-if="$slots.extra" class="muted-count"><slot name="extra" /></span>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: [Object, Function], default: null },
  title: { type: String, default: '' },
})
</script>
