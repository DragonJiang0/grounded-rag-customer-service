import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 全局样式重置：消除 body 默认 margin，保证整页布局贴顶
import './styles/global.css'
import App from './App.vue'
import router from './router'
import { clearToken, getToken } from './utils/auth'
import { setAccesskey, setUnauthorizedHandler } from './utils/request'

// 启动时恢复登录态：localStorage 有 token 则注入请求头，刷新后保持登录
setAccesskey(getToken())

// chat 侧 401 只清理失效凭证，不跳转：
// chat 与 login 各自处理自己的异常，页面不会被强制踢到登录页。
setUnauthorizedHandler(() => {
  clearToken()
  setAccesskey('')
})

createApp(App).use(ElementPlus).use(router).mount('#app')
