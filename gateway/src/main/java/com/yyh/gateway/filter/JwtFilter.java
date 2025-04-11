package com.yyh.gateway.filter;




import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtFilter extends AbstractGatewayFilterFactory {
    @Override
    public GatewayFilter apply(Object config) {
        // Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ4LXVzZXItaWQiOiIxIiwieC11c2VyLW5hbWUiOiJ0ZXN0LXRyYWRlciIsIngtdXNlci1yb2xlIjoiVFJBREVSIn0.Jr69ObT2WJWBbZuRu8zLuPgsIe4nrWbZzfHjAZUL65c
        return (exchange, chain) -> {
            var headers = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (headers == null || headers.isEmpty()) {
                return OnUnauthorized(exchange);
            }
            String header = headers.get(0);
            String token = header.substring("Bearer ".length());
            var decodedJwt = JwtUtils.Verify(token);
            if (decodedJwt == null) {
                return OnUnauthorized(exchange);
            }
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                    .header("x-user-id", decodedJwt.getClaim("x-user-id").asString())
                    .header("x-user-name", decodedJwt.getClaim("x-user-name").asString())
                    .header("x-user-role", decodedJwt.getClaim("x-user-role").asString());
            return chain.filter(exchange.mutate().request(builder.build()).build());
        };
    }

    private Mono<Void> OnUnauthorized(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    public static class Config {
        // 配置属性，如果有的话可以添加在这里
    }
}
