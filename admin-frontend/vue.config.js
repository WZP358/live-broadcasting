'use strict'
const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = process.env.VUE_APP_TITLE || 'PulseLive'
const baseUrl = process.env.VUE_APP_BACKEND_URL || 'http://localhost:9000'
const port = process.env.port || process.env.npm_config_port || 5180

module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? '/' : '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,
  parallel: false,
  transpileDependencies: ['quill'],
  devServer: {
    host: '0.0.0.0',
    port,
    open: true,
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: baseUrl,
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      },
      '^/v3/api-docs/(.*)': {
        target: baseUrl,
        changeOrigin: true
      }
    },
    disableHostCheck: true
  },
  css: {
    loaderOptions: {
      sass: {
        sassOptions: { outputStyle: 'expanded' }
      }
    }
  },
  configureWebpack: {
    name,
    resolve: {
      alias: {
        '@': resolve('src')
      }
    },
    plugins: []
  },
  chainWebpack(config) {
    config.plugins.delete('preload')
    config.plugins.delete('prefetch')

    config.module
      .rule('svg')
      .exclude.add(resolve('src/assets/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/assets/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()
  }
}
