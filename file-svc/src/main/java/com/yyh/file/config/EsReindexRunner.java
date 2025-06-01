package com.yyh.file.config;

import com.yyh.file.domain.File;
import com.yyh.file.repository.FileRepository;
import com.yyh.file.search.FileDocument;
import com.yyh.file.search.FileSearchRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class EsReindexRunner implements ApplicationRunner {

  private final FileRepository fileRepo;
  private final FileSearchRepository searchRepo;

  public EsReindexRunner(FileRepository fileRepo,
                         FileSearchRepository searchRepo) {
    this.fileRepo   = fileRepo;
    this.searchRepo = searchRepo;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<File> allFiles = fileRepo.findAll();

    List<FileDocument> docs = allFiles.stream()
            .map(f -> {
              LocalDateTime ldt = f.getUpload_time();     // 或 f.getUploadTime()
              Instant uploaded = ldt.atOffset(ZoneOffset.UTC)
                      .toInstant();
              return new FileDocument(
                      f.getFileId(),
                      f.getFileOwner(),
                      f.getFileName(),
                      null,
                      uploaded
              );
            })
            .collect(Collectors.toList());

    searchRepo.saveAll(docs);
    System.out.println("▶️ Reindexed " + docs.size() + " docs into Elasticsearch");
  }
}