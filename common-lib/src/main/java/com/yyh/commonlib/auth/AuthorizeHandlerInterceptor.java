package com.yyh.commonlib.auth;

import com.yyh.commonlib.exception.InvalidRoleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

public class AuthorizeHandlerInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        var method = handlerMethod.getMethod();
        var authorizeAnnotation = method.getAnnotation(Authorize.class);
        if (authorizeAnnotation == null) {
            return true;
        }
        var expectedRoles = authorizeAnnotation.value();
        var currentRole = request.getHeader(AuthConstant.AUTH_USER_ROLE);
        if (expectedRoles != null && !Arrays.asList(expectedRoles).contains(currentRole)) {
            throw new InvalidRoleException(String.format("expected role: %s, current role: %s", String.join(",", expectedRoles), currentRole));
        }
        return true;
    }
}
