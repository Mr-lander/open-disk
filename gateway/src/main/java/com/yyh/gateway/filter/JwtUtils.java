package com.yyh.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
public class JwtUtils {
    private static final String SECRET = "123456";

    public static String SignToken(String userId, String userName, String userRole) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET.getBytes(Charset.defaultCharset()));
            return JWT.create()
                    .withClaim("x-user-id", userId)
                    .withClaim("x-user-name", userName)
                    .withClaim("x-user-role", userRole)
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            log.error(exception.getMessage());
        }
        return null;
    }

    public static DecodedJWT Verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET.getBytes(Charset.defaultCharset()));
            JWTVerifier verifier = JWT.require(algorithm)
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            log.error(exception.getMessage());
        }
        return null;
    }
}
