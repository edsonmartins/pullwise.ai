import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/webhooks': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // React core
          'react-core': ['react', 'react-dom'],
          // Mantine UI
          'mantine': [
            '@mantine/core',
            '@mantine/hooks',
            '@mantine/notifications',
            '@mantine/modals',
          ],
          // Routing
          'routing': ['react-router-dom', 'react-router'],
          // Queries
          'queries': ['@tanstack/react-query'],
          // Charts
          'charts': ['recharts', 'reactflow'],
          // Icons
          'icons': ['@tabler/icons-react'],
          // Editor
          'editor': ['@monaco-editor/react'],
          // WebSocket
          'socket': ['socket.io-client'],
          // Diff viewer
          'diff': ['react-diff-viewer-continued'],
        },
      },
    },
    chunkSizeWarningLimit: 600,
  },
})
