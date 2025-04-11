package com.yyh.file.service;

import com.yyh.commonlib.utils.HashUtils;
import com.yyh.file.client.UidServiceClient;
import com.yyh.file.client.VaultServiceClient;
import com.yyh.file.domain.File;
import com.yyh.file.repository.FileRepository;
import com.yyh.file.socket.WebSocketSessionHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;


// reactor flux
@Service
public class FileService {

//    private final Vault vault;
    private final Path fileStorageLocation;
    private final FileRepository fileRepository;
    private final WebSocketSessionHolder sessionHolder;
    private final UidServiceClient uidServiceClient;
    private final MinioService minioService;

    @Autowired
    private VaultServiceClient vaultServiceClient;

    public FileService(@Value("${file.upload-dir}") String uploadDir, FileRepository fileRepository,
                       WebSocketSessionHolder sessionHolder, UidServiceClient uidServiceClient, MinioService minioService) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;
        this.sessionHolder = sessionHolder;
        this.uidServiceClient = uidServiceClient;
        this.minioService = minioService;
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
                        // 使用文件路径计算哈希（这里假设有一个同步方法，根据文件路径计算 SHA256）
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
                        fileEntity.setFile_owner(userId);
                        fileEntity.setContent_type(contentType);
                        fileEntity.setFile_path("null");
                        fileEntity.setUpload_time(LocalDateTime.now());
                        fileEntity.setFile_size(String.valueOf(fileSize));
                        fileRepository.save(fileEntity);
                        return targetLocation.toString();
                    }))
                    .flatMap(path -> {
                        // 异步上传到 MinIO，并更新数据库
                        return minioService.uploadFileToMinio(targetLocation, userId)
                                .doOnSuccess(minioPath -> {
                                    File fileEntity = fileRepository.findByFileId(fileId);
                                    fileEntity.setFile_path(minioPath);
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

//    public Mono<String> storeFile(FilePart file, String userId, String sessionId) {
//        String fileName = file.filename();
//        Path targetLocation = this.fileStorageLocation.resolve(fileName);
//
//        AtomicLong totalBytesWritten = new AtomicLong(0);
//
//        return uidServiceClient.fetchUid().flatMap(fileId -> {
//            // 先计算哈希，然后保存文件到本地，异步上传到minio
//            return HashUtils.calculateSHA256_FilePart(file)
//                    .flatMap(hash -> {
//                        return file.transferTo(targetLocation)
//                                .then(Mono.fromCallable(() -> {
//                                    // FilePart 没有直接的 content_type 属性。不过，我们可以通过 FilePart 的 headers() 方法来获取文件的 Content-Type，也就是 MIME 类型。
//                                    String contentType = file.headers().getContentType() != null
//                                            ? file.headers().getContentType().toString()
//                                            : "application/octet-stream";
//                                    System.out.println("上传文件类型: "+contentType);
//                                    // 这里可以保存文件元数据到数据库
//                                    File fileEntity = new File();
//                                    fileEntity.setFileId(fileId);
//                                    fileEntity.setFileName(file.filename());
//                                    fileEntity.setFile_hash(hash);
//                                    fileEntity.setFile_owner(userId);
//                                    fileEntity.setContent_type(contentType);
//                                    fileEntity.setFile_path("null");
//                                    fileEntity.setUpload_time(LocalDateTime.now());
//                                    // 假设 fileRepository 是阻塞的，实际应改为响应式
//                                    fileRepository.save(fileEntity);
//                                    return targetLocation.toString();
//                                }));
//                    })
//                    .flatMap(path -> {
//                        // 异步上传到 MinIO 并更新数据库
//                        return minioService.uploadFileToMinio(targetLocation, userId)
//                                .doOnSuccess(minioPath -> {
//                                    // 更新数据库（阻塞，需改为响应式）
//                                    File fileEntity = fileRepository.findByFileId(fileId);
//                                    fileEntity.setFile_path(minioPath);
//                                    fileRepository.save(fileEntity);
//                                    try {
//                                        Files.delete(targetLocation); // 删除本地文件
//                                        System.out.println("已删除文件缓存，id："+fileId);
//                                    } catch (IOException e) {
//                                        throw new RuntimeException("删除本地文件失败", e);
//                                    }
//                                })
//                                .thenReturn(path);
//                    })
//                    .doOnNext(path -> {
//// 通过 Sink 发送 WebSocket 消息
//                        var wrapper = sessionHolder.getSessionWrapper(sessionId);
//                        if (wrapper != null) {
//                            // 通过包装中的 session 创建 WebSocketMessage
//                            WebSocketMessage message = wrapper.getSession().textMessage("{\"progress\": 100}");
//                            // 将消息推送到客户端
//                            wrapper.getSink().tryEmitNext(message);
//                        }
//                    })
//                    .onErrorMap(e -> new RuntimeException("存储文件失败：" + fileName, e));
//        });
//    }


}



// Spring MVC
//@Service
//public class FileService {
//
//    private final Path fileStorageLocation;
//    private final FileRepository fileRepository;
//
//    private final WebSocketSessionHolder sessionHolder; // 自定义类，用于持有 WebSocketSession
//
//    @Autowired
//    private UidClient uidClient;
//
//    @Autowired
//    private MinioService minioService;
//
//    public FileService(@Value("${file.upload-dir}") String uploadDir, FileRepository fileRepository,WebSocketSessionHolder sessionHolder) {
//        // uploadDir 配置为 data
//        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
//        this.fileRepository = fileRepository;
//        this.sessionHolder = sessionHolder;
//        try {
//            Files.createDirectories(this.fileStorageLocation);
//        } catch (IOException ex) {
//            throw new RuntimeException("无法创建文件存储目录", ex);
//        }
//    }
//
//    // 异步任务
//    @Async
//    public void uploadToMinioAndClean(Path localFile, String userId, Long fileId) {
//        try {
//            String filePath = minioService.uploadFileToMinio(localFile, userId);
//            System.out.println("需要更新数据库中fileId： "+fileId);
//            File fileEntity = fileRepository.findByFileId(fileId);
//            fileEntity.setFile_path(filePath);  // 设置 MinIO 的文件路径
//            fileRepository.save(fileEntity);
//            Files.delete(localFile);  // 上传成功后删除本地文件
//
//        } catch (Exception e) {
//            e.printStackTrace(); // 异常处理
//        }
//    }
//
//
//    public String storeFile(MultipartFile file, String userId,String sessionId) {
//        Long file_id = uidClient.getUid();
//
//        // 上传请求
//        System.out.println("收到上传请求");
//        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//
//        Path targetLocation = this.fileStorageLocation.resolve(fileName);
//
//        try (InputStream inputStream = file.getInputStream();
//             FileOutputStream outputStream = new FileOutputStream(targetLocation.toFile())){
//
//            // 上传进度：通过 WebSocket 发送进度
//            long totalSize = file.getSize();
//            long bytesRead = 0;
//            byte[] buffer = new byte[1024];
//            int bytesReadAtOnce;
//
//            var session = sessionHolder.getSession(sessionId);
//            if (session == null || !session.isOpen()) {
//                System.out.println("WebSocket session not found or closed for sessionId: " + sessionId);
//            }
//
//            while ((bytesReadAtOnce = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesReadAtOnce);
//                bytesRead += bytesReadAtOnce;
//
//                int progress = (int) ((bytesRead * 100) / totalSize);
//                System.out.println("Upload progress: " + progress + "%");
//
//                if (session != null && session.isOpen()) {
//                    String message = "{\"progress\": " + progress + "}";
//                    session.send(Mono.just(session.textMessage(message))) //将消息包装成一个 Mono，这是 Reactive 编程的核心。
//                            .subscribe(); // 订阅以触发发送
//                }
//            }
//
//
//
//            // 保存文件元数据到数据库
//            File fileEntity = new File();
//            fileEntity.setFileId(file_id);
//            fileEntity.setFile_path("待配置");
//            fileEntity.setFileName(fileName);
//            fileEntity.setFile_size(String.valueOf(file.getSize()));
//            fileEntity.setContent_type(file.getContentType());
//            fileEntity.setFile_owner(userId);
//            fileEntity.setUpload_time(LocalDateTime.now());
//            fileEntity.setFile_hash(HashUtils.calculateSHA256(file));
//
//            fileRepository.save(fileEntity);
//
//            uploadToMinioAndClean(targetLocation, userId, file_id);
//
//            return targetLocation.toString();
//        } catch (Exception e) {
//            throw new RuntimeException("存储文件失败：" + fileName, e);
//        }
//    }
//}