<!-- src/pages/Register.vue -->
<template>
  <a-form @submit.prevent="handleSubmit">
    <a-form-item label="用户名">
      <a-input v-model:value="username" placeholder="请输入用户名" />
    </a-form-item>
    <a-form-item label="密码">
      <a-input-password v-model:value="password" placeholder="请输入密码" />
    </a-form-item>
    <!-- 可根据 savedUserDto 添加更多字段，例如 email、手机号等 -->
    <a-form-item>
      <a-button type="primary" htmlType="submit">注册</a-button>
    </a-form-item>
  </a-form>
</template>

<script>
import { ref } from 'vue';
import userApi from '@/api/user';
import { useRouter } from 'vue-router';

export default {
  name: 'Register',
  setup() {
    const username = ref('');
    const password = ref('');
    const router = useRouter();

    const handleSubmit = async () => {
      try {
        await userApi.register({
          name: username.value,
          password: password.value,
          // 补充其他字段
        });
        console.log('注册成功');
        // 注册成功后跳转到登录页
        router.push('/login');
      } catch (error) {
        console.error('注册失败', error);
      }
    };

    return { username, password, handleSubmit };
  },
};
</script>
