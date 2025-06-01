package com.yyh.download.service;

import com.yyh.commonlib.utils.HashUtils;
import com.yyh.file.client.KesVaultWebClient;
import com.yyh.file.domain.File;
import com.yyh.file.repository.FileRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// File: DownloadService.java
@Service
public class DownloadService {
    private final KesVaultWebClient vaultClient;
    @Qualifier("hotMinioClient")   private final MinioClient hot;
    @Qualifier("coldMinioClient")  private final MinioClient cold;
    private final FileRepository fileRepo; // JPA

    public DownloadService(KesVaultWebClient vaultClient,
                           @Qualifier("hotMinioClient") MinioClient hot, @Qualifier("coldMinioClient") MinioClient cold, FileRepository fileRepo) {
        this.vaultClient = vaultClient;
        this.hot = hot;
        this.cold = cold;


        this.fileRepo = fileRepo;
    }

    /**
     * 生成预览或下载 URL，并更新访问统计
     * @param userId       用户 ID
     * @param cipherKey    加密后的 objectKey
     * @param attachment   true -> attachment, false -> inline
     */
    public Mono<String> generateUrl(Long userId,
                                    String cipherKey,
                                    boolean attachment) {
        return vaultClient.getEncryptionKey(userId)
                .flatMap(key -> {
                    String objectName = HashUtils.decryptWithKey(cipherKey, key);
                    // 阻塞方式查询并更新统计
                    return Mono.fromCallable(() -> {
                                String fullPath = userId + "/" + objectName;
                                File file = fileRepo.findByFilePath(fullPath);
                                if (file == null) {
                                    throw new RuntimeException("文件不存在");
                                }
                                // 使用实体字段名对应的 setter
                                file.setAccess_count(file.getAccess_count() + 1);
                                file.setLast_accessed(LocalDateTime.now());
                                return fileRepo.save(file);
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            // 生成 Presigned URL
                            .flatMap(f -> Mono.fromCallable(() -> {
                                MinioClient client = "COLD".equals(f.getStorageTier()) ? cold : hot;
                                Map<String, String> params = new HashMap<>();
                                params.put("response-content-disposition",
                                        (attachment ? "attachment" : "inline") +
                                                "; filename=\"" + URLEncoder.encode(f.getFileName(), StandardCharsets.UTF_8) + "\"");
                                if ("application/pdf".equals(f.getContent_type())) {
                                    params.put("response-content-type", "application/pdf");
                                }
                                return client.getPresignedObjectUrl(
                                        GetPresignedObjectUrlArgs.builder()
                                                .method(Method.GET)
                                                .bucket(String.valueOf(userId))
                                                .object(objectName)
                                                .expiry(76800)
                                                .extraQueryParams(params)
                                                .build()
                                );
                            }).subscribeOn(Schedulers.boundedElastic()));
                });
    }
}
