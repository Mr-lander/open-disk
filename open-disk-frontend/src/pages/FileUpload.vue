<template>
  <div>
    <a-card title="文件上传" style="max-width: 600px; margin: 20px auto;">
      <a-form @submit.prevent="handleSubmit">
        <a-form-item label="选择文件">
          <!-- 使用 a-upload 组件来选择文件 -->
          <a-upload
              :beforeUpload="fileUploadStore.beforeUpload"
              :fileList="fileUploadStore.fileList"
              :showUploadList="true"
              :disabled="fileUploadStore.uploading"
          >
            <a-button icon="upload" :disabled="fileUploadStore.uploading">选择文件</a-button>
          </a-upload>
        </a-form-item>
          <a-form-item>
            <a-button type="primary" htmlType="submit" :disabled="fileUploadStore.uploading">上传文件</a-button>
          </a-form-item>
        <a-progress :percent="fileUploadStore.uploadProgress" id="progress" status="active" style="margin-top: 20px" />
      </a-form>
      <!-- 上传中提示 -->
      <div v-if="fileUploadStore.uploading" style="margin-top: 16px;">
        <a-alert message="上传中，请稍候…" type="info" show-icon />
      </div>

      <!-- 上传结束才显示结果 -->
      <div v-else-if="fileUploadStore.uploadMessage" style="margin-top: 16px;">
        <a-alert
            :message="fileUploadStore.uploadMessage"
            :type="fileUploadStore.uploadMessage.includes('成功') ? 'success' : 'error'"
            show-icon
        />
      </div>

    </a-card>
  </div>
</template>

<script>
import {onMounted} from 'vue';
import fileApi from '@/api/file';
import {useFileUploadStore} from "@/store/fileUploadStore.js";
export default {
  name: 'FileUpload',
  setup() {
    const fileUploadStore = useFileUploadStore();
    //上传状态禁止重新选择
    onMounted(() => {
      fileUploadStore.initStore();
    });

    // 提交文件上传请求，携带 userId 与 sessionId
    const handleSubmit = async () => {
      await fileUploadStore.uploadFile(fileApi)
    };
    return {  fileUploadStore, handleSubmit  };
    //解构 —— 失去活性
    // return {
    //   ...fileUploadStore,
    //   handleSubmit,
    //
    // };
  }
};
</script>

<style scoped>
/* 根据需要调整样式 */
</style>
