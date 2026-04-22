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
      host: 'localhost',
      port: Number(env.VITE_APP_PORT),
      // 运行时自动打开浏览器
      // open: true,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: 'http://localhost:9000',
          changeOrigin: true
        }
      }
    }
  }
})
