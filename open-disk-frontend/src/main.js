import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import Antd from 'ant-design-vue';
import { createPinia } from 'pinia'
import {useAuthStore} from "@/store/auth.js";

const app = createApp(App);
const pinia = createPinia()

window.addEventListener('storage', (e) => {
    if (e.key === 'jwtToken') {
        const auth = useAuthStore();
        auth.token = e.newValue || '';
        // 如果被清空，自动登出
        if (!e.newValue) auth.user = null;
    }
});


app.use(pinia)  // 注册 Pinia
app.use(router);
app.use(Antd);
app.mount('#app');
