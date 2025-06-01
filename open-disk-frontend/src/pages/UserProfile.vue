<template>
  <div class="profile-page">
    <!-- 垂直堆叠三张卡片 -->
    <a-space direction="vertical" size="large" style="width: 100%">
      <!-- 个人信息 -->
      <a-card bordered class="profile-card">
        <div class="header">
          <a-avatar :size="72"><UserOutlined /></a-avatar>
          <div class="info">
            <h2 class="username">{{ user.username }}</h2>
            <div class="subtitle">默认角色：普通用户</div>
          </div>
        </div>

        <a-descriptions title="基本信息" :column="1" size="middle" bordered>
          <a-descriptions-item label="用户名">{{ user.username }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">user01@example.com</a-descriptions-item>
          <a-descriptions-item label="注册时间">2025‑01‑01 10:00:00</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 修改密码 -->
      <a-card type="inner" title="修改密码" bordered class="inner-card">
        <a-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="rules"
            layout="vertical"
            @finish="onChangePwd"
        >
          <a-form-item label="旧密码" name="oldPwd" has-feedback>
            <a-input-password v-model:value="pwdForm.oldPwd" placeholder="请输入旧密码" />
          </a-form-item>
          <a-form-item label="新密码" name="newPwd" has-feedback>
            <a-input-password v-model:value="pwdForm.newPwd" placeholder="请输入新密码（≥6位）" />
          </a-form-item>
          <a-form-item label="确认新密码" name="confirmPwd" dependencies="newPwd" has-feedback>
            <a-input-password v-model:value="pwdForm.confirmPwd" placeholder="请再次输入新密码" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" block>更新密码</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 账户操作 -->
      <a-card type="inner" title="账户操作" bordered class="inner-card">
        <a-space>
          <a-button type="primary" danger @click="onLogout">退出登录</a-button>
          <a-popconfirm
              title="确定要注销账号？此操作不可恢复"
              ok-text="确认"
              cancel-text="取消"
              @confirm="onDeleteAccount"
          >
            <a-button danger>注销账号</a-button>
          </a-popconfirm>
        </a-space>
      </a-card>
    </a-space>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { UserOutlined } from '@ant-design/icons-vue';
import { useAuthStore } from '@/store/auth.js';
import { useRouter } from 'vue-router';

/* 用户信息 —— demo 默认值 */
const auth = useAuthStore();
const router = useRouter();
const user = reactive({
  username: auth.user?.username || 'user01',
});

/* 修改密码 */
const pwdFormRef = ref(null);
const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' });
const rules = {
  oldPwd: [{ required: true, message: '请输入旧密码' }],
  newPwd: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '至少 6 个字符' },
  ],
  confirmPwd: [
    { required: true, message: '请再次输入新密码' },
    ({ getFieldValue }) => ({
      validator(_, value) {
        if (!value || getFieldValue('newPwd') === value) {
          return Promise.resolve();
        }
        return Promise.reject(new Error('两次输入的密码不一致'));
      },
    }),
  ],
};
function onChangePwd() {
  message.success('演示：密码已更新');
  Object.assign(pwdForm, { oldPwd: '', newPwd: '', confirmPwd: '' });
  pwdFormRef.value?.resetFields();
}

/* 退出登录 */
function onLogout() {
  auth.clearToken();
  router.push('/login');
  message.success('已退出登录');
}

/* 注销账号（演示） */
function onDeleteAccount() {
  message.success('演示：账号已注销');
  auth.clearToken();
  router.push('/register');
}
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 16px;
}
.profile-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}
.username {
  margin: 0;
}
.subtitle {
  color: #888;
  font-size: 14px;
  margin-top: 4px;
}
.inner-card {
  background: #fafafa;
  border-radius: 8px;
}
</style>
