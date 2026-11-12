import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ command }) => ({
  // 生产构建挂在 /apps/ 子路径（对应 deploy/Dockerfile.front 里的 html/apps 与 nginx 的 /apps/）；
  // dev server 仍从 / 起，本地 http://localhost:5180/ 访问方式不变。
  base: command === 'build' ? '/apps/' : '/',
  plugins: [
    vue(),
    // Element Plus 按需引入：组件与 ElMessage / ElMessageBox 等 API 及其样式自动注入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5180,
    strictPort: true,
    // 前端统一用同源 /manage 请求，由这里转发到后端，浏览器侧不涉及跨域
    // 转发时移除 /manage 前缀：/manage/user/login -> http://localhost:8080/user/login
    proxy: {
      '/manage': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/manage/, ''),
      },
    },
  },
}))
