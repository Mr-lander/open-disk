// src/api/axiosOpenDisk.js
import axios from 'axios';
import {useAuthStore} from "@/store/auth.js";
import router from "@/router/index.js";
import {ElMessage} from "element-plus";

const instance = axios.create({
    baseURL: '/api/v1', // 使用代理后的相对路径
    timeout: 0,
});

// 请求拦截器：从 localStorage 中读取 token，并附加到请求头中
instance.interceptors.request.use(
    (config) => {
        // 直接从 Pinia store（推荐）
        const authStore = useAuthStore();
        const token = authStore.token;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

instance.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            const message = error.response.data.error || '认证失败，请重新登录';
            ElMessage.error(message);
            const authStore = useAuthStore();
            authStore.logout();  // 清除 token
            // 跳转到登录页面
            router.push('/login')
            // router.push('/login');
        }
        return Promise.reject(error);
    }
);

export default instance;
