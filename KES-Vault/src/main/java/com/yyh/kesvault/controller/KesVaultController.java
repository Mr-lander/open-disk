package com.yyh.kesvault.controller;

import com.yyh.kesvault.service.KesVaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kesvault")
public class KesVaultController {

    @Autowired
    private KesVaultService kesVaultService;

    /**
     * 接收明文数据密钥，加密后返回 Vault 中存储的密文
     */
    @PostMapping("/encrypt")
    public String encryptDataKey(@RequestBody String dataKey) {
        return kesVaultService.encryptDataKey(dataKey);
    }

    /**
     * 接收加密后的密文，解密后返回明文数据密钥
     */
    @PostMapping("/decrypt")
    public String decryptDataKey(@RequestBody String ciphertext) {
        return kesVaultService.decryptDataKey(ciphertext);
    }
}
