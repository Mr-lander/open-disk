package com.yyh.file.service;

import com.yyh.commonlib.utils.HashUtils;
import com.yyh.file.client.KesVaultWebClient;
import com.yyh.file.client.UidServiceClient;
import com.yyh.file.domain.File;
import com.yyh.file.repository.FileRepository;
import com.yyh.file.search.FileDocument;
import com.yyh.file.search.FileSearchRepository;
import com.yyh.file.socket.WebSocketSessionHolder;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;


// reactor flux
@Service
public class FileService {

//    private final Vault vault;
    private final Path fileStorageLocation;
    private final FileRepository fileRepository;
    private final WebSocketSessionHolder sessionHolder;
    private final UidServiceClient uidServiceClient;
    private final MinioService minioService; // 只负责热端上传


    private final MinioClient hotMinioClient;              // 新增：直调热端 MinIO
    private final MinioClient coldMinioClient;             // 新增：直调冷端 Ceph RGW

    private final KesVaultWebClient vaultClient;           // 新增：Vault 客户端
    private final FileSearchRepository fileSearchRepository;


    public FileService(@Value("${file.upload-dir}") String uploadDir, FileRepository fileRepository,
                       WebSocketSessionHolder sessionHolder, UidServiceClient uidServiceClient,  // 在这里给参数加 @Qualifier
                       //关键：@Qualifier 一定要贴在构造器参数上（Spring 才能在解析依赖时用它来区分 Bean），而不是只放在字段上。
                       @Qualifier("hotMinioClient") MinioClient hotMinioClient,
                       @Qualifier("coldMinioClient") MinioClient coldMinioClient,
                        MinioService minioService,
             KesVaultWebClient vaultClient, FileSearchRepository fileSearchRepository) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;
        this.sessionHolder = sessionHolder;
        this.uidServiceClient = uidServiceClient;
        this.minioService = minioService;
        this.hotMinioClient = hotMinioClient;
        this.coldMinioClient = coldMinioClient;
        this.vaultClient = vaultClient;
        this.fileSearchRepository = fileSearchRepository;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }
    public Mono<String> storeFile(FilePart file, String userId, String sessionId,long fileSize) {
        String fileName = file.filename();
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        AtomicLong totalBytesWritten = new AtomicLong(0);

        return uidServiceClient.fetchUid().flatMap(fileId -> {
            // 使用 DataBufferUtils.write() 写入文件，同时在每个数据块中统计字节数并推送进度
            return DataBufferUtils.write(
                            file.content().doOnNext(dataBuffer -> {
                                int bytes = dataBuffer.readableByteCount();
                                long total = totalBytesWritten.addAndGet(bytes);
                                int progress = (int) ((total * 100) / fileSize);
                                // 推送实时进度更新
                                var wrapper = sessionHolder.getSessionWrapper(sessionId);
                                if (wrapper != null) {
                                    wrapper.getSink().tryEmitNext(
                                            wrapper.getSession().textMessage("{\"progress\": " + progress + "}")
                                    );
                                }
                            }),
                            targetLocation,
                            StandardOpenOption.CREATE
                    )
                    // 当文件写入完成后，再执行后续操作
                    .then(Mono.fromCallable(() -> {
                        // 使用文件路径计算哈希（这里有一个同步方法，根据文件路径计算 SHA256）
                        String hash = HashUtils.calculateSHA256_LocalPath(targetLocation);
                        // 获取 Content-Type（如果没有则默认 application/octet-stream）
                        String contentType = file.headers().getContentType() != null
                                ? file.headers().getContentType().toString()
                                : "application/octet-stream";
                        System.out.println("上传文件类型: " + contentType);

                        // 保存文件元数据到数据库（注意：此处 fileRepository.save 是阻塞操作，建议后续改为响应式或使用 Schedulers.boundedElastic()）
                        File fileEntity = new File();
                        fileEntity.setFileId(fileId);
                        fileEntity.setFileName(file.filename());
                        fileEntity.setFile_hash(hash);
                        fileEntity.setFileOwner(Long.valueOf(userId));
                        fileEntity.setContent_type(contentType);
                        fileEntity.setFilePath("null");
                        fileEntity.setUpload_time(LocalDateTime.now());
                        fileEntity.setFile_size(String.valueOf(fileSize));
                        fileEntity.setAccess_count(1);
                        fileEntity.setStorageTier("HOT");
                        fileEntity.setLast_accessed(LocalDateTime.now());
                        fileRepository.save(fileEntity);
                        FileDocument doc = new FileDocument(
                                fileId, Long.valueOf(userId), fileName, "HOT",
                                fileEntity.getUpload_time().toInstant(ZoneOffset.UTC));
                        fileSearchRepository.save(doc);

                        return targetLocation.toString();
                    }))
                    .flatMap(path -> {
                        // 异步上传到 MinIO，并更新数据库
                        return minioService.uploadFileToMinio(targetLocation, userId)
                                .doOnSuccess(minioPath -> {
                                    File fileEntity = fileRepository.findByFileId(fileId);
                                    fileEntity.setFilePath(minioPath);
                                    fileRepository.save(fileEntity);
                                    try {
                                        Files.delete(targetLocation); // 删除本地文件缓存
                                        System.out.println("已删除文件缓存，id：" + fileId);
                                    } catch (IOException e) {
                                        throw new RuntimeException("删除本地文件失败", e);
                                    }
                                })
                                .thenReturn(path);
                    })
                    .doOnNext(path -> {
                        // 文件存储及 MinIO 上传完成后，确保推送最终进度 100%
                        var wrapper = sessionHolder.getSessionWrapper(sessionId);
                        if (wrapper != null) {
                            wrapper.getSink().tryEmitNext(
                                    wrapper.getSession().textMessage("{\"progress\": 100}")
                            );
                        }
                    })
                    .onErrorMap(e -> new RuntimeException("存储文件失败：" + file.filename(), e));
        });
    }

    public Mono<Void> deleteFile(Long userId, String cipherKey) {
        return vaultClient.getEncryptionKey(userId)
                .flatMap(key -> {
                    String objectName = HashUtils.decryptWithKey(cipherKey, key);
                    File file = fileRepository.findByFileOwnerAndFileName(userId, objectName);
                    if (file == null) {
                        return Mono.error(new RuntimeException("文件不存在"));
                    }

                    // 选择客户端：HOT 用 hotMinioClient，COLD 用 coldMinioClient
                    MinioClient client =
                            "COLD".equalsIgnoreCase(file.getStorageTier())
                                    ? coldMinioClient
                                    : hotMinioClient;

                    // 1）删 MinIO/Ceph RGW 上的对象
                    Mono<Void> deleteObject = Mono.fromRunnable(() -> {
                        try {
                            client.removeObject(
                                    RemoveObjectArgs.builder()
                                            .bucket(String.valueOf(userId))
                                            .object(objectName)
                                            .build()
                            );
                        } catch (Exception e) {
                            throw new RuntimeException("删除对象失败: " + e.getMessage(), e);
                        }
                    });

                    // 2）删 MySQL 元数据
                    Mono<Void> deleteDb = Mono.fromRunnable(() -> {
                        fileRepository.deleteById(file.getFileId());
                    });

                    // 3）删 ES 索引
                    Mono<Void> deleteEs = Mono.fromRunnable(() -> {
                        fileSearchRepository.deleteById(file.getFileId());
                    });

                    return deleteObject
                            .then(deleteDb)
                            .then(deleteEs);
                });
    }



}