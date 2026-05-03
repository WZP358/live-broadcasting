import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'

const server = await createServer({
  configFile: false,
  root: process.cwd(),
  plugins: [
    vue(),
    Components({
      resolvers: [AntDesignVueResolver({ importStyle: false })],
    }),
  ],
  resolve: {
    alias: [{ find: '@', replacement: path.resolve('./src') }],
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '@use "@/styles/index.scss" as *; @use "@/styles/color.scss" as *;',
      },
    },
  },
  server: {
    host: 'localhost',
    port: 5174,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:9000',
        changeOrigin: true,
        rewrite: p => p.replace(/^\/api/, ''),
      },
    },
  },
  optimizeDeps: {
    noDiscovery: true,
    include: [],
  },
})

await server.listen()
server.printUrls()
await new Promise(() => {})
