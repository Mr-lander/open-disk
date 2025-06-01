package com.yyh.kesvault.controller;

import com.yyh.kesvault.service.KesVaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class KesVaultController {

    // 自动注入 Vault 中加载的属性
    @Value("${my.secret:未配置}")
    private String mySecret;
    private final KesVaultService kesVaultService;

    @Autowired
    public KesVaultController(KesVaultService KesVaultService) {
        this.kesVaultService = KesVaultService;
    }

    // 新增 POST 方法，支持通过请求体获取密钥
    @PostMapping("/secret/get")
    public ResponseEntity<String> getSecretByPost(@RequestBody Map<String, String> requestBody) {
        String path = requestBody.get("path");
        System.out.println("POST 获取密钥请求到达，路径: " + path);
        String secret = kesVaultService.getSecret(path);
        return ResponseEntity.ok(secret);
    }

    @PostMapping("/secret")
    public ResponseEntity<Void> writeSecret(@RequestBody Map<String, String> requestBody) {
        String path = requestBody.get("path");
        String encryptionKey = requestBody.get("encryptionKey");
        System.out.println("POST 请求到达，路径: " + path);
        System.out.println("写入的 encryptionKey: " + encryptionKey);
        kesVaultService.writeSecret(path, encryptionKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public String test(){
        return "hello hacker";
    }




}
