package com.yyh.user.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UidServiceClient {

    private final RestTemplate restTemplate;

    @Autowired
    public UidServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public long getUid(String path) {
        // KES-Vault 服务地址，当前为本地测试地址，后续可通过配置动态调整
        String vaultUrl = "http://localhost:8003/api/v1/uids";
        return restTemplate.getForObject(vaultUrl, long.class);
    }
}
