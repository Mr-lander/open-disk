package com.yyh.uidGenerator.controller;

import cn.hutool.core.util.IdUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uids")
public class UidController {
    @GetMapping
    public long getUid() {
        return IdUtil.getSnowflake(1, 1).nextId();
    }
}
