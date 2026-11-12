import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command }) => ({
  // 生产构建挂在 /chat/ 子路径（对应 deploy/Dockerfile.front 里的 html/chat 与 nginx 的 /chat/）；
  // dev server 仍从 / 起。
  base: command === 'build' ? '/chat/' : '/',
  plugins: [vue()],
  server: {
    proxy: {
      // 聊天接口统一走 /portal 前缀，转发时去掉前缀，后端收到原 /window|/loginedwindow|/open
      // /portal/window/info -> http://localhost:9090/window/info
      '/portal': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/portal/, ''),
      },
    },
  },
}))
