/* ============================================================
   public-app/main.js —— 独立子应用的入口，与主应用完全隔离。
   不引入主应用的 router / store / 布局，挂载点也是独立的 #public-app。
   ============================================================ */

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import PublicApp from './PublicApp.vue'
import router from './router'

// 独立样式：只加载子应用自己的样式，不耦合主应用的 theme.css / base.css
import './styles.css'

createApp(PublicApp).use(router).use(ElementPlus, { locale: zhCn }).mount('#public-app')
