package com.yyh.userApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/*
* 由于 @Builder 的存在，原本由 @Data 自动生成的无参构造方法会被覆盖。
* 因此，当你尝试实例化 UserDTO 而不使用 Builder 时（例如：通过反射或者一些框架需要无参构造方法的情况下），你会发现缺少无参构造方法。
* */
public class UserDTO {
    private String id;

    private String name;

    private String role;
}