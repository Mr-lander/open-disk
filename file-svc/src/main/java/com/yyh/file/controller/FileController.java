package com.yyh.file.controller;

// React Programing

import com.auth0.jwt.interfaces.DecodedJWT;
import com.yyh.commonlib.utils.HashUtils;
import com.yyh.commonlib.utils.JwtUtils;
import com.yyh.file.client.KesVaultWebClient;
import com.yyh.file.domain.File;
import com.yyh.file.domain.FileMetadataDto;
import com.yyh.file.repository.FileRepository;
import com.yyh.file.search.FileDocument;
import com.yyh.file.search.FileSearchRepository;
import com.yyh.file.service.FileService;
import com.yyh.file.service.MinioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final MinioService minioService;

    private final KesVaultWebClient vaultClient;

    private final FileSearchRepository searchRepo;

    private final FileRepository fileRepo;



    public FileController(FileService fileService, MinioService minioService, KesVaultWebClient vaultClient, FileSearchRepository searchRepo, FileRepository fileRepo) {
        this.fileService = fileService;
        this.minioService = minioService;
        this.vaultClient = vaultClient;
        this.searchRepo = searchRepo;
        this.fileRepo = fileRepo;
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
    public Mono<List<FileMetadataDto>> listFiles(
            @RequestHeader("Authorization") String authHeader) {
        System.out.println("所有文件列表");
        // 1. JWT 验证
        String userId = JwtUtils.Verify(authHeader.replace("Bearer ", "")).getClaim("x-user-id").asString();
        if (userId == null) {
            return Mono.error(new RuntimeException("无效的 JWT"));
        }
        return Mono.fromCallable(() -> {
                    List<File> files = fileRepo.findByOwner(Long.valueOf(userId), Pageable.unpaged()).getContent();
                    return files.stream().map(f -> new FileMetadataDto(
                            f.getFileName(),
                            Long.parseLong(f.getFile_size()),
                            f.getContent_type(),
                            f.getUpload_time().atOffset(ZoneOffset.UTC),
                            null            // cipherKey 稍后加
                    )).toList();
                }).flatMapMany(Flux::fromIterable)
                .zipWith(vaultClient.getEncryptionKey(Long.valueOf(userId)).repeat())
                .map(tuple -> {
                    FileMetadataDto dto = tuple.getT1();
                    String key = tuple.getT2();
                    dto.setCipherKey(HashUtils.encryptWithKey(dto.getFileName(), key));
                    return dto;
                })
                .collectList();
    }

    @GetMapping("/search")
    public Mono<List<FileMetadataDto>> searchFiles(
            @RequestHeader("Authorization") String auth,
            @RequestParam("query") String query) {

        Long userId = Long.valueOf(
                JwtUtils.Verify(auth.replace("Bearer ","")).getClaim("x-user-id").asString());

        // 1. 在 ES 里搜索文件名
        Page<FileDocument> docs = searchRepo
                .findByUserIdAndFileNameContaining(userId, query, Pageable.unpaged());

        // 2. 按照 listFiles 逻辑生成 DTO + cipherKey
        return Flux.fromIterable(docs.getContent())
                .flatMap(doc -> Mono.just(doc.getFileName())
                        .zipWith(vaultClient.getEncryptionKey(userId)))
                .map(tuple -> {
                    String fname = tuple.getT1();
                    String key   = tuple.getT2();
                    File f = fileRepo.findByFileOwnerAndFileName(userId, fname);
                    return new FileMetadataDto(
                            fname,
                            Long.parseLong(f.getFile_size()),
                            f.getContent_type(),
                            f.getUpload_time().atOffset(ZoneOffset.UTC),
                            HashUtils.encryptWithKey(fname, key));
                })
                .collectList();
    }

    /** 删除文件 **/
    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("cipher") String cipherKey) {

        // 1) 验证 JWT、拿 userId
        Long userId = Long.valueOf(
                JwtUtils.Verify(authHeader.replace("Bearer ","")).getClaim("x-user-id").asString());


        // 2) 调 FileService.deleteFile
        return fileService.deleteFile(userId, cipherKey)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                );
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
