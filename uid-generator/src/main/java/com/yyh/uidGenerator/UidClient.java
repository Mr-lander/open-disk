package com.yyh.uidGenerator;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "uid-generator",path = "/api/v1/uids",url = "${open-disk.uid-endpoint}")
public interface UidClient {

    @GetMapping("")
    public long getUid();

}
