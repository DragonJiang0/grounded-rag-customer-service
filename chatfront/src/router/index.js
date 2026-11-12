import { createRouter, createWebHistory } from 'vue-router'
import ChatPage from '../views/ChatPage.vue'
import LoginPage from '../views/LoginPage.vue'
import { isLogined } from '../utils/auth'

const routes = [
  // 根路径重定向到免密聊天（保留 accesskey 场景）
  { path: '/', redirect: '/chat' },
  {
    path: '/chat/:accesskey',
    name: 'chat',
    component: ChatPage,
    props: (route) => ({ mode: 'anonymous', accesskey: route.params.accesskey }),
  },
  // 兼容 /chat 无 accesskey 的情况，交给组件判定 404
  {
    path: '/chat',
    name: 'chat-empty',
    component: ChatPage,
    props: { mode: 'anonymous', accesskey: '' },
  },
  {
    path: '/login/:appkey',
    name: 'login',
    component: LoginPage,
    props: (route) => ({ appkey: route.params.appkey }),
  },
  // 无 appkey 访问 /login：交给组件展示 404
  {
    path: '/login',
    name: 'login-empty',
    component: LoginPage,
    props: { appkey: '' },
  },
  {
    path: '/loginedchat',
    name: 'loginedchat',
    component: ChatPage,
    props: { mode: 'logined' },
    // 未登录访问时展示 404（由组件内 notFound 逻辑控制）
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  // 未登录访问需鉴权页面：放行，由页面自身展示 404（不跳转登录页）
  if (to.meta.requiresAuth && !isLogined()) {
    return true
  }
  return true
})

export default router
