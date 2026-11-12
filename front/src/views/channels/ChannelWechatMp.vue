<!-- ============================================================
    ChannelWechatMp.vue —— 「微信公众号」渠道的配置区。
    「新增接入」按钮与弹窗由本组件自己维护（各渠道形态不同，不公用）。
    待实现：公众号授权与服务器配置确定后补充。
    ============================================================ -->
<template>
  <div class="channel-add-bar">
    <el-button type="primary" @click="dialogVisible = true" :disabled="disabled">
      <el-icon class="btn-icon"><Plus /></el-icon>新增接入
    </el-button>
  </div>

  <div v-if="records.length" class="record-list">
    <div v-for="(r, i) in records" :key="r.id" class="record-item">
      <div class="record-head">
        <div class="record-index">{{ i + 1 }}</div>
        <div class="record-name">{{ r.name || '未命名公众号' }}</div>
        <el-button text type="danger" size="small" @click="removeAt(i)">移除</el-button>
      </div>
    </div>
  </div>

  <div v-else class="record-empty">
    <div class="empty-text">尚未配置「微信公众号」</div>
    <div class="empty-tip">该渠道仅允许配置一个接入</div>
  </div>

  <!-- 微信公众号专属弹窗：待实现 -->
  <el-dialog v-model="dialogVisible" title="新增微信公众号" width="520px">
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

/** 单实例渠道：已配置一条后不再允许新增 */
const disabled = computed(() => props.channel?.multi === false && records.value.length > 0)

const dialogVisible = ref(false)

/** 待实现说明 */
const NOTE_ITEMS = [
  '公众号名称：展示用的名称',
  'AppID / AppSecret：公众号后台的开发凭证',
  '服务器配置：URL、Token 与 EncodingAESKey',
  '消息类型：文本 / 图片 / 菜单事件的回复策略',
  '授权方式：可改为公众号扫码授权，一键绑定',
]

function removeAt(index) {
  const next = [...records.value]
  next.splice(index, 1)
  emit('update:modelValue', next)
}
</script>
