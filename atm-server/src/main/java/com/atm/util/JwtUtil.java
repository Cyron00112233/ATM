package com.atm.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责签发和解析 Token
 */
@Slf4j
@Component
public class JwtUtil {

    /** 签名密钥（从 application.properties 的 atm.jwt.secret 读取） */
    @Value("${atm.jwt.secret}")
    private String secret;

    /** 有效期（毫秒，从 atm.jwt.expire 读取） */
    @Value("${atm.jwt.expire}")
    private Long expire;

    /** 把字符串密钥转成 jjwt 需要的 SecretKey */
    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Token
     *
     * @param userId   用户 id（放进标准字段 sub）
     * @param username 用户名（自定义字段）
     * @param role     角色（自定义字段）
     */
    public String createToken(Long userId,String username,String role){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(String.valueOf(userId))   //sub：标准字段，主体
                .claim("username",username)     //自定义字段
                .claim("role",role)             //自定义字段
                .issuedAt(now)                     //iat：签发时间
                .expiration(expiration)            //exp: 过期时间
                .signWith(getKey())                //签名（算法由密钥长度自动判断）
                .compact();
    }

    /**
     * 解析 Token，返回 Claims（验签失败或已过期会抛异常）
     */
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(getKey())           //用同一个密钥验签
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从Token 取用户 id*/
    public Long getUserId(String token){
        return Long.valueOf(parseToken(token).getSubject());
    }

    //从 Token 取用户名
    public String getUsername(String token){
        return parseToken(token).get("username",String.class);
    }

    // 从Token 取角色
    public String getRole(String token){
        return parseToken(token).get("role",String.class);
    }

    //有效期（秒）—— 返回前端，让它知道什么时候该重新登录
    public Long getExpireSeconds(){
        return expire / 1000;
    }
}
