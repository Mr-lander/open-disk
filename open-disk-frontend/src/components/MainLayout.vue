<template>
  <a-layout style="min-height: 100vh">
    <!-- 侧边栏 -->
    <a-layout-sider collapsible v-model:collapsed="collapsed" theme="dark">
      <div class="logo">云端文件共享系统</div>
      <a-menu
          theme="dark"
          mode="inline"
          :selectedKeys="[selectedKey]"
          @click="handleMenuClick"
      >
        <a-menu-item key="home">
          <span>全部文件</span>
        </a-menu-item>
        <a-menu-item key="file-upload">
          <span>文件上传</span>
        </a-menu-item>
        <a-sub-menu key="user" title="用户管理">
          <a-menu-item key="user-list">用户列表</a-menu-item>
          <a-menu-item key="user-profile">用户资料</a-menu-item>
        </a-sub-menu>
      </a-menu>
      <!-- 退出登录按钮 -->
      <a-button
          type="primary"
          block
          style="position: absolute; bottom: 50px; width: 100%;"
          @click="handleLogout"
      >
        退出登录
      </a-button>
    </a-layout-sider>

    <a-layout class="site-layout">
      <a-layout-header class="site-layout-background header-container">
        <div class="header-title">管理您的文件共享云盘</div>
        <div class="user-info">
          <a-avatar :size="32"><UserOutlined /></a-avatar>
          <span class="username">user01</span>
        </div>
      </a-layout-header>

      <a-breadcrumb style="margin: 16px">
        <a-breadcrumb-item>User01</a-breadcrumb-item>
        <a-breadcrumb-item>List</a-breadcrumb-item>
        <a-breadcrumb-item>File</a-breadcrumb-item>
      </a-breadcrumb>

      <a-layout-content style="margin: 16px">
        <!-- 子路由页面内容在这里呈现 -->
        <keep-alive include="FileUpload">
          <router-view />
        </keep-alive>
      </a-layout-content>

      <a-layout-footer style="text-align: center">
        Copyright @com.yyh.openDisk
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "@/store/auth.js";
import {UserOutlined} from "@ant-design/icons-vue";

const collapsed = ref(false);
const selectedKey = ref('home'); // 默认选中的菜单项
const router = useRouter();
const authStore = useAuthStore(); // 获取 Pinia store

// 菜单点击事件，跳转到相应的页面
function handleMenuClick(e) {
  selectedKey.value = e.key; // 更新选中的菜单项
  switch (e.key) {
    case 'home':
      router.push('/home');
      break;
    case 'file-upload':
      router.push('/upload');
      break;
    case 'user-list':
      router.push('/user-list');
      break;
    case 'user-profile':
      router.push('/user-profile');
      break;
  }
}
// 退出登录处理函数
function handleLogout() {
  authStore.clearToken(); // 清除 token 和用户信息
  router.push('/login'); // 跳转到登录页面
}

onMounted(() => {
  window.addEventListener('storage', e => {
    if (e.key === 'jwtToken') {
      auth.token = e.newValue || '';
      if (!e.newValue) {
        auth.user = null;
        router.replace('/login');
      }
    }
  });
});
</script>

<style scoped>
.site-layout-background {
  background: #fff;
}
.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-container {
  padding: 0 20px;
  display: flex;
  align-items: center;
  color: #000; /* 调整为黑色以适应白色背景 */
}
.header-title {
  font-size: 20px;
  font-weight: bold;
}
.user-info {
  display: flex;
  align-items: center;
  margin-left: auto; /* 将用户信息推到右侧 */
}
.username {
  margin-left: 8px; /* 用户名与头像之间的间距 */
}
</style>