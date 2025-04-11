package com.yyh.file.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")  // 指定数据库表名为 files
public class File {

    @Id
    @Column(name = "file_id")
    private Long fileId;

    private String file_path;

    @Column(name = "file_name") // 修改为驼峰命名，数据库列名保持不变
    private String fileName;

    private String file_size;

    // 文件 MIME 类型
    private String content_type;

    private String file_owner;

    // 文件上传时间
    private LocalDateTime upload_time;

    // 文件的哈希值，用于校验完整性或去重（可选）
    private String file_hash;
}
