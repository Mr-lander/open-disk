package com.yyh.download.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yyh.commonlib.utils.JwtUtils;
import com.yyh.download.service.DownloadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

// File: DownloadController.java
@RestController
@RequestMapping("/api/v1/download")
public class DownloadController {
    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /** 预览（inline） **/
    @GetMapping("/preview")
    public Mono<ResponseEntity<String>> preview(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam("cipher") String cipherKey) {
        System.out.println("cipher get preview");
        String token = authHeader.replace("Bearer ", "").trim();
        DecodedJWT jwt = JwtUtils.Verify(token);
        if (jwt == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的JWT token"));
        }
        String userId = jwt.getClaim("x-user-id").asString();

        return downloadService.generateUrl(Long.valueOf(userId), cipherKey, false)
                .map(url -> ResponseEntity.ok(url))
                .onErrorResume(e ->
                  Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage()))
                );
    }

    /** 下载（attachment） **/
    @GetMapping("/file")
    public Mono<ResponseEntity<String>> download(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam("cipher") String cipherKey) {

        String token = authHeader.replace("Bearer ", "").trim();
        DecodedJWT jwt = JwtUtils.Verify(token);
        if (jwt == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的JWT token"));
        }
        String userId = jwt.getClaim("x-user-id").asString();

        return downloadService.generateUrl(Long.valueOf(userId), cipherKey, true)
                .map(url -> ResponseEntity.ok(url))
                .onErrorResume(e ->
                  Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage()))
                );
    }
}
