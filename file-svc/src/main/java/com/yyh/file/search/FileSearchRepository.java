package com.yyh.file.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// com.yyh.file.search.FileSearchRepository.java
public interface FileSearchRepository
    extends ElasticsearchRepository<FileDocument, Long> {

  // 基于文件名模糊 / 前缀 / 分词搜索
  Page<FileDocument> findByUserIdAndFileNameContaining(
      Long userId, String fileName, Pageable page);
}
