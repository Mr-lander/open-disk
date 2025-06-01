package com.yyh.kesvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;

import java.util.Map;

@Service
public class KesVaultService {
    private final VaultTemplate vaultTemplate;

    // 从配置文件中获取 Vault 的基本 URL（例如：http://localhost:8200）
    @Value("${Vault.url}")
    private String vaultUrl;

    public KesVaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    /**
     * 从 Vault 中读取密钥
     * @param path 密钥的路径，例如 "user-keys/1911036456084836352"
     * @return 密钥值（encryptionKey），如果不存在则返回 null
     */
    public String getSecret(String path) {
        VaultResponse response = vaultTemplate.read("secret/data/" + path);
        if (response != null && response.getData() != null) {
            Map<String, Object> outerData = response.getData();
            if (outerData.containsKey("data")) {
                Map<String, Object> innerData = (Map<String, Object>) outerData.get("data");
                if (innerData != null && innerData.containsKey("encryptionKey")) {
                    return (String) innerData.get("encryptionKey");
                }
            }
        }
        return null;
    }

    /**
     * 向 Vault 中写入密钥
     * @param path 存储密钥的路径，例如 "user-keys/1911036456084836352"
     * @param encryptionKey 要存储的密钥值
     */
    public void writeSecret(String path, String encryptionKey) {
// Step 1: Create the inner data map with your secret
        Map<String, Object> secretData = new HashMap<>();
        secretData.put("encryptionKey", encryptionKey);

        // Step 2: Wrap it in a "data" field as Vault expects
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("data", secretData);

        // Step 3: Write to Vault with the correct path and data
        vaultTemplate.write("secret/data/" + path, requestData);
    }
}
