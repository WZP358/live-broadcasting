import { createApp } from 'vue'
import { message } from 'ant-design-vue'
import './style.css'
import '@/styles/index.scss'
import '@/styles/admin.scss'
import App from './App.vue'
import { createPinia } from 'pinia'
import routers from '@/router'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import '@/assets/iconfont/iconfont.js'
import vueInfiniteScroll from 'vue-infinite-scroll'


// import '@/styles/index.scss'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
// 组合式api中无法使用$reset
pinia.use(({ store }) => {
    const initialState = JSON.parse(JSON.stringify(store.$state));
    store.$reset = () => {
        store.$patch(initialState);
    };
});

message.config({
    duration: 1.5,
    maxCount: 1,
});

const app = createApp(App)
app.use(pinia)
app.use(vueInfiniteScroll)
app.use(routers)
app.mount('#app')
