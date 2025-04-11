package com.yyh.userApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * openfeign自定义的UserClient，这个启动类是不需要的，他不是一个服务
 *
 * */

@SpringBootApplication
public class userApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(userApiServiceApplication.class, args);
    }
}