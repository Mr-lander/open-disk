package com.yyh.commonlib.auth;

import feign.RequestInterceptor;
import io.micrometer.common.util.StringUtils;

public class OpenFeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(feign.RequestTemplate template) {
        String userId = AutoContext.getUserId();
        String userName = AutoContext.getUserName();
        String userRole = AutoContext.getUserRole();

//        System.out.println("Intercepted userId: " + userId);
//        System.out.println("Intercepted userName: " + userName);
//        System.out.println("Intercepted userRole: " + userRole);

        if (!StringUtils.isBlank(userId) && !StringUtils.isBlank(userName) && !StringUtils.isBlank(userRole)) {
            template.header(AuthConstant.AUTH_USER_ID, userId);
            template.header(AuthConstant.AUTH_USER_NAME, userName);
            template.header(AuthConstant.AUTH_USER_ROLE, userRole);
            System.out.println("Template with headers: " + template);
        }
    }
}
