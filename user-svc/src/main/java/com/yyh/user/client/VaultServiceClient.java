package com.yyh.user.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class VaultServiceClient {

    private final RestTemplate restTemplate;

    // 从配置文件中读取 kes‑vault 微服务的 URL
    @Value("${kesvault.url}")
    private String vaultServiceUrl;

    @Autowired
    public VaultServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 使用 POST 请求获取密钥，与 writeSecret 的封装逻辑一致
     * @param path 密钥的相对路径，如 "user-keys/1911036456084836352"
     * @return 密钥值
     */
    public String getSecret(String path) {
        String url = vaultServiceUrl + "/secret/get";   // /api/vault + /secret/get
        Map<String, String> payload = new HashMap<>();
        payload.put("path", path);
        return restTemplate.postForObject(url, payload, String.class);
    }

    /**
     * 远程调用 kes‑vault 微服务写入 secret。
     * 例如，调用 POST {vaultServiceUrl}/secret/{path}
     * 请求体格式：{"encryptionKey":"加密密钥"}
     *
     * @param path          存储密钥的相对路径，如 "user-keys/1911036456084836352"
     * @param encryptionKey 要写入的加密密钥
     */
    public void writeSecret(String path, String encryptionKey) {
        String url = vaultServiceUrl + "/secret";
        System.out.println("url: "+url);
        Map<String, String> payload = Map.of(
                "path", path,
                "encryptionKey", encryptionKey
        );
        ResponseEntity<Void> resp = restTemplate
                .postForEntity(url, payload, Void.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Vault WRITE Secret 失败: " + resp.getStatusCode());
        }
    }
}
