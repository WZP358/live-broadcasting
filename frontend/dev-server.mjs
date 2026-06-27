import { createServer, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const env = loadEnv(process.env.NODE_ENV || 'dev', __dirname, '')
const backendUrl = env.VITE_BACKEND_URL || process.env.VITE_BACKEND_URL || 'http://localhost:8088'
const minioUrl = env.VITE_MINIO_URL || process.env.VITE_MINIO_URL || 'http://localhost:9000'
const websocketUrl = env.VITE_WEBSOCKET_URL || process.env.VITE_WEBSOCKET_URL || 'ws://localhost:10022'
const liveStreamUrl = env.VITE_LIVE_STREAM_URL || process.env.VITE_LIVE_STREAM_URL || 'http://localhost:8080'

const server = await createServer({
  configFile: false,
  root: __dirname,
  plugins: [
    vue(),
    Components({
      resolvers: [AntDesignVueResolver({ importStyle: false })],
    }),
  ],
  resolve: {
    alias: [{ find: '@', replacement: path.resolve(__dirname, 'src') }],
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: (source, filePath) => {
          if (filePath.endsWith('color.scss')) return source
          return '@use "@/styles/color.scss" as *;' + '\n' + source
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: true,
      },
      '/uploads': {
        target: backendUrl,
        changeOrigin: true,
      },
      '/live.file.bucket': {
        target: minioUrl,
        changeOrigin: true,
      },
      '/ws-netty': {
        target: websocketUrl,
        ws: true,
        changeOrigin: true,
        rewrite: p => p.replace(/^\/ws-netty/, '/'),
      },
      '/ws/browser-live': {
        target: backendUrl,
        ws: true,
        changeOrigin: true,
      },
      '/live-stream': {
        target: liveStreamUrl,
        changeOrigin: true,
        rewrite: p => p.replace(/^\/live-stream/, ''),
      },
    },
  },
  optimizeDeps: {
    noDiscovery: true,
    include: ['dayjs', 'dayjs/plugin/advancedFormat', 'dayjs/plugin/customParseFormat', 'dayjs/plugin/weekday', 'dayjs/plugin/localeData', 'dayjs/plugin/weekOfYear', 'dayjs/plugin/weekYear', 'dayjs/plugin/quarterOfYear'],
  },
})

await server.listen()
server.printUrls()
await new Promise(() => {})
