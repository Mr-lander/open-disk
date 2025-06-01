// src/api/file.js
import axios from 'axios';
import instance from "./axiosOpenDisk.js";


// 封装文件相关 API
export default {
    // 文件上传，fileData 应该是 FormData 对象
    uploadFile(fileData) {
        return instance.post('/files/upload', fileData);
    },
    getAllMeta(config) {
        return instance.get('/files', config);
    },
    // 预览
    getPreviewUrl(cipherKey) {
        return instance.get('/download/preview', {
            params: { cipher: cipherKey }
        });
    },
    // 下载
    getDownloadUrl(cipherKey) {
        return instance.get('/download/file', {
            params: { cipher: cipherKey }
        });
    },
    // 新增：搜索
    searchFiles(config) {
        // config 里必须包含 params: { query } 和 headers
        return instance.get('/files/search', config);
    },
    deleteFile(cipherKey) {
        return instance.delete('/files', {
            params: { cipher: cipherKey }
        });
    }
};
