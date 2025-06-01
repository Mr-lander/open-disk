package com.yyh.file.repository;

import com.yyh.file.domain.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    File findByFileName(String fileName);

    File findByFileId(Long id);

    File findByFileOwnerAndFileName(Long userId, String objectKey);

    File findByFilePath(String filePath);


    /**
     * 找出所有 storageTier=HOT 且 (access_count < :threshold OR last_accessed < :cutoff) 的文件
     */
    @Query("""
      select f
      from File f
      where f.storageTier = 'HOT'
        and (f.access_count < :threshold 
             or f.last_accessed < :cutoff)
    """)
    List<File> findColdCandidates(
            @Param("threshold") int accessThreshold,
            @Param("cutoff") LocalDateTime lastAccessCutoff
    );

    @Query("select f from File f where f.fileOwner = :uid")
    Page<File> findByOwner(@Param("uid") Long userId, Pageable pageable);

}