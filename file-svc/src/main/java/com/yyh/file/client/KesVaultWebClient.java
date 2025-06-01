package com.yyh.file.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 使用 Spring WebClient 与 kesvault-svc HTTP 通信，避免 Feign 与 WebFlux 不兼容。
 */
@Component
public class KesVaultWebClient {
    private final WebClient webClient;
    private final String secretPathPrefix;

    public KesVaultWebClient(WebClient.Builder builder,
                             @Value("${kesvault.url}") String baseUrl,
                             @Value("${kesvault.secret-path-prefix}") String prefix) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.secretPathPrefix = prefix;
    }

    /**
     * 从 Vault 获取 encryptionKey
     */
    public Mono<String> getEncryptionKey(Long userId) {
        String path = secretPathPrefix + "/" + userId.toString();
        return webClient.post()
            .uri("/api/vault/secret/get")
            .bodyValue(Map.of("path", path))
            .retrieve()
            .bodyToMono(String.class);
    }

    /**
     * 写入 Vault encryptionKey
     */
    public Mono<Void> writeEncryptionKey(String userId, String encryptionKey) {
        String path = secretPathPrefix + "/" + userId;
        return webClient.post()
            .uri("/api/vault/secret")
            .bodyValue(Map.of("path", path, "encryptionKey", encryptionKey))
            .retrieve()
            .bodyToMono(Void.class);
    }
}