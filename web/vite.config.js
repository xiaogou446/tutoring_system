import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/h5': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
