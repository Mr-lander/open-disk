package com.yyh.tier.service;

import com.yyh.file.client.UidServiceClient;
import com.yyh.file.domain.File;
import com.yyh.file.repository.FileRepository;
import com.yyh.file.service.CephService;
import com.yyh.file.service.MinioService;
import com.yyh.file.socket.WebSocketSessionHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ColdTierScheduler {

    private final FileRepository fileRepo;
    private final CephService cephService;


    @Value("5")
    private int accessThreshold;
    @Value("30")
    private int daysThreshold;

    /** 
     * 定时迁移：先查出所有 HOT 且符合“冷候选”条件的文件 
     */
    @Scheduled(cron = "${tier.cron}")
    public void migrateColdCandidates() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysThreshold);
        // 自定义查询：storageTier='HOT' AND (access_count< X OR last_accessed< cutoff)
        List<File> candidates = fileRepo.findColdCandidates(accessThreshold, cutoff);
        if(candidates.size()>0){
            log.info("发现 {} 个冷候选，开始迁移…", candidates.size());
            System.out.println(candidates);
        }
        for (File f : candidates) {
            String bucket = String.valueOf(f.getFileOwner());
            String object = f.getFileName();
            cephService.migrateObject(bucket, object)
                .doOnError(err -> log.error("迁移失败 {}:{}", bucket, object, err))
                .doOnSuccess(v -> {
                  f.setStorageTier("COLD");
                  fileRepo.save(f);
                  log.info("已迁移并标记冷端：{}/{}", bucket, object);
                })
                .subscribe();
        }
    }
}
