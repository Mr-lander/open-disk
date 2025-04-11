package com.yyh.user.controller;

import com.yyh.commonlib.auth.AuthConstant;
import com.yyh.commonlib.auth.AutoContext;
import com.yyh.user.domain.User;
import com.yyh.user.domain.registerUserDto;
import com.yyh.user.domain.savedUserDto;
import com.yyh.user.service.UserService;
import com.yyh.userApi.LoginRequestDto;
import com.yyh.userApi.LoginResponseDto;
import com.yyh.userApi.UserDTO;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    @PostMapping
    public void createUser(@Validated @RequestBody registerUserDto userdto){
        userService.save(userdto);
    }

    @DeleteMapping
    public void deleteUser(@NotBlank @RequestBody String Id){
        userService.delete(Id);
    }

    @GetMapping("/all")
    public List<UserDTO> getAllUsers(@RequestHeader(AuthConstant.AUTH_USER_ID) @Nullable String id){
        System.out.println("Received userId in controller: " + id);
        return userService.findAll();
    }


    @PostMapping("/login")
    // [POST] http://{host}:{port}/api/v1/users/login
    public ResponseEntity<?> loginUser(@RequestBody @Valid LoginRequestDto user) {

        Optional<LoginResponseDto> response = this.userService.login(user.getUsername(), user.getPassword());
        System.out.println("用户登录请求");
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping
//    @RequestHeader(AuthConstant.AUTH_USER_ID) @Nullable ——> 访问自己
//    提供给别人访问
    public String getUserName(@RequestParam("id") @NotNull String id){
        //返回了optional，一行代码取出User.getname
        Optional<User> user = this.userService.getUserById(id);
        return user.map(User::getName).orElse(null);
    }

//    @GetMapping("getrank")
//    public List<UserDTO> getUserRank(@RequestHeader(AuthConstant.AUTH_USER_ID) @Nullable String id){
//        return this.userService.User
//    }

}

