package com.zidong.manage.security;

import com.zidong.manage.service.JwtSecretService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/** JWT 签发与校验 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_SESSION_UUID = "session_uuid";

    private final JwtSecretService secretService;

    /** 签名密钥 */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secretService.getSecretBytes());
    }

    /** 签发会话令牌 */
    public String generateSessionToken(String sessionUuid) {
        return Jwts.builder()
                .claim(CLAIM_SESSION_UUID, sessionUuid)
                .issuedAt(new Date())
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** 解析并校验令牌 */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getSessionUuidClaim(Claims claims) {
        return claims.get(CLAIM_SESSION_UUID, String.class);
    }
}
