package com.yyh.userApi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-svc",path = "/api/v1/users",url = "${yyh.user-endpoint}")
public interface UserClient {
    /*
    * 为什么这里需要指定添加GetMapping?
    * ans: 因为FeignClient默认是post请求，所以需要指定GetMapping
    * */
    @GetMapping("/all")
    public List<UserDTO> getUsers();

    @GetMapping
    public String getUserName(@RequestParam("id") String id);
}
