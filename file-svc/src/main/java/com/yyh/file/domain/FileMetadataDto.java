package com.yyh.file.domain;

import java.time.OffsetDateTime;

public class FileMetadataDto {
    private String fileName;
    private long fileSize;
    private String contentType;
    private OffsetDateTime lastModified;
    private String fileUrl;

    // Getter & Setter
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public OffsetDateTime getLastModified() {
        return lastModified;
    }
    public void setLastModified(OffsetDateTime lastModified) {
        this.lastModified = lastModified;
    }
    public String getFileUrl() {
        return fileUrl;
    }
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
