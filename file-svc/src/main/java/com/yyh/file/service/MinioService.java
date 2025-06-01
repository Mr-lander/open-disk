package com.yyh.file.service;

import com.yyh.file.domain.FileMetadataDto;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

// Reactor flux
@Service
public class MinioService {

    private final MinioClient minioClient;

    public MinioService(@Qualifier("hotMinioClient") MinioClient hotMinioClient) {
        this.minioClient = hotMinioClient;
    }

    /**
     * 上传文件到 MinIO
     * @param localFilePath 本地文件路径
     * @param userId        用户ID（作为桶名）
     * @return 文件在 MinIO 中的存储路径，格式为 bucket/objectName
     */
    public Mono<String> uploadFileToMinio(Path localFilePath, String userId) {
        String bucketName = userId;
        String originalFileName = localFilePath.getFileName().toString();

        return Mono.fromCallable(() -> {
            // 检查桶是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return bucketName;
        }).then(Mono.fromCallable(() -> {
            // 上传文件到 MinIO（阻塞，需优化为异步）
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(originalFileName)
                            .filename(localFilePath.toString())
                            .build()
            );
            return bucketName + "/" + originalFileName;
        })).onErrorMap(e -> new RuntimeException("上传文件到 MinIO 失败", e));
    }

    /**
     * 为指定文件生成临时签名 URL，用于文件预览
     * @param bucketName    桶名（通常为用户ID）
     * @param objectName    文件对象名
     * @param expirySeconds 签名有效期（秒）
     * @return 签名 URL 字符串
     */
    public Mono<String> generateSignedUrl(String bucketName, String objectName, int expirySeconds) {
        return Mono.fromCallable(() ->
                minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(expirySeconds)
                                .build()
                )
        ).onErrorMap(e -> new RuntimeException("生成签名 URL 失败", e));
    }

    /**
     * 获取指定用户桶中所有文件的详细元数据，包括签名 URL
     * @param userId        用户ID（作为桶名）
     * @param urlExpirySeconds 签名 URL 有效期（秒）
     * @return 包含文件元数据的 Flux 列表
     */
    /**
     * 列出用户桶下所有对象并组装元数据DTO，不包含 URL
     */
    public Flux<FileMetadataDto> getAllFilesMetadata(String userId) {
        return Flux.create(sink -> {
            try {
                // 检查桶存在
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(userId).build()
                );
                if (!exists) {
                    sink.error(new RuntimeException("用户桶不存在: " + userId));
                    return;
                }
                // 列出对象
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(userId).build()
                );
                for (Result<Item> res : results) {
                    Item item = res.get();
                    // 获取对象状态用于 size/contentType/lastModified
                    StatObjectResponse stat = minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(userId)
                                    .object(item.objectName())
                                    .build()
                    );
                    FileMetadataDto dto = new FileMetadataDto(
                            item.objectName(),
                            stat.size(),
                            stat.contentType(),
                            stat.lastModified().toOffsetDateTime(),
                            null
                    );
                    sink.next(dto);
                }
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }



}

