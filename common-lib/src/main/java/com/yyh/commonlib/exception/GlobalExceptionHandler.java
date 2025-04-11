package com.yyh.commonlib.exception;

import com.yyh.commonlib.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理MethodArgumentNotValidException错误
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletResponse response) {
        /*
        *  为什么要初始化未ArrayList？
        * ans：1.因为List是接口，2.List接口没有add方法，3.ArrayList实现了List接口，4.ArrayList实现了add方法
        * */
        List<String> errors = new ArrayList<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ":" + error.getDefaultMessage());
        }
        response.setStatus(400);
        System.out.println("MethodArgumentNotValidException测试输出："+errors.toString());
        return Result.error(errors.toString());
    }

//    @ExceptionHandler(InvalidRoleException.class)
//    public Result handleInvalidRoleException(InvalidRoleException e, HttpServletResponse response) {
//        response.setStatus(403);
//        return Result.error(e.getMessage());
//    }

    @ExceptionHandler({InvalidRoleException.class})
    public ResponseEntity<?> handleInvalidRoleException(InvalidRoleException ex) {
        System.out.println("角色不正确: "+ ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}
