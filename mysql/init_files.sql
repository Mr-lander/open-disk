CREATE TABLE IF NOT EXISTS files (
                                     file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     file_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_size VARCHAR(50),
    content_type VARCHAR(100),
    file_owner VARCHAR(100),
    upload_time DATETIME,
    file_hash VARCHAR(255),
    last_accessed DATETIME,
    access_count VARCHAR(50),
    storage_tier VARCHAR(50)
    );
