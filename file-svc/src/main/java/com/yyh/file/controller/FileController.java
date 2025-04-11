package com.yyh.file.controller;

// React Programing

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yyh.commonlib.utils.JwtUtils;
import com.yyh.file.service.FileService;
import com.yyh.file.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final MinioService minioService;

    public FileController(FileService fileService, MinioService minioService) {
        this.fileService = fileService;
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("file") FilePart file,
            @RequestPart("sessionId") String sessionId,
            @RequestPart("fileSize") String fileSizeStr) {
        System.out.println("收到上传请求");

        // 转换 fileSize 为 long
        long fileSize;
        try {
            fileSize = Long.parseLong(fileSizeStr);
        } catch (NumberFormatException e) {
            return Mono.just(ResponseEntity.badRequest().body("无效的文件大小"));
        }

        // 提取和验证 JWT
        String token = authHeader.replace("Bearer ", "").trim();
        DecodedJWT jwt = JwtUtils.Verify(token);
        if (jwt == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的JWT token"));
        }
        String userId = jwt.getClaim("x-user-id").asString();

        // 调用 FileService 存储文件
        return fileService.storeFile(file, userId, sessionId,fileSize)
                .map(storedPath -> ResponseEntity.ok("文件上传成功，存储路径：" + storedPath))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件上传失败：" + e.getMessage())));
    }

    @GetMapping
    public Mono<ResponseEntity<Object>> getAllFiles(@RequestHeader("Authorization") String authHeader) {
        // 提取和验证 JWT
        String token = authHeader.replace("Bearer ", "").trim();
        DecodedJWT jwt = JwtUtils.Verify(token);
        if (jwt == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的JWT token"));
        }
        String userId = jwt.getClaim("x-user-id").asString();

// 获取 MinIO 文件列表
        return minioService.getAllFilesMetadata(userId,76800)
                .collectList() // 将 Flux<String> 收集为 Mono<List<String>>
                .map(files -> ResponseEntity.ok((Object) files))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("获取文件失败：" + e.getMessage())));
    }
}




// Spring MVC
//import com.auth0.jwt.interfaces.DecodedJWT;
//import com.yyh.commonlib.utils.JwtUtils;
//import com.yyh.file.service.FileService;
//import com.yyh.file.service.MinioService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/files")
//public class FileController {
//
//    private final FileService fileService;
//    private final MinioService minioService;
//
//    public FileController(FileService fileService, MinioService minioService) {
//        this.fileService = fileService;
//        this.minioService = minioService;
//    }
//
//    @PostMapping("/upload")
//    public ResponseEntity<?> uploadFile(@RequestHeader("Authorization") String authHeader,
//                                        @RequestParam("file") MultipartFile file,
//                                        @RequestParam("sessionId") String sessionId) {
//        System.out.println("收到上传请求");
//
//        try {
//            // 假设 authHeader 格式为 "Bearer <token>"
//            String token = authHeader.replace("Bearer ", "").trim();
//            // 解密 JWT
//            DecodedJWT jwt = JwtUtils.Verify(token);
//            if (jwt == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的JWT token");
//            }
//            // 提取 x-user-id 信息
//            String userId = jwt.getClaim("x-user-id").asString();
//            // 调用 Service 存储文件，文件将存放到 data/{x-user-id} 目录下
//            String storedPath = fileService.storeFile(file, userId,"0");
//            return ResponseEntity.ok("文件上传成功，存储路径：" + storedPath);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("文件上传失败：" + e.getMessage());
//        }
//    }
//
//    @GetMapping
//    public ResponseEntity<?> getAllFiles(@RequestHeader("Authorization") String authHeader) {
//        try {
//            // 假设 authHeader 格式为 "Bearer <token>"
//            String token = authHeader.replace("Bearer ", "").trim();
//            // 解密 JWT
//            DecodedJWT jwt = JwtUtils.Verify(token);
//            if (jwt == null) {
//                return ResponseEntity.status(401).body("无效的JWT token");
//            }
//
//            // 提取 x-user-id 信息
//            String userId = jwt.getClaim("x-user-id").asString();
//
//            // 获取该用户在 MinIO 上的所有文件
//            List<String> files = minioService.getAllFilesInUserBucket(userId);
//
//            return ResponseEntity.ok(files);  // 返回文件列表
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("获取文件失败：" + e.getMessage());
//        }
//
//    }
//}
