// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/pages/Login.vue';
import Register from '@/pages/Register.vue';
import UserList from '@/pages/UserList.vue';
import UserProfile from '@/pages/UserProfile.vue';
import FileUpload from "@/pages/FileUpload.vue";
import Home from "@/pages/Home.vue";
import MainLayout from "@/components/MainLayout.vue";
import {useAuthStore} from "@/store/auth.js";

const routes = [
    {
        path: '/',
        component: MainLayout,
        // MainLayout 下的子路由都自动带有导航栏
        children: [
            {
                path: '',
                redirect: '/home',
            },
            {
                path: 'home',
                name: 'Home',
                component: Home,
                meta: { requiresAuth: true }, // 需要认证
            },
            {
                path: 'user-profile',
                name: 'UserProfile',
                component: UserProfile,
                meta: { requiresAuth: true }, // 需要认证
            },
            {
                path:'upload',
                name:'FileUpload',
                component: FileUpload,
                meta: { requiresAuth: true }, // 需要认证
            },
            {
                path:'user-list',
                name:'UserList',
                component: UserList,
                meta: { requiresAuth: true }, // 需要认证
            }
            // 其他需要统一导航的页面都放在这里
        ],
    },

    { path: '/login', name: 'Login', component: Login },
    { path: '/register', name: 'Register', component: Register },
    // { path: '/upload', name: 'FileUpload', component: FileUpload }, // 文件上传页面路由
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});
const whiteList = ['/login', '/register'];

// 导航守卫
router.beforeEach((to, from, next) => {
    if (whiteList.includes(to.path)) {
        // 白名单，直接放行
        return next();
    }
    const auth = useAuthStore();
    console.log("jiancha")
    // 先做一次过期校验
    if (!auth.checkAuth()) {
        console.log("过期")
        return next('/login');

    }
    // 再看这个路由需不需要登录
    if (to.matched.some(r => r.meta.requiresAuth) && !auth.token) {
        console.log("你没权限")
        return next('/login');
    }
    next();
});

export default router;
