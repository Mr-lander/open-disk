package com.yyh.user.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class savedUserDto {
    @NotBlank
    private String id;
    @NotBlank
    private String name;

    private String role;

    private String password;
}
