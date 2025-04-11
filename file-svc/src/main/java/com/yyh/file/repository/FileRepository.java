package com.yyh.file.repository;

import com.yyh.file.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
    File findByFileName(String fileName);

    File findByFileId(Long id);
}