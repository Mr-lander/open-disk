// src/store/auth.js
import { defineStore } from 'pinia';
import { decodeJWT } from '@/utils/jwt.js';
export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: sessionStorage.getItem('jwtToken') || '', // 从 localStorage 获取 token
        user: JSON.parse(sessionStorage.getItem('user')) || null, // 获取用户信息（如果有的话）
    }),
    actions: {
        // 设置 token
        setToken(newToken) {
            this.token = newToken;
            sessionStorage.setItem('jwtToken', newToken);
            // 立即解码，提取 user
            const payload = decodeJWT(newToken);
            this.user = {
                id: payload['x-user-id'],
                name: payload.sub,
                role: payload.role,
            };
            sessionStorage.setItem('user', JSON.stringify(this.user));
        },

        // 清除 token 和用户信息
        clearToken() {
            this.token = '';
            this.user = null;
            sessionStorage.removeItem('jwtToken');
            sessionStorage.removeItem('user');
            console.log("执行clearToken操作,this.token = " + this.token);
        },

        // 设置用户信息
        setUser(userData) {
            this.user = userData;
            sessionStorage.setItem('user', JSON.stringify(userData));
        },

        logout() {
            this.clearToken();
        },

        checkAuth() {
            if (!this.token) return false;
            const { exp } = decodeJWT(this.token);
            if (!exp || exp * 1000 < Date.now()) {
                this.logout();
                return false;
            }
            return true;
        },

    },
});
