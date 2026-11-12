<!-- ============================================================
   ChatDemo.vue —— 独立门户的纯前端演示对话。
   本地关键词规则模拟「确定性命中」，不产生任何后端请求。
   ============================================================ -->
<template>
  <div class="demo-view">
    <div class="demo-body" ref="bodyRef">
      <div v-for="(m, i) in messages" :key="i" :class="['demo-bubble', m.role]">
        <div class="demo-text">{{ m.text }}</div>
        <div v-if="m.hit" class="demo-hit">
          <el-icon><Aim /></el-icon>命中语料：<b>{{ m.hit }}</b>
        </div>
      </div>
    </div>

    <div class="demo-suggest">
      <span>试试：</span>
      <el-tag v-for="q in suggests" :key="q" effect="plain" round
              class="demo-chip" @click="ask(q)">{{ q }}</el-tag>
    </div>

    <div class="demo-input">
      <el-input v-model="input" placeholder="输入一个客服问题…" clearable
                @keyup.enter="ask()" />
      <el-button type="primary" :disabled="!input.trim()" @click="ask()">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { Aim } from '@element-plus/icons-vue'

const input = ref('')
const bodyRef = ref(null)

// 演示用本地语料：关键词 -> 命中标题 + 标准答案
const RULES = [
  { keys: ['退款', '退货', '退'], hit: '退货退款', answer: '商品签收后 7 天内可在「我的订单」申请退款，审核通过后 1-3 个工作日原路退回。' },
  { keys: ['发货', '物流', '快递'], hit: '物流配送', answer: '现货商品 24 小时内发货，默认中通/顺丰，可在订单详情查看实时物流。' },
  { keys: ['发票', '开票'], hit: '发票开具', answer: '支持电子发票，下单时可勾选，也可在订单完成后于「发票中心」申请。' },
  { keys: ['密码', '登录', '账号'], hit: '账号问题', answer: '忘记密码可在登录页点「忘记密码」通过手机号重置；账号被锁请联络客服。' },
]

const messages = ref([
  { role: 'bot', text: '你好，我是智能客服小助手。请输入你的问题，我会尝试精确命中标准答案。' },
])

const suggests = ['如何申请退款？', '多久发货？', '可以开发票吗？']

function match(question) {
  for (const rule of RULES) {
    if (rule.keys.some((k) => question.includes(k))) {
      return rule
    }
  }
  return null
}

async function ask(text) {
  const q = (text ?? input.value).trim()
  if (!q) {
    return
  }
  messages.value.push({ role: 'user', text: q })
  input.value = ''

  const rule = match(q)
  messages.value.push(
    rule
      ? { role: 'bot', text: rule.answer, hit: rule.hit }
      : { role: 'bot', text: '未命中语料，交由大模型基于知识库组织回答（演示区仅展示样式）。' },
  )

  await nextTick()
  const el = bodyRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}
</script>

<style scoped>
.demo-view {
  max-width: 720px;
  border: 1px solid #E8EFF8;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 34px rgba(46, 102, 180, .08);
  background: #fff;
}
.demo-body {
  height: 320px;
  overflow: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #FAFCFF;
}
.demo-bubble {
  display: flex;
  flex-direction: column;
  max-width: 78%;
}
.demo-bubble.user {
  align-self: flex-end;
  align-items: flex-end;
}
.demo-bubble.bot {
  align-self: flex-start;
  align-items: flex-start;
}
.demo-text {
  padding: 10px 13px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.75;
}
.demo-bubble.user .demo-text {
  background: #3E8BFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.demo-bubble.bot .demo-text {
  background: #F2F7FE;
  color: #33445C;
  border-bottom-left-radius: 4px;
}
.demo-hit {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #2FA36B;
  background: #EAF7F0;
  border-radius: 8px;
  padding: 3px 9px;
}
.demo-suggest {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 18px;
  border-top: 1px solid #EEF4FB;
  font-size: 12.5px;
  color: #93A2B8;
}
.demo-chip {
  cursor: pointer;
}
.demo-input {
  display: flex;
  gap: 10px;
  padding: 12px 18px 16px;
}
</style>
