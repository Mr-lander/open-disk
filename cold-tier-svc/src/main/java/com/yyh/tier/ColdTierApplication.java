package com.yyh.tier;

import com.yyh.file.service.CephService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hello world!
 *
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.yyh.tier",         // your scheduler, config, etc.
                "com.yyh.file.config" ,      // if you reuse CephService, MinioService, etc.
                "com.yyh.file.socket",
                "com.yyh.file.client",
                "com.yyh.file.search"
        }
)
@EnableJpaRepositories(basePackages = "com.yyh.file.repository")
@EntityScan(basePackages = "com.yyh.file.domain")
@EnableElasticsearchRepositories(basePackages = "com.yyh.file.search")
@EnableScheduling
public class ColdTierApplication
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        SpringApplication.run(ColdTierApplication.class,args);
    }

    //手动注册依赖
    @Bean
    public CephService cephService(
            @Qualifier("coldMinioClient") io.minio.MinioClient cephClient,
            @Qualifier("hotMinioClient")  io.minio.MinioClient hotMinioClient) {
        return new CephService(cephClient, hotMinioClient);
    }
}
