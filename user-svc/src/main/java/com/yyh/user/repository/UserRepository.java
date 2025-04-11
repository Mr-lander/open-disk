package com.yyh.user.repository;

import com.yyh.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    //完善这个JPA的userreository
    Optional<User> findByName(String name); // TODO:为什么要单独写？

    Optional<User> findById(String id);
}
