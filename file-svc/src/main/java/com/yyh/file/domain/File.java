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

    private LocalDateTime last_accessed; // 上次访问时间

    private int access_count; // 访问次数

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name") // 修改为驼峰命名，数据库列名保持不变
    private String fileName;

    private String file_size;

    // 文件 MIME 类型
    private String content_type;

    @Column(name = "file_owner")
    private Long fileOwner;

    /** 当前对象是在热端（minio）还是冷端（ceph） */
    @Column(name="storage_tier")
    private String storageTier; // "HOT" 或 "COLD"


    // 文件上传时间
    private LocalDateTime upload_time;

    // 文件的哈希值，用于校验完整性或去重（可选）
    private String file_hash;
}
