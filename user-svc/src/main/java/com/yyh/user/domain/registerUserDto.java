package com.yyh.user.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class registerUserDto {
    @NotBlank
    private String name;

    private String password;
}
