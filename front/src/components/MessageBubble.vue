!-- ============================================================
    MessageBubble.vue —— 对话气泡外壳（头像 + 气泡）。
    气泡内容由使用方通过默认插槽提供（对话日志详情等场景使用）。
    头像：传了 botIcon / userIcon 时用图标，否则回退到文字。
    ============================================================ -->
<template>
  <div :class="['msg', role]">
    <div class="msg-avatar">
      <el-icon v-if="role === 'bot' && botIcon" :size="19">
        <component :is="botIcon" />
      </el-icon>
      <el-icon v-else-if="role === 'user' && userIcon" :size="19">
        <component :is="userIcon" />
      </el-icon>
      <template v-else>{{ role === 'user' ? '我' : avatar }}</template>
    </div>
    <div class="bubble"><slot /></div>
  </div>
</template>

<script setup>
defineProps({
  role: { type: String, default: 'bot' },     // 'user' | 'bot'
  avatar: { type: String, default: '' },      // 助手头像文字
  botIcon: { type: [Object, Function], default: null },  // 机器人头像图标
  userIcon: { type: [Object, Function], default: null }, // 用户头像图标
})
</script>

<style scoped>
.msg{display:flex;gap:11px;max-width:86%;}
.msg.user{flex-direction:row-reverse;align-self:flex-end;}
/* 头像：浅色底 + 浅色描边，图标为深色 */
.msg-avatar{width:36px;height:36px;border-radius:var(--radius-sm);flex:0 0 36px;
  display:flex;align-items:center;justify-content:center;font-size:14px;font-weight:600;
  background:#F7F9FC;border:1px solid var(--line);color:#2B323D;}
.msg.bot .msg-avatar{background:var(--brand-soft);border-color:var(--brand-soft-3, #C9DDFF);color:#2B323D;}
.msg.user .msg-avatar{background:#F2F5FA;border-color:#DCE4EE;color:#2B323D;}

.bubble{padding:13px 16px;border-radius:var(--radius-lg);font-size:14px;line-height:1.65;}
.msg.user .bubble{background:linear-gradient(135deg,#6BA6FF,#3E8BFF);color:#fff;
  border-top-right-radius:4px;box-shadow:0 3px 10px rgba(62,139,255,.22);}
.msg.bot .bubble{background:var(--brand-soft-2);border:1px solid var(--line);color:var(--text);
  border-top-left-radius:4px;}

.hit-title{display:flex;align-items:center;gap:7px;font-weight:700;font-size:13.5px;
  margin-bottom:9px;color:var(--ok);}
.hit-title--miss{color:var(--warn);}
.reply-box{background:#fff;border:1px solid var(--line);border-radius:var(--radius);padding:12px 14px;font-size:13.5px;}
</style>
