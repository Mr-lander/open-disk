package com.yyh.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

public class FileDto {

    private Long fileId;

    private String fileName;

    private String fileSize;

    // 文件 MIME 类型
    private String contentType;

    // 文件上传时间
    private LocalDateTime upload_time;

    // 临时URL下载链接
    private String fileUrl;
}
