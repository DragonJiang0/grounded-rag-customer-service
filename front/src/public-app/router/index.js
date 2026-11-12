/* ============================================================
   public-app/router/index.js —— 独立子应用自己的路由表。
   与主应用路由（/corpus、/app …）互不重叠，且无任何鉴权守卫：
   未登录即可直接访问，刷新、直链均不受影响。

   采用 hash 模式（createWebHashHistory）：URL 形如
     /public-app.html#/
     /public-app.html#/demo
   这样刷新与深链不需要任何服务器 fallback 配置，
   直接访问 /public-app.html 也会命中默认路由，不会白屏。
   ============================================================ */

import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  { path: '/',     name: 'public-home', component: HomeView },
  { path: '/demo', name: 'public-demo', component: HomeView },
  // 兜底：任何未匹配的 hash 路径都回到首页，避免白屏
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 注意：这里不注册任何 beforeEach 鉴权守卫，子应用整体公开
export default router
