package com.yyh.user.service.impl;

import com.yyh.commonlib.utils.HashUtils;
import com.yyh.commonlib.utils.JwtUtils;
import com.yyh.user.client.VaultServiceClient;
import com.yyh.user.client.UidServiceClient;
import com.yyh.user.domain.User;
import com.yyh.user.domain.registerUserDto;
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

    @Autowired
    private VaultServiceClient vaultServiceClient;

    @Autowired
    private UidServiceClient uidServiceClient;



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
        long user_id = uidServiceClient.getUid("");
        user.setId(user_id);
        user.setName(userdto.getName());
        user.setRole("ADMIN");
        String passwordHash = HashUtils.HashString(userdto.getPassword());
        user.setPassword_hash(passwordHash);


        String encryptionKey = HashUtils.generateRandomEncryptionKey();
        vaultServiceClient.writeSecret("user-keys/" + user_id, encryptionKey);

        // 使用vault存储的密钥加密这个加密哈希
        assert passwordHash != null;
        String encryptedHash = HashUtils.encryptWithKey(passwordHash, encryptionKey); // 自定义加密方法
        user.setPassword_hash(encryptedHash);

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
                .filter(user -> {
                    // 先哈希
                    String passwordHash = HashUtils.HashString(password);
                    // 从 Vault 获取密钥
                    String key = vaultServiceClient.getSecret("user-keys/" + user.getId());
//                    String key = "";
                    // 使用vault提取出来的密钥加密
                    String encryptedHash_request = HashUtils.encryptWithKey(passwordHash, key);
                    // 比较密码
                    return Objects.equals(encryptedHash_request, user.getPassword_hash());
                })
                .map(user -> {
                    //ans: var是自动推断类型，不用写类型，但是必须赋值
                    var loginResponse = this.modelMapper.map(user, LoginResponseDto.class);

                    loginResponse.setToken(JwtUtils.SignToken(String.valueOf(user.getId()), user.getName(), user.getRole()));

                    return loginResponse;
                });
    }

    @Override
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

}
