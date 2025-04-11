package com.yyh.commonlib.configuration;

import com.yyh.commonlib.auth.AuthorizeHandlerInterceptor;
import com.yyh.commonlib.auth.OpenFeignRequestInterceptor;
import com.yyh.commonlib.exception.GlobalExceptionHandler;
import feign.RequestInterceptor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
@Import({GlobalExceptionHandler.class})
public class CommonConfig implements WebMvcConfigurer {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    //如果要用interceptor，就用我的这个
    public RequestInterceptor getRequestInterceptor() {
        return new OpenFeignRequestInterceptor();
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthorizeHandlerInterceptor()).excludePathPatterns(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
        );
    }
}
