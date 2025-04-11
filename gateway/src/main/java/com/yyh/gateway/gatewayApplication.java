package com.yyh.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class gatewayApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(gatewayApplication.class, args);
    }
}
