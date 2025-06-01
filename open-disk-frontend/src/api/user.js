// src/api/user.js
import axios from 'axios';
import instance from "@/api/axiosOpenDisk.js";


export default {
    // 登录
    login(data) {
        return instance.post('/users/login', data);
    },
    // 注册（创建用户）
    register(data) {
        return instance.post('/users', data);
    },
    // 获取所有用户（需要鉴权时，可在请求头中传入用户 id 或 token）
    fetchAllUsers() {
        return instance.get('/users/all');
    },
    // 获取用户详情
    fetchUser(id) {
        return instance.get(`/users?id=${id}`);
    },
    // 删除用户（删除接口要求 request body 中传入 id 字符串）
    deleteUser(id) {
        return instance.delete('/users', { data: id });
    },
};
