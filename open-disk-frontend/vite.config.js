import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  define: {
    'global': 'window',  // 将 global 定义为 window
  },
  plugins: [
    vue(),
    Components({
      resolvers: [
        AntDesignVueResolver({
          importStyle: false, // 使用 css in js 方式加载样式
        }),
      ],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // 当请求以 /api 开头时，将其代理到后端服务
      '/api/v1': {
        target: 'http://localhost:8080', // 后端地址
        changeOrigin: true,              // 修改请求头中的 Origin 字段
        // rewrite: (path) => path.replace(/^\/api/, ''), // 重写路径，去除 /api 前缀（根据后端要求调整）
      },
    },
  }
})
