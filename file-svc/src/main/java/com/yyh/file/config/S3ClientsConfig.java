package com.yyh.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3ClientsConfig {

    @Bean("hotMinioClient")
    public MinioClient hotMinioClient(@Value("${minio.url}") String endpoint,
                                      @Value("${minio.access-key}") String accessKey,
                                      @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean("coldMinioClient")
    public MinioClient coldMinioClient(@Value("${ceph.rgw.endpoint}") String endpoint,
                                       @Value("${ceph.rgw.access-key}") String accessKey,
                                       @Value("${ceph.rgw.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
