<!-- ============================================================
   HomeView.vue —— 独立子应用的首页（产品落地页）。
   独立于主应用：不依赖任何主应用 store / api / 布局，可未登录直接访问。
   ============================================================ -->
<template>
  <div class="lp">
    <!-- 顶部栏 -->
    <header class="lp-nav">
      <div class="lp-nav-inner">
        <div class="lp-brand">
          <span class="lp-logo"><el-icon :size="20"><Cpu /></el-icon></span>
          <span class="lp-brand-text">智能客服 · 独立门户</span>
        </div>
        <nav class="lp-links">
          <a href="#features">核心能力</a>
          <a href="#demo">在线演示</a>
          <a href="#faq">常见问题</a>
        </nav>
        <el-button type="primary" round @click="toMain">进入控制台</el-button>
      </div>
    </header>

    <!-- Hero -->
    <section class="lp-hero">
      <div class="lp-hero-inner">
        <div class="lp-hero-copy">
          <el-tag effect="plain" round class="lp-badge">Deterministic Intent Matching</el-tag>
          <h1>确定性命中，<br />让每一句提问都有标准答案</h1>
          <p class="lp-sub">
            不是天马行空的生成，而是先用关键词精确命中语料，命中为准则直出标准答案，
            未命中才交给大模型组织语言。可解释、可追溯、可运营。
          </p>
          <div class="lp-hero-actions">
            <el-button type="primary" size="large" @click="toMain">
              <el-icon class="btn-icon"><Promotion /></el-icon>立即体验
            </el-button>
            <el-button size="large" plain @click="scrollTo('features')">了解能力</el-button>
          </div>
          <ul class="lp-stats">
            <li v-for="s in stats" :key="s.label">
              <strong>{{ s.value }}</strong>
              <span>{{ s.label }}</span>
            </li>
          </ul>
        </div>

        <div class="lp-hero-art">
          <div class="lp-chat-card">
            <div class="lp-chat-head">
              <span class="dot" /><span class="dot" /><span class="dot" />
              <span class="lp-chat-title">客服助手</span>
            </div>
            <div class="lp-chat-body">
              <div class="bubble user">如何申请退款？</div>
              <div class="bubble bot">
                命中语料 <el-tag size="small" type="success" effect="light">退货退款</el-tag><br />
                商品签收后 7 天内可在「我的订单」申请退款，审核通过后 1-3 个工作日原路退回。
              </div>
            </div>
            <div class="lp-chat-foot">
              <span class="lp-chat-input">输入问题…</span>
              <el-icon><Promotion /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 核心能力 -->
    <section id="features" class="lp-section">
      <h2 class="lp-section-title">核心能力</h2>
      <p class="lp-section-sub">从语料治理到多渠道发布，一条链路跑通</p>
      <div class="lp-features">
        <div v-for="f in features" :key="f.title" class="lp-feature">
          <span class="lp-feature-icon" :style="{ background: f.bg, color: f.color }">
            <el-icon :size="22"><component :is="f.icon" /></el-icon>
          </span>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 在线演示 -->
    <section id="demo" class="lp-section lp-demo">
      <h2 class="lp-section-title">在线演示</h2>
      <p class="lp-section-sub">试试输入一个常见问题</p>
      <ChatDemo />
    </section>

    <!-- 常见问题 -->
    <section id="faq" class="lp-section">
      <h2 class="lp-section-title">常见问题</h2>
      <el-collapse v-model="activeFaq" class="lp-faq">
        <el-collapse-item v-for="(q, i) in faqs" :key="i" :name="i" :title="q.q">
          <div class="lp-faq-answer">{{ q.a }}</div>
        </el-collapse-item>
      </el-collapse>
    </section>

    <!-- 页脚 -->
    <footer class="lp-footer">
      <span>© {{ year }} 基于AI的确定性客服助手 · 独立门户</span>
      <span class="lp-footer-sep">|</span>
      <a @click="toMain">进入控制台</a>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, Promotion, Collection, MagicStick, Monitor, Connection } from '@element-plus/icons-vue'
import ChatDemo from '../components/ChatDemo.vue'

const router = useRouter()
const year = new Date().getFullYear()
const activeFaq = ref(0)

const stats = [
  { value: '99.9%', label: '命中准确率' },
  { value: '<50ms', label: '平均响应' },
  { value: '多渠道', label: '一键发布' },
]

const features = [
  { icon: Collection, title: '语料管理', desc: '问题与标准答案结构化沉淀，支持分组隔离与批量导入。', bg: '#EAF3FF', color: '#3E8BFF' },
  { icon: MagicStick, title: '确定性命中', desc: '关键词精确匹配优先，命中即为标准答案，杜绝模型编造。', bg: '#EAF7F0', color: '#2FA36B' },
  { icon: Cpu, title: '大模型兜底', desc: '未命中时由大模型基于语料组织语言，兼顾覆盖与自然度。', bg: '#F3EEFF', color: '#7C5CFC' },
  { icon: Monitor, title: '提示词调试', desc: '在线编辑提示词、实时流式预览，所见即所得地调优。', bg: '#FFF4E8', color: '#E8912B' },
  { icon: Connection, title: '渠道发布', desc: 'Web、API、微信等多渠道接入，一套配置全端生效。', bg: '#EAF3FF', color: '#2C74E8' },
  { icon: Collection, title: '对话日志', desc: '完整留痕每次问答，命中与否一目了然，便于持续运营。', bg: '#FFEDED', color: '#E45B5B' },
]

const faqs = [
  { q: '这个独立门户和主控制台是什么关系？', a: '它是完全独立的第二个应用：独立的入口、独立的路由与构建产物，不依赖主控制台的登录态，未登录也能直接访问。' },
  { q: '未登录真的可以直接打开吗？', a: '可以。该门户的路由表没有注册任何鉴权守卫，刷新与直接访问均放行；主控制台仍保持原有登录校验。' },
  { q: '演示区会真的调用后端吗？', a: '演示区为纯前端模拟，用于展示交互样式，不产生任何后端请求，可放心体验。' },
]

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}
function toMain() {
  // 跳回主应用：独立门户与主应用共用同一域名，直接切到根路径即可
  window.location.href = '/'
}
</script>

<style scoped>
.lp {
  min-height: 100vh;
  background: #fff;
}

/* 顶部栏 */
.lp-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, .86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #E8EFF8;
}
.lp-nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  height: 62px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.lp-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15.5px;
  color: #1F2A3A;
}
.lp-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6BA6FF, #3E8BFF);
  box-shadow: 0 3px 10px rgba(62, 139, 255, .32);
}
.lp-links {
  display: flex;
  gap: 22px;
  margin-left: auto;
}
.lp-links a {
  color: #5A6B84;
  font-size: 14px;
  text-decoration: none;
  transition: color .15s;
}
.lp-links a:hover {
  color: #3E8BFF;
}

/* Hero */
.lp-hero {
  background: linear-gradient(135deg, #EAF3FF 0%, #F5F9FF 45%, #DCEAFF 100%);
  padding: 72px 24px 84px;
}
.lp-hero-inner {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.05fr .95fr;
  gap: 48px;
  align-items: center;
}
.lp-badge {
  margin-bottom: 18px;
}
.lp-hero-copy h1 {
  font-size: 38px;
  line-height: 1.28;
  margin: 0 0 18px;
  color: #16233A;
  letter-spacing: .5px;
}
.lp-sub {
  color: #5A6B84;
  font-size: 15px;
  line-height: 1.85;
  margin: 0 0 26px;
}
.lp-hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
}
.lp-stats {
  display: flex;
  gap: 34px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.lp-stats li {
  display: flex;
  flex-direction: column;
}
.lp-stats strong {
  font-size: 22px;
  color: #2C74E8;
}
.lp-stats span {
  color: #7E8FA8;
  font-size: 12.5px;
  margin-top: 2px;
}

/* Hero 右侧聊天卡 */
.lp-hero-art {
  display: flex;
  justify-content: center;
}
.lp-chat-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border: 1px solid #E2ECF8;
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(46, 102, 180, .14);
  overflow: hidden;
}
.lp-chat-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  border-bottom: 1px solid #EEF4FB;
}
.lp-chat-head .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #D8E4F2;
}
.lp-chat-title {
  margin-left: 8px;
  font-size: 13px;
  color: #7E8FA8;
}
.lp-chat-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 190px;
}
.bubble {
  max-width: 84%;
  padding: 10px 13px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
}
.bubble.user {
  align-self: flex-end;
  background: #3E8BFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble.bot {
  align-self: flex-start;
  background: #F2F7FE;
  color: #33445C;
  border-bottom-left-radius: 4px;
}
.lp-chat-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #EEF4FB;
  color: #A9B7CA;
  font-size: 13px;
}

/* 通用区块 */
.lp-section {
  max-width: 1120px;
  margin: 0 auto;
  padding: 72px 24px;
}
.lp-section-title {
  text-align: center;
  font-size: 26px;
  color: #16233A;
  margin: 0 0 8px;
}
.lp-section-sub {
  text-align: center;
  color: #7E8FA8;
  font-size: 14px;
  margin: 0 0 40px;
}

/* 能力卡片 */
.lp-features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.lp-feature {
  border: 1px solid #E8EFF8;
  border-radius: 14px;
  padding: 24px 22px;
  transition: transform .18s, box-shadow .18s, border-color .18s;
}
.lp-feature:hover {
  transform: translateY(-3px);
  border-color: #C6DCFF;
  box-shadow: 0 12px 30px rgba(46, 102, 180, .1);
}
.lp-feature-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.lp-feature h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #16233A;
}
.lp-feature p {
  margin: 0;
  color: #7E8FA8;
  font-size: 13.5px;
  line-height: 1.75;
}

/* 演示 */
.lp-demo :deep(.demo-view) {
  margin: 0 auto;
}

/* FAQ */
.lp-faq {
  border: none;
}
.lp-faq :deep(.el-collapse-item__header) {
  font-size: 15px;
  font-weight: 600;
  color: #16233A;
}
.lp-faq-answer {
  color: #5A6B84;
  font-size: 14px;
  line-height: 1.85;
}

/* 页脚 */
.lp-footer {
  border-top: 1px solid #E8EFF8;
  padding: 26px 24px;
  text-align: center;
  color: #93A2B8;
  font-size: 13px;
}
.lp-footer a {
  color: #3E8BFF;
  cursor: pointer;
}
.lp-footer-sep {
  margin: 0 10px;
}

@media (max-width: 900px) {
  .lp-hero-inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .lp-features {
    grid-template-columns: 1fr;
  }
  .lp-links {
    display: none;
  }
  .lp-hero-copy h1 {
    font-size: 30px;
  }
}
</style>
