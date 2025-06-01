<template>
  <a-page-header
      title="用户管理"
      sub-title="查看和管理所有用户"
      style="padding: 0;"
  />
  <div class="toolbar" style="margin: 16px 0; display: flex; justify-content: space-between;">
    <a-input-search
        v-model:value="searchText"
        placeholder="搜索用户名"
        @search="onSearch"
        style="width: 300px;"
        enter-button
    />
    <a-button type="primary" @click="openCreateModal">创建用户</a-button>
  </div>
  <a-table
      :columns="columns"
      :data-source="filteredUsers"
      :loading="loading"
      row-key="id"
      :pagination="{ pageSize: 10 }"
      scroll="{ x: '100%' }"
  >
    <template #bodyCell="{ column, record }">
      <span v-if="column.key !== 'action'">{{ record[column.dataIndex] }}</span>
      <a-space v-else>
        <a-button type="primary" size="small" @click="goToEdit(record.id)">编辑</a-button>
        <a-button type="default" danger size="small" @click="showDeleteModal(record)">删除</a-button>
      </a-space>
    </template>
  </a-table>

  <!-- 删除确认模态框 -->
  <a-modal
      v-model:visible="isModalVisible"
      title="确认删除"
      @ok="confirmDelete"
      @cancel="cancelDelete"
      okText="确定"
      cancelText="取消"
  >
    <p>确认删除用户 "{{ deletingUser?.name }}" (ID: {{ deletingUser?.id }}) 吗？</p>
  </a-modal>

  <!-- 创建用户模态框 -->
  <a-modal
      v-model:visible="isCreateModalVisible"
      title="创建新用户"
      @ok="confirmCreate"
      @cancel="cancelCreate"
      okText="提交"
      cancelText="取消"
      :mask-closable="false"
      width="400px"
  >
    <a-form
        :model="createForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="用户名">
        <a-input v-model:value="createForm.name" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="密码">
        <a-input-password v-model:value="createForm.password" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item label="角色">
        <a-select v-model:value="createForm.role" placeholder="请选择角色">
          <a-select-option value="user">用户</a-select-option>
          <a-select-option value="admin">管理员</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import userApi from '@/api/user.js';

const router = useRouter();
const users = ref([]);
const loading = ref(false);
const searchText = ref('');

// 删除相关状态
const isModalVisible = ref(false);
const deletingUser = ref(null);

// 创建相关状态
const isCreateModalVisible = ref(false);
const createForm = reactive({
  name: '',
  password: '',
  role: 'user'
});

const fetchUsers = async () => {
  loading.value = true;
  try {
    const response = await userApi.fetchAllUsers();
    users.value = response.data;
  } catch (error) {
    message.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
};

onMounted(fetchUsers);

const filteredUsers = computed(() => {
  const text = searchText.value.trim().toLowerCase();
  if (!text) return users.value;
  return users.value.filter((u) => u.name.toLowerCase().includes(text));
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 200 },
  { title: '用户名', dataIndex: 'name', key: 'name', width: 200 },
  {
    title: '角色',
    dataIndex: 'role',
    key: 'role',
    filters: [
      { text: '管理员', value: 'admin' },
      { text: '用户', value: 'user' }
    ],
    onFilter: (value, record) => record.role === value,
    width: 150
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 180
  }
];

// 搜索
const onSearch = () => {};

// 编辑
const goToEdit = (id) => {
  router.push(`/users/edit/${id}`);
};

// 删除
const showDeleteModal = (user) => {
  deletingUser.value = user;
  isModalVisible.value = true;
};
const cancelDelete = () => {
  isModalVisible.value = false;
  deletingUser.value = null;
};
const confirmDelete = async () => {
  try {
    await userApi.deleteUser(deletingUser.value.id);
    message.success('删除用户成功');
    fetchUsers();
  } catch (error) {
    message.error('删除用户失败');
  } finally {
    isModalVisible.value = false;
    deletingUser.value = null;
  }
};

// 创建
const openCreateModal = () => {
  isCreateModalVisible.value = true;
};
const cancelCreate = () => {
  isCreateModalVisible.value = false;
  Object.assign(createForm, { name: '', password: '', role: 'user' });
};
const confirmCreate = async () => {
  try {
    await userApi.register({ ...createForm });
    message.success('创建用户成功');
    cancelCreate();
    fetchUsers();
  } catch (error) {
    message.error('创建用户失败');
  }
};
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
/* 模态框背景模糊 */
.ant-modal-wrap {
  backdrop-filter: blur(5px);
}
</style>
