import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      input: {
        main: 'index.html',
        adminImport: 'admin-import.html',
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/h5': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/admin/': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
