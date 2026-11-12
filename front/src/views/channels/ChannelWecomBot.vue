<!-- ============================================================
    ChannelWecomBot.vue —— 「企业微信机器人」渠道的配置区。
    「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
    待实现：企业凭证与回调配置确定后补充。
    ============================================================ -->
<template>
  <div class="channel-add-bar">
    <el-button type="primary" @click="dialogVisible = true">
      <el-icon class="btn-icon"><Plus /></el-icon>新增接入
    </el-button>
  </div>

  <div v-if="records.length" class="record-list">
    <div v-for="(r, i) in records" :key="r.id" class="record-item">
      <div class="record-head">
        <div class="record-index">{{ i + 1 }}</div>
        <div class="record-name">{{ r.name || '未命名机器人' }}</div>
        <el-button text type="danger" size="small" @click="removeAt(i)">移除</el-button>
      </div>
    </div>
  </div>

  <div v-else class="record-empty">
    <div class="empty-text">尚未配置「企业微信机器人」</div>
    <div class="empty-tip">可添加多个接入</div>
  </div>

  <!-- 企业微信机器人专属弹窗：待实现 -->
  <el-dialog v-model="dialogVisible" title="新增企业微信机器人" width="520px">
    <ModuleNote :items="NOTE_ITEMS" />
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import ModuleNote from './ModuleNote.vue'

const props = defineProps({
  modelValue: { type: Array, required: true },
  channel: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])

const records = computed(() => props.modelValue)
const dialogVisible = ref(false)

/** 待实现说明 */
const NOTE_ITEMS = [
  '接入名称：便于区分多个机器人',
  '企业 ID（CorpID）：企业微信后台的企业标识',
  '应用 Secret：自建应用的密钥',
  'AgentID：自建应用的 AgentID',
  '回调配置：URL、Token 与 EncodingAESKey 校验',
]

function removeAt(index) {
  const next = [...records.value]
  next.splice(index, 1)
  emit('update:modelValue', next)
}
</script>
