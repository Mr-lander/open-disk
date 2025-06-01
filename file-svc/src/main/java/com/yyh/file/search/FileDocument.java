package com.yyh.file.search;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

// com.yyh.file.search.FileDocument.java
@Document(indexName = "files")
@Setting(settingPath = "es/settings.json")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDocument {
  @Id
  private Long fileId;

  private Long userId;
  @Field(type = FieldType.Text,
          analyzer = "ngram_analyzer",
          searchAnalyzer = "standard")
  private String fileName;
  private String storageTier;

  @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
  private Instant uploadTime;
}
