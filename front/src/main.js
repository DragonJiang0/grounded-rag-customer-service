import { createApp } from 'vue'
import ElementPlus from 'element-plus'
// 中文语言包：日期选择器、分页、弹窗等组件的内置文案统一为中文
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'

// 主题覆盖必须在 Element Plus 样式之后加载：
// 上面两条 import 会先把视图里按需引入的 Element Plus 样式注入，再引入下面的覆盖。
import './styles/theme.css'
import './styles/base.css'

createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
