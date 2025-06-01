// src/store/fileUploadStore.js
import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useFileUploadStore = defineStore('fileUpload', () => {
    // 状态
    const fileList = ref([]);
    const selectedFile = ref(null);
    const uploadProgress = ref(0);
    const uploading = ref(false);
    const uploadMessage = ref('');
    const sessionId = ref('');

    // WebSocket 连接实例
    let ws = null;

    // 初始化 WebSocket 连接
    const initWebSocket = () => {
        ws = new WebSocket("ws://localhost:8080/ws/upload");
        ws.onopen = () => {
            console.log("WebSocket 已连接");
        };
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.sessionId) {
                    sessionId.value = data.sessionId;
                    console.log("收到 sessionId:", sessionId.value);
                }
                if (data.progress !== undefined) {
                    uploadProgress.value = data.progress;
                }
            } catch (error) {
                console.error("解析 WebSocket 消息错误：", error);
            }
        };
        ws.onclose = () => {
            console.log("WebSocket 已关闭");
        };
        ws.onerror = (error) => {
            console.error("WebSocket 错误：", error);
        };
    };

    // 对外暴露的初始化方法
    const initStore = () => {
        if (!ws || ws.readyState === WebSocket.CLOSED) {
            initWebSocket();
        }
    };

    // 关闭 WebSocket
    const closeWebSocket = () => {
        if (ws) {
            ws.close();
            ws = null;
        }
    };

    // 文件选择拦截逻辑
    const beforeUpload = (file) => {
        if (uploading.value) return false; // 正在上传时不允许选择新文件
        uploadMessage.value = '';
        selectedFile.value = file;
        fileList.value = [file];
        uploadProgress.value = 0; // 重置进度条
        return false; // 阻止组件自动上传
    };

    // 文件上传逻辑
    const uploadFile = async (fileApi) => {
        if (!selectedFile.value) {
            uploadMessage.value = "请先选择文件！";
            return;
        }
               // 开始上传前，清空提示
        uploadMessage.value = '';
        uploading.value = true;
        const formData = new FormData();
        formData.append("file", selectedFile.value);
        formData.append("sessionId", sessionId.value);
        formData.append("fileSize", selectedFile.value.size);
        try {
            const response = await fileApi.uploadFile(formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            uploadMessage.value = "文件上传成功！";
            console.log("上传响应：", response.data);
        } catch (error) {
            uploadMessage.value = "文件上传失败，请重试。";
            console.error("上传错误：", error);
        } finally {
            uploading.value = false;
        }
    };

    return {
        fileList,
        selectedFile,
        uploadProgress,
        uploading,
        uploadMessage,
        sessionId,
        initStore,
        closeWebSocket,
        beforeUpload,
        uploadFile,
    };
});
