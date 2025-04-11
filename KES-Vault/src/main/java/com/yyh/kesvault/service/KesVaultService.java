package com.yyh.kesvault.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class KesVaultService {

    @Autowired
    private VaultTemplate vaultTemplate;

    // 在 Vault 中配置好的 Transit 密钥名称
    private final String transitKeyName = "my-transit-key";

    /**
     * 使用 Vault Transit 引擎对传入的明文数据加密，返回加密后的密文。
     * @param dataKey 明文数据密钥（DEK）
     * @return 加密后的密文
     */
    public String encryptDataKey(String dataKey) {
        // 先将明文转换为 Base64 编码
        String base64Plaintext = Base64.getEncoder().encodeToString(dataKey.getBytes());
        Map<String, Object> request = new HashMap<>();
        request.put("plaintext", base64Plaintext);
        VaultResponse response = vaultTemplate.write("transit/encrypt/" + transitKeyName, request);
        if (response != null && response.getData() != null && response.getData().get("ciphertext") != null) {
            return response.getData().get("ciphertext").toString();
        }
        throw new RuntimeException("加密失败");
    }

    /**
     * 使用 Vault Transit 引擎对传入的密文进行解密，返回解密后的明文数据密钥。
     * @param ciphertext 加密后的密文
     * @return 解密后的明文数据密钥
     */
    public String decryptDataKey(String ciphertext) {
        Map<String, Object> request = new HashMap<>();
        request.put("ciphertext", ciphertext);
        VaultResponse response = vaultTemplate.write("transit/decrypt/" + transitKeyName, request);
        if (response != null && response.getData() != null && response.getData().get("plaintext") != null) {
            String base64Plaintext = response.getData().get("plaintext").toString();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Plaintext);
            return new String(decodedBytes);
        }
        throw new RuntimeException("解密失败");
    }
}
