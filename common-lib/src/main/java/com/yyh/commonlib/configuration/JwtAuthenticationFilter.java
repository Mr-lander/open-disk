package com.yyh.commonlib.configuration;

import com.yyh.commonlib.utils.JwtUtils;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        //排除登录接口
        if (path.equals("/api/v1/users/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 对需要验证的 URL 进行过滤（例如 /api/**）
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("缺少JWT token");
            return;
        }

        String token = authHeader.replace("Bearer ", "").trim();
        DecodedJWT jwt = JwtUtils.Verify(token);
        if (jwt == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("无效或已过期的JWT token");
            return;
        }
        
        // JWT验证通过，可以将用户信息存入请求上下文，供后续使用
        // 例如：request.setAttribute("x-user-id", jwt.getClaim("x-user-id").asString());

        filterChain.doFilter(request, response);
    }
}
