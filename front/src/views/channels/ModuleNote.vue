<!-- ============================================================
    ModuleNote.vue —— 渠道弹窗的「待实现」说明块。
    各渠道弹窗形态待定，先用它列出该模块计划包含的内容，
    后续实现具体表单时逐个替换掉。
    ============================================================ -->
<template>
  <div class="module-note">
    <div class="note-title">该模块待实现，计划包含：</div>
    <ul class="note-list">
      <li v-for="(t, i) in items" :key="i">{{ t }}</li>
    </ul>
  </div>
</template>

<script setup>
defineProps({
  /** 说明条目 */
  items: { type: Array, default: () => [] },
})
</script>
