package com.yyh.commonlib.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpRequest;

public class AutoContext {
    //简化获取
    public static String getRequestHeader(String key) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if(requestAttributes instanceof ServletRequestAttributes attributes){
            // 获取请求参数
            HttpServletRequest request = attributes.getRequest();
//            System.out.println("获取请求参数key"+key);
            return request.getHeader(key);

        }
        return null;
    }
    public static String getUserId() {
        return getRequestHeader(AuthConstant.AUTH_USER_ID);
    }

    public static String getUserName() {
        return getRequestHeader(AuthConstant.AUTH_USER_NAME);
    }

    public static String getUserRole() {
        return getRequestHeader(AuthConstant.AUTH_USER_ROLE);
    }

}
