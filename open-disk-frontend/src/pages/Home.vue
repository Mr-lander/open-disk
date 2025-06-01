<template>
  <div style="padding: 24px;">
<!--    <a-alert>用户名:</a-alert>>-->
    <a-page-header
        title="我的文件"
        sub-title="查看和管理您已上传的文件"
        style="background: #fff; margin-bottom: 16px;"
    />

    <div class="toolbar" style="margin-bottom: 16px; display: flex; gap: 8px;">
      <!-- 搜索框 -->
      <a-input-search
          v-model:value="searchQuery"
          placeholder="按文件名搜索"
          enter-button
          @search="onSearch"
          :allow-clear="true"
          style="width: 300px;"
      />
      <!-- 刷新按钮 -->
      <a-button type="primary" @click="resetAndFetch">刷新文件列表</a-button>
<!--      <a-button type="primary" @click="fetchFiles" style="margin-bottom: 16px;">-->
<!--        刷新文件列表-->
<!--      </a-button>-->
    </div>


    <!-- 文件列表表格 -->
    <a-table :columns="columns" :data-source="tableData" :loading="loading" rowKey="cipherKey">
      <template #fileName="{ text, record }">
        <div class="file-name-wrapper" @mouseenter="record.hover = true" @mouseleave="record.hover = false">
          <!-- 点击文件名触发预览 -->
          <span class="file-name" @click="openPreview(record)" style="cursor: pointer;">{{ text }}</span>
          <span class="action-icons" :style="{ opacity: record.hover ? 1 : 0 }">
            <a-button type="link" size="small" @click="download(record)">
              <DownloadOutlined />
            </a-button>
            <a-button type="link" size="small" @click="openShareModal(record)">
              <ShareAltOutlined />
            </a-button>
            <a-button type="link" size="small" @click="more(record)">
              <MoreOutlined />
            </a-button>
            <a-button
                type="link"
                size="small"
                @click="onDelete(record)"
            >
                <DeleteOutlined />
            </a-button>
          </span>
        </div>
      </template>
    </a-table>

    <!-- 预览 Modal -->
<!--    <a-modal v-model:visible="previewVisible" footer={null} @cancel="closePreview" width="80%">-->
<!--      <template #title>-->
<!--        文件预览 - {{ previewFile?.fileName }}-->
<!--      </template>-->
<!--      <div v-if="previewFile">-->
<!--        <component :is="previewComponent" :file="previewFile" />-->
<!--      </div>-->
<!--    </a-modal>-->
    <!-- FileList.vue 中的 Modal 预览部分 -->
    <a-modal
        v-model:visible="previewVisible"
        footer={null}
        @cancel="closePreview"
        width="80%"
    >
      <template #title>
        文件预览 - {{ previewFile.fileName }}
      </template>

      <!-- 这里不用 <component :is="…">，改成原生标签 + v-if -->
      <div v-if="previewFile">
        <!-- 图片 -->
        <img
            v-if="previewFile.contentType.startsWith('image/')"
            :src="previewFile.fileUrl"
            alt="Image Preview"
            style="max-width: 100%;"
        />

        <!-- 视频 -->
        <video
            v-else-if="previewFile.contentType.startsWith('video/')"
            :src="previewFile.fileUrl"
            controls
            style="max-width: 100%;"
        ></video>

        <!-- PDF -->
        <iframe
            v-else-if="previewFile.contentType === 'application/pdf'"
            :src="previewFile.fileUrl"
            style="width: 100%; height: 80vh;"
            frameborder="0"
        ></iframe>

        <!-- 其他：用 iframe 简单嵌入 -->
        <iframe
            v-else
            :src="previewFile.fileUrl"
            style="width: 100%; height: 80vh;"
            frameborder="0"
        ></iframe>
      </div>
    </a-modal>

    <a-modal v-model:visible="shareModalVisible" title="分享文件" @ok="generateShareLink" @cancel="closeShareModal">
      <div>
        <a-form layout="vertical">
          <a-form-item label="过期日期">
            <a-date-picker v-model:value="expireDate" />
          </a-form-item>
          <a-form-item>
            <a-alert message="所有人都可访问" type="info" />
          </a-form-item>
        </a-form>
        <div v-if="shareLink">
          <a-input :value="shareLink" readonly />
          <a-button type="primary" @click="copyLink" style="margin-top: 8px;">复制链接</a-button>
        </div>
      </div>
    </a-modal>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { message } from 'ant-design-vue';
import { useAuthStore } from '@/store/auth.js';
import moment from 'moment';
import { DownloadOutlined, ShareAltOutlined, MoreOutlined,DeleteOutlined } from '@ant-design/icons-vue';
import instance from "@/api/axiosOpenDisk.js";
import fileApi from '@/api/file.js';
// 表格数据和加载状态
const tableData = ref([]);
const loading = ref(false);
const searchQuery = ref('');    // <-- 增加查询字段

// 预览相关状态
const previewVisible = ref(false);
const previewFile = ref(null);

const shareModalVisible = ref(false);
const currentRecord = ref(null);
const shareLink = ref('');
const expireDate = ref(null);

// 根据文件类型选择预览组件
const previewComponent = computed(() => {
  if (!previewFile.value) return 'div';
  const type = previewFile.value.contentType;
  if (type.startsWith('image/')) {
    // 图片预览组件
    return {
      template: `<img :src="file.fileUrl" alt="Image Preview" style="max-width: 100%;"/>`,
      props: ['file']
    };
  } else if (type.startsWith('video/')) {
    // 视频预览组件
    return {
      template: `<video :src="file.fileUrl" controls style="max-width: 100%;"></video>`,
      props: ['file']
    };
  } else if (type === 'application/pdf') {
    // PDF 预览组件
    return {
      template: `<iframe :src="file.fileUrl" style="width: 100%; height: 600px;" frameborder="0"></iframe>`,
      props: ['file']
    };
  } else {
    // 默认预览：尝试 iframe 嵌入
    return {
      template: `<iframe :src="file.fileUrl" style="width: 100%; height: 600px;" frameborder="0"></iframe>`,
      props: ['file']
    };
  }
});

// 表格列配置
const columns = [
  {
    title: '文件名称',
    dataIndex: 'fileName',
    key: 'fileName',
    slots: { customRender: 'fileName' },
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    key: 'fileSize',
    customRender: ({ text }) => formatSize(text),
  },
  {
    title: '内容类型',
    dataIndex: 'contentType',
    key: 'contentType',
  },
  {
    title: '最后修改',
    dataIndex: 'lastModified',
    key: 'lastModified',
    customRender: ({ text }) => moment.unix(text).format('YYYY-MM-DD HH:mm:ss'),
  },
];

// 辅助函数：格式化文件大小
function formatSize(size) {
  if (size < 1024) return size + ' B';
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB';
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

// 打开预览：设置预览文件并显示 Modal
// 新版
async function openPreview(record) {
  try {
    // 调后端拿 inline URL
    const { data: url } = await fileApi.getPreviewUrl(record.cipherKey);
    // 把 URL 注入到 previewFile，Modal 里用 file.fileUrl
    previewFile.value = { ...record, fileUrl: url };
    previewVisible.value = true;
  } catch (e) {
    message.error('预览失败：' + e.message);
  }
}
// 关闭预览
function closePreview() {
  previewVisible.value = false;
}


function openShareModal(record) {
  currentRecord.value = record;
  shareModalVisible.value = true;
}

function closeShareModal() {
  shareModalVisible.value = false;
  expireDate.value = null;
  shareLink.value = '';
}

// 其他操作
async function generateShareLink() {
  try {
    const { data: url } = await fileApi.getDownloadUrl(currentRecord.value.cipherKey);
    shareLink.value = url;
  } catch (e) {
    message.error('生成分享链接失败：' + e.message);
  }
}

function copyLink() {
  navigator.clipboard.writeText(shareLink.value).then(() => {
    message.success('链接已复制');
  }).catch(() => {
    message.error('复制失败');
  });
}

async function download(record) {
  try {
    // 调后端拿 attachment URL
    const { data: url } = await fileApi.getDownloadUrl(record.cipherKey);
    window.open(url, '_blank');
  } catch (e) {
    message.error('下载失败：' + e.message);
  }
}

function share(record) {
  message.info(`分享文件：${record.fileName}`);
}

function more(record) {
  message.info(`更多操作：${record.fileName}`);
}

// async function fetchFiles() {
//   loading.value = true;
//   try {
//     const token = useAuthStore().token;
//     const { data } = await fileApi.getAllMeta({
//       headers: { Authorization: `Bearer ${token}` }
//     });
//     // 后端返回的是 FileMetadataDto[]：{ fileName, fileSize, contentType, lastModified, cipherKey }
//     tableData.value = data.map(item => ({ ...item, hover: false }));
//   } finally {
//     loading.value = false;
//   }
// }

// 刷新列表（全量）
async function fetchFiles() {
  loading.value = true;
  try {
    const token = useAuthStore().token;
    const { data } = await fileApi.getAllMeta({ headers: { Authorization: `Bearer ${token}` } });
    tableData.value = data.map(item => ({ ...item, hover: false }));
  } catch (e) {
    message.error('获取文件列表失败：' + e.message);
  } finally {
    loading.value = false;
  }
}

// 搜索
async function onSearch(query) {
  loading.value = true;
  try {
    const token = useAuthStore().token;
    let result;
    if (!query) {
      // 为空则重置为全量
      result = (await fileApi.getAllMeta({ headers: { Authorization: `Bearer ${token}` } })).data;
    } else {
      result = (await fileApi.searchFiles(
          { params: { query }, headers: { Authorization: `Bearer ${token}` } }
      )).data;
    }
    tableData.value = result.map(item => ({ ...item, hover: false }));
  } catch (e) {
    message.error('搜索失败：' + e.message);
  } finally {
    loading.value = false;
  }
}

// 清空搜索并刷新
function resetAndFetch() {
  searchQuery.value = '';
  fetchFiles();
}

async function onDelete(record) {
  try {
    await fileApi.deleteFile(record.cipherKey);
    message.success('删除成功');
    fetchFiles();
  } catch (e) {
    message.error('删除失败：' + e.message);
  }
}


onMounted(() => {
  fetchFiles();
});

</script>

<style scoped>
.file-name-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  padding-right: 80px;
}
.file-name {
  flex: 1;
  /* 可添加下划线或颜色区分链接效果 */
  color: #1890ff;
}
.action-icons {
  transition: opacity 0.3s;
}
.action-icons .ant-btn-link {
  padding: 0 4px;
}
</style>
