<template>
  <div class="empty-hint">
    <el-icon :size="34"><component :is="icon" /></el-icon>
    <div>{{ text }}</div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: [Object, Function], default: null },
  text: { type: String, default: '' },
})
</script>
