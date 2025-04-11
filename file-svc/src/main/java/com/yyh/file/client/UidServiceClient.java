package com.yyh.file.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UidServiceClient {

    private final WebClient webClient;

    @Autowired
    public UidServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Long> fetchUid() {
        // 请确保 uid-svc 的地址正确，建议通过配置或服务发现管理地址
        String uidServiceUrl = "http://localhost:8003/api/v1/uids";
        return webClient.get()
                        .uri(uidServiceUrl)
                        .retrieve()
                        .bodyToMono(Long.class);
    }
}
