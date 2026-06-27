import { defineConfig, loadEnv } from 'vite'
import Components from 'unplugin-vue-components/vite';
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers';
import vue from '@vitejs/plugin-vue'
import * as path from 'path';

// https://vite.dev/config/
export default defineConfig((mode) => {
  const env = loadEnv(mode, process.cwd());
  return {
    plugins: [vue(),
    Components({
      resolvers: [
        AntDesignVueResolver({
          importStyle: false, // css in js
        }),
      ],
    }),
    ],
    resolve: {
      alias: [
        // @代替src
        {
          find: '@',
          replacement: path.resolve('./src')
        }
      ]
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @use "@/styles/color.scss" as *;
          `
        }
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT),
      // 运行时自动打开浏览器
      // open: true,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: env.VITE_BACKEND_URL || 'http://localhost:8088',
          changeOrigin: true
        },
        '/uploads': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8088',
          changeOrigin: true
        },
        '/live.file.bucket': {
          target: env.VITE_MINIO_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/ws-netty': {
          target: 'ws://localhost:10022',
          ws: true,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ws-netty/, '/')
        },
        '/ws/browser-live': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8088',
          ws: true,
          changeOrigin: true
        },
        '/live-stream': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/live-stream/, '')
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('ant-design-vue')) {
              return 'vendor-antdv'
            }
            if (id.includes('@ant-design/icons-vue')) {
              return 'vendor-icons'
            }
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vendor-vue'
            }
            if (id.includes('hls.js') || id.includes('flv.js')) {
              return 'vendor-player'
            }
            if (id.includes('svgaplayerweb')) {
              return 'vendor-gift'
            }
            return 'vendor'
          }
        }
      }
    }
  }
})
