<!-- ============================================================
    AppChannelPanel.vue —— 发布渠道配置。
    左侧渠道分类树；右侧为公共头部 + 各渠道专属配置组件。
    每个渠道的可配置内容差异较大，因此按渠道拆成独立组件
    （见 ./channels/），此处只负责分类切换与记录数据的分发。
    通过 v-model 绑定 { channels: [{ id, type, name, fields }] }。
    ============================================================ -->
<template>
  <div class="channel-wrap">
    <div class="channel-layout">
      <!-- 左侧：分类 + 渠道清单 -->
      <aside class="channel-aside">
        <div v-for="g in CHANNEL_GROUPS" :key="g.key" class="aside-group">
          <div class="aside-group-title">{{ g.label }}</div>
          <div v-for="c in g.channels" :key="c.key"
               :class="['aside-item', { active: activeKey === c.key }]"
               :title="c.label"
               @click="activeKey = c.key">
            <el-icon class="aside-icon" :size="15">
              <component :is="resolveIcon(c.icon)" />
            </el-icon>
            <span class="aside-label">{{ c.label }}</span>
            <span v-if="countOf(c.key)" class="aside-badge">{{ countOf(c.key) }}</span>
          </div>
        </div>
      </aside>

      <!-- 右侧：公共头部 + 渠道专属配置 -->
      <section class="channel-main">
        <template v-if="activeChannel">
          <!-- 公共头部：所有渠道一致，仅标题与说明 -->
          <div class="main-head">
            <div class="main-title-box">
              <div class="main-title">
                {{ activeChannel.label }}
                <el-tag size="small" effect="plain" type="info">
                  {{ activeGroup?.label }}
                </el-tag>
              </div>
              <div class="main-desc">{{ activeChannel.desc }}</div>
            </div>
          </div>

          <!--
            渠道专属配置：按渠道 key 渲染对应组件。
            「新增接入」按钮与弹窗也由各渠道组件自己实现，
            因为不同渠道的新增流程差异很大（就地填写 / 弹窗 / 扫码授权等）。
            微信个人号的列表直接来自接口，不走 modelValue 绑定。
          -->
          <component :is="activeComponent" v-bind="componentBind"
                     :channel="activeChannel" :app-id="appId" :app-key="appKey"
                     @update:model-value="updateRecords" />
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CHANNEL_GROUPS, findChannel } from '@/data/apps'
import { resolveIcon } from '@/utils/icons'
import ChannelApi from './channels/ChannelApi.vue'
import ChannelFreeLogin from './channels/ChannelFreeLogin.vue'
import ChannelAccountLogin from './channels/ChannelAccountLogin.vue'
import ChannelWechatPersonal from './channels/ChannelWechatPersonal.vue'
// 以下 4 个渠道暂未开放，与 CHANNEL_GROUPS 中的注释保持一致；启用时一并放开
// import ChannelFeishuBot from './channels/ChannelFeishuBot.vue'
// import ChannelDingtalkBot from './channels/ChannelDingtalkBot.vue'
// import ChannelWecomBot from './channels/ChannelWecomBot.vue'
// import ChannelWechatMp from './channels/ChannelWechatMp.vue'
import './channels/channel.css'

const props = defineProps({
  /**
   * 渠道状态：
   *   channels: [{ id, type, name, fields: { [fieldKey]: string } }]
   *   type 与渠道定义里的 key 一致，决定右侧渲染哪个组件
   */
  modelValue: { type: Object, required: true },
  /** 所属应用 ID，透传给渠道组件（API 接入新增需作为路径参数） */
  appId: { type: [Number, String], default: null },
  /** 所属应用的 AppKey，透传给渠道组件（用于拼装登录地址） */
  appKey: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

/** 统一走一次浅拷贝再派发，父组件持有唯一数据源 */
function update(patch) {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

const allRecords = computed(() => props.modelValue.channels || [])

/* ---------- 左侧选择 ---------- */
const activeKey = ref(CHANNEL_GROUPS[0].channels[0].key)

const activeGroup = computed(() =>
  CHANNEL_GROUPS.find(g => g.channels.some(c => c.key === activeKey.value)),
)
const activeChannel = computed(() => findChannel(activeKey.value))

/** 当前渠道下的记录 */
const activeRecords = computed(() =>
  allRecords.value.filter(r => r.type === activeKey.value),
)

function countOf(type) {
  return allRecords.value.filter(r => r.type === type).length
}

/* ---------- 渠道 key -> 配置组件 ---------- */
const COMPONENTS = {
  api: ChannelApi,
  free_login: ChannelFreeLogin,
  account_login: ChannelAccountLogin,
  wechat_personal: ChannelWechatPersonal,
  // 以下 4 个渠道暂未开放，入口已在 CHANNEL_GROUPS 中注释；启用时一并放开
  // feishu_bot: ChannelFeishuBot,
  // dingtalk_bot: ChannelDingtalkBot,
  // wecom_bot: ChannelWecomBot,
  // wechat_mp: ChannelWechatMp,
}

const activeComponent = computed(() => COMPONENTS[activeKey.value] || ChannelApi)

/**
 * 以接口为数据源的渠道不需要本地的 records 绑定，
 * 微信个人号的列表由 /app/setting/botCredentialList/{appId} 提供。
 */
const API_DRIVEN_CHANNELS = new Set(['wechat_personal'])

const componentBind = computed(() =>
  API_DRIVEN_CHANNELS.has(activeKey.value) ? {} : { modelValue: activeRecords.value },
)

/**
 * 子组件只维护「当前渠道」的记录，这里把改动合并回完整列表：
 * 先剔除该渠道的旧记录，再按子组件返回的新顺序插回，
 * 其他渠道的记录保持原位不受影响。
 */
function updateRecords(next) {
  const others = allRecords.value.filter(r => r.type !== activeKey.value)
  update({ channels: [...others, ...next] })
}
</script>

<style scoped>
/*
 * 撑满父级 section-body 可用高度，使本 Tab 与应用配置 Tab 等高。
 * 父级用 min-height（flex 容器），这里 flex:1 吸掉剩余高度。
 */
.channel-wrap {
  display: flex;
  align-self: stretch;
  flex: 1;
  min-height: 0;
}

/* 实际内容：铺满整块高度，左右两栏等高 */
.channel-layout {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
  width: 100%;
  min-height: 0;
}
@media (max-width: 1080px) {
  .channel-layout {
    grid-template-columns: 1fr;
  }
}

/* ---------- 左侧分类 ---------- */
.channel-aside {
  /* 只保留边框线，不加背景色 */
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 10px;
  /* 内容不足时也撑满，与右侧等高 */
  min-height: 0;
  overflow-y: auto;
}
.aside-group + .aside-group {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}
/* 分类标题：明显大于下面的渠道项，作为分组层级 */
.aside-group-title {
  padding: 0 10px 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: .3px;
}
.aside-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  /* 渠道名称字号与列表一致：默认 13.5px 的 120% ≈ 16.2px */
  font-size: 16.2px;
  color: #4B5A70;
  cursor: pointer;
  transition: background .15s, color .15s;
}
.aside-item:hover {
  background: #fff;
  color: var(--brand-dark);
}
.aside-item.active {
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-weight: 700;
}
/* 渠道图标：固定宽度，保证各行的文字左边缘对齐；随文字同步放大 */
.aside-icon {
  flex: 0 0 18px;
  color: #93A7BD;
  transition: color .15s;
}
.aside-item :deep(.aside-icon) {
  font-size: 18px;
}
.aside-item:hover .aside-icon {
  color: var(--brand);
}
.aside-item.active .aside-icon {
  color: var(--brand);
}
.aside-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.aside-badge {
  margin-left: auto;
  flex: 0 0 auto;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

/* ---------- 右侧 ---------- */
.channel-main {
  min-width: 0;
  min-height: 0;
  /* 纵向撑满，与左侧分类等高 */
  display: flex;
  flex-direction: column;
  /* 内容过多时内部滚动，不撑破父级高度 */
  overflow-y: auto;
}
.main-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}
.main-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}
/*
 * 标题右侧的分类标签：默认 small 尺寸高度只有 20px，
 * 文字上下几乎没有留白，显得贴着上边。
 * 这里直接把标签撑高，并用 flex 居中内容，文字上下留白自然变大。
 * 用 height + line-height 同时覆盖，避免被 el-tag 的 size 样式压回去。
 */
.main-title :deep(.el-tag) {
  height: 26px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
}
.main-title :deep(.el-tag .el-tag__content) {
  display: inline-flex;
  align-items: center;
  height: 100%;
  font-size: 12.5px;
  line-height: 1;
}
.main-desc {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--muted);
}
</style>
