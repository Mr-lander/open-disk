// common-lib/src/main/java/com/yyh/common/dto/FileMetadataDto.java
package com.yyh.file.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文件元数据 DTO —— 在 file-svc & download-svc & 前端间共享
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataDto {
    private String fileName;
    private long fileSize;
    private String contentType;
    private OffsetDateTime lastModified;
    /** 
     * 对象名称的 AES 加密密钥（cipherKey），前端用它向 download-svc 请求预览/下载 
     */
    private String cipherKey;
}
