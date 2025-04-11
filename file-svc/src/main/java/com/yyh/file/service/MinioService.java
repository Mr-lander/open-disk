package com.yyh.file.service;

import com.yyh.file.domain.FileMetadataDto;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Reactor flux
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

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
    public Flux<FileMetadataDto> getAllFilesMetadata(String userId, int urlExpirySeconds) {
        return Mono.fromCallable(() -> {
                    // 检查桶是否存在
                    boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(userId).build());
                    if (!bucketExists) {
                        throw new RuntimeException("用户桶不存在");
                    }
                    return userId;
                }).thenMany(Flux.create(sink -> {
                    try {
                        Iterable<Result<Item>> items = minioClient.listObjects(
                                ListObjectsArgs.builder().bucket(userId).build()
                        );
                        for (Result<Item> result : items) {
                            Item item = result.get();
                            sink.next(item);
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(new RuntimeException("列出 MinIO 文件失败", e));
                    }
                })).cast(Item.class)
                .flatMap(item -> {
                    String objectName = item.objectName();
                    // 获取文件元数据并生成签名 URL
                    return Mono.fromCallable(() -> {
                        StatObjectResponse stat = minioClient.statObject(
                                StatObjectArgs.builder().bucket(userId).object(objectName).build()
                        );
                        FileMetadataDto dto = new FileMetadataDto();
                        dto.setFileName(objectName);
                        dto.setFileSize(stat.size());
                        dto.setContentType(stat.contentType());
                        dto.setLastModified(stat.lastModified().toOffsetDateTime());
                        String signedUrl = minioClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(userId)
                                        .object(objectName)
                                        .expiry(urlExpirySeconds)
                                        .build()
                        );
                        dto.setFileUrl(signedUrl);
                        return dto;
                    }).onErrorResume(e -> Mono.empty());
                });
    }


}


// Spring MVC
//
//@Service
//public class MinioService {
//
//    @Autowired
//    private MinioClient minioClient;
//    // 其他已有的属性与构造方法略...
//
//    public String uploadFileToMinio(Path localFilePath, String userId) throws Exception {
//        // 使用 userId 作为桶名
//        String bucketName = userId;
//
//        // 检查桶是否存在
//        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
//        if (!bucketExists) {
//            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
//        }
//
//        // 获取文件名
//        String originalFileName = localFilePath.getFileName().toString();
//
//        // 检查文件是否存在
//        File localFile = localFilePath.toFile();
//        if (!localFile.exists()) {
//            throw new IOException("文件不存在：" + originalFileName);
//        }
//
//        // 上传文件到 MinIO
//        try (FileInputStream fis = new FileInputStream(localFile)) {
//            minioClient.putObject(
//                    PutObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(originalFileName)
//                            .stream(fis, localFile.length(), -1)
//                            .contentType("application/octet-stream") // 默认的文件类型
//                            .build()
//            );
//        } catch (IOException e) {
//            throw new IOException("上传文件到 MinIO 失败", e);
//        }
//
//        // 返回文件在 MinIO 中的存储路径（bucket/objectName）
//        return bucketName + "/" + originalFileName;
//    }
//
//    // 获取用户桶中的所有文件
//    public List<String> getAllFilesInUserBucket(String userId) throws Exception {
//        List<String> fileNames = new ArrayList<>();
//
//        // 检查桶是否存在
//        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(userId).build());
//        if (!bucketExists) {
//            throw new RuntimeException("用户桶不存在");
//        }
//
//        // 列出用户桶中的所有对象（文件）
//        Iterable<Result<Item>> objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(userId).build());
//        for (Result<Item> object : objects) {
//            Item item = object.get();
//            fileNames.add(item.objectName());  // 将文件名添加到返回列表中
//        }
//
//        return fileNames;  // 返回文件名列表
//    }
//
//}
