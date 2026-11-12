/* ============================================================
   router/index.js —— 路由表。path 与 data/menus.js 的 key 一一对应。
   视图采用静态引入（而非懒加载）：让按需引入的 Element Plus 样式
   始终排在 styles/theme.css 之前，保证主题变量覆盖生效。
   ============================================================ */

import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import LoginView from '@/views/LoginView.vue'
import CorpusView from '@/views/CorpusView.vue'
import GroupView from '@/views/GroupView.vue'
import AppView from '@/views/AppView.vue'
import AppCreateView from '@/views/AppCreateView.vue'
import AppEditView from '@/views/AppEditView.vue'
import AgentView from '@/views/AgentView.vue'
import AgentDebugView from '@/views/AgentDebugView.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/corpus' },          // 默认落在「语料管理」
  { path: '/corpus',   name: 'corpus',   component: CorpusView },
  { path: '/group',    name: 'group',    component: GroupView },
  { path: '/app',      name: 'app',      component: AppView },
  { path: '/app/create', name: 'app-create', component: AppCreateView },
  { path: '/app/edit/:id', name: 'app-edit', component: AppEditView },
  { path: '/agent',    name: 'agent',    component: AgentView },
  { path: '/agent/debug', name: 'agent-debug', component: AgentDebugView },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 未登录一律拦到登录页；已登录再访问登录页则回首页
router.beforeEach((to) => {
  const { isLoggedIn } = useAuth()
  if (!to.meta.public && !isLoggedIn.value) {
    return { path: '/login', query: to.fullPath === '/' ? undefined : { redirect: to.fullPath } }
  }
  if (to.path === '/login' && isLoggedIn.value) {
    return { path: '/' }
  }
  return true
})

export default router
