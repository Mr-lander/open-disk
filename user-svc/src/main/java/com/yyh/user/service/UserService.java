package com.yyh.user.service;


import com.yyh.user.domain.User;
import com.yyh.user.domain.registerUserDto;
import com.yyh.user.domain.savedUserDto;
import com.yyh.userApi.LoginResponseDto;
import com.yyh.userApi.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

    public Optional<User> findByName(String name);
    //实现删除、save，findAll的方法
    public void delete(String id);
    public User save(registerUserDto registerUserDto);
    public List<UserDTO> findAll();

    public Optional<LoginResponseDto> login(String username, String password);

    //根据id返回user
    public Optional<User> getUserById(String id);

}
