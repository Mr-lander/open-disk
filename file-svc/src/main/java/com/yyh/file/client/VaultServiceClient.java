package com.yyh.file.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class VaultServiceClient {

    private final WebClient webClient;

    @Autowired
    public VaultServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getSecret(String path) {
        // KES-Vault 服务地址，当前为本地测试地址，后续可通过配置动态调整
        String vaultUrl = "http://localhost:8004/v1/secret/data/" + path;
        return webClient.get()
                        .uri(vaultUrl)
                        .retrieve()
                        .bodyToMono(String.class);
    }
}