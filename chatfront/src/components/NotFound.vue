<script setup>
defineProps({
  code: {
    type: String,
    default: '404',
  },
  text: {
    type: String,
    default: '页面不存在或链接无效',
  },
  desc: {
    type: String,
    default: '请确认访问链接是否完整后再试。',
  },
})
</script>

<template>
  <div class="not-found">
    <div class="not-found-code">{{ code }}</div>
    <div class="not-found-text">{{ text }}</div>
    <div class="not-found-desc">{{ desc }}</div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;
  padding: 24px;
  text-align: center;
}

.not-found-code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  color: #409eff;
}

.not-found-text {
  margin-top: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.not-found-desc {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
</style>
