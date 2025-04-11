package com.yyh.user.service.impl;

import com.yyh.commonlib.utils.HashUtils;
import com.yyh.commonlib.utils.JwtUtils;
import com.yyh.user.domain.User;
import com.yyh.user.domain.registerUserDto;
import com.yyh.user.domain.savedUserDto;
import com.yyh.user.repository.UserRepository;
import com.yyh.user.service.UserService;
import com.yyh.userApi.LoginResponseDto;
import com.yyh.userApi.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Optional<User> findByName (String name) {
        return userRepository.findByName(name);
    }

    @Override
    public void delete(String id) {
        //检查是否能够删除
        userRepository.deleteById(id);
    }

    @Override
    public User save(registerUserDto userdto) {
        User user = new User();
        user.setName(userdto.getName());
        user.setRole("ADMIN");
        user.setPassword_hash(HashUtils.HashString(userdto.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public List<UserDTO> findAll() {

        //modelMapper转化repofindall的内容为userDTO
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))//user->的语法是函数式编程，lambda表达式，user来自userRepository.findAll(),stream()用于遍历
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LoginResponseDto> login(String username, String password) {
//        String hash = HashUtils.HashString(password);
        return userRepository.findByName(username)
                .filter(user -> Objects.equals(user.getPassword_hash(), HashUtils.HashString(password)))
                .map(user -> {
                    //ans: var是自动推断类型，不用写类型，但是必须赋值
                    var loginResponse = this.modelMapper.map(user, LoginResponseDto.class);

                    loginResponse.setToken(JwtUtils.SignToken(user.getId(), user.getName(), user.getRole()));

                    return loginResponse;
                });
    }

    @Override
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

}
