package com.yyh.commonlib.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;


import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class HashUtils {
    private static final String SECRET = "123456";

    public static String HashString(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            // Apache Commons Codec 的方法：
            return Hex.encodeHexString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 使用 AES/ECB/PKCS5Padding 对给定文本进行加密，返回 URL-safe Base64。
     */
    public static String encryptWithKey(String value, String hexKey) {
        try {
            byte[] keyBytes = Hex.decodeHex(hexKey.toCharArray());
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error encrypting: {}", e.getMessage());
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 使用 AES/ECB/PKCS5Padding 对加密文本进行解密。
     */
    public static String decryptWithKey(String cipherText, String hexKey) {
        try {
            byte[] keyBytes = Hex.decodeHex(hexKey.toCharArray());
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getUrlDecoder().decode(cipherText);
            byte[] original = cipher.doFinal(decoded);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting: {}", e.getMessage());
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 计算文件的 SHA-256 哈希值
     * @param file 上传的 MultipartFile
     * @return 文件的 SHA-256 哈希字符串
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String calculateSHA256(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        // 将字节数组转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 计算 FilePart 的 SHA-256 哈希值
     */
    public static Mono<String> calculateSHA256_FilePart(FilePart filePart) {
        try {
            return filePart.content()
                    .reduce(MessageDigest.getInstance("SHA-256"), (digest, dataBuffer) -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        digest.update(bytes);
                        return digest;
                    })
                    .map(digest -> bytesToHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            return Mono.error(new RuntimeException("无法计算 SHA-256 哈希值", e));
        }
    }

    /**
     * 新增方法：根据本地文件（相对路径）计算 SHA-256 哈希值
     * @param filePath 本地文件的相对路径
     * @return 文件的 SHA-256 哈希字符串
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String calculateSHA256_LocalPath(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(digest.digest());
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    /**
     * 生成一个随机的加密密钥，以十六进制字符串返回。
     * 如果你的加密算法要求 AES-128，则生成 16 字节（128 位）的密钥，
     * 如需 AES-256 则将数组大小改为 32 字节。
     *
     * @return 随机生成的密钥的十六进制表示
     */
    public static String generateRandomEncryptionKey() {
        // 生成一个 16 字节（128 位）的随机密钥
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[16];
        secureRandom.nextBytes(keyBytes);
        // 用 Apache Commons Codec 将二进制密钥编码成十六进制字符串
        return Hex.encodeHexString(keyBytes);
    }
}
