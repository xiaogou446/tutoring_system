package com.lin.webtemplate.service.service;

import java.time.Instant;
import java.util.Date;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.lin.webtemplate.infrastructure.dataobject.AdminUserDO;
import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.model.AdminSessionModel;

/**
 * 功能：管理员 JWT 签发与校验。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Service
public class AdminJwtService {

    private static final String ISSUER = "tutoring-system-admin";

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    public AdminSessionModel issue(AdminUserDO adminUserDO) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(adminAuthJwtProperties.getExpireSeconds());
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(adminUserDO.getId()))
                .withClaim("username", adminUserDO.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expireAt))
                .sign(algorithm());

        AdminSessionModel sessionModel = new AdminSessionModel();
        sessionModel.setUserId(adminUserDO.getId());
        sessionModel.setUsername(adminUserDO.getUsername());
        sessionModel.setIssuedAtEpochSeconds(now.getEpochSecond());
        sessionModel.setExpireAtEpochSeconds(expireAt.getEpochSecond());
        sessionModel.setToken(token);
        return sessionModel;
    }

    public AdminSessionModel verify(String token) {
        JWTVerifier verifier = JWT.require(algorithm())
                .withIssuer(ISSUER)
                .build();
        DecodedJWT jwt = verifier.verify(token);

        AdminSessionModel sessionModel = new AdminSessionModel();
        sessionModel.setUserId(Long.parseLong(jwt.getSubject()));
        sessionModel.setUsername(jwt.getClaim("username").asString());
        sessionModel.setIssuedAtEpochSeconds(jwt.getIssuedAt().toInstant().getEpochSecond());
        sessionModel.setExpireAtEpochSeconds(jwt.getExpiresAt().toInstant().getEpochSecond());
        sessionModel.setToken(token);
        return sessionModel;
    }

    public boolean isExpired(Throwable throwable) {
        return throwable instanceof TokenExpiredException;
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(adminAuthJwtProperties.getSecret());
    }
}
