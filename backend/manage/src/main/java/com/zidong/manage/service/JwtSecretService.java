package com.zidong.manage.service;

import com.zidong.dao.entity.SysSecret;
import com.zidong.dao.mapper.SysSecretMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/** JWT 签名密钥存取 */
@Service
@RequiredArgsConstructor
public class JwtSecretService {

    /** 单行表固定主键 */
    private static final long SECRET_ID = 1L;

    /** HMAC-SHA256 要求密钥不短于 32 字节，这里生成 32 字节随机数（Base64 后 43 字符） */
    private static final int SECRET_BYTE_LENGTH = 32;

    private static final Logger log = LoggerFactory.getLogger(JwtSecretService.class);

    private final SysSecretMapper secretMapper;

    /** 启动时加载一次后不再变化 */
    private String secret;

    @PostConstruct
    void loadOrCreate() {
        SysSecret stored = secretMapper.selectById(SECRET_ID);
        if (stored != null && stored.getSecret() != null && !stored.getSecret().isBlank()) {
            secret = stored.getSecret();
            log.info("已从 sys_secret 加载 JWT 签名密钥");
            return;
        }

        String generated = generate();
        LocalDateTime now = LocalDateTime.now();
        secretMapper.insert(SysSecret.builder()
                .id(SECRET_ID)
                .secret(generated)
                .createdAt(now)
                .updatedAt(now)
                .build());
        secret = generated;
        log.info("sys_secret 无数据，已生成 JWT 签名密钥并写入（{} 字符）", generated.length());
    }

    /** 密钥字节 */
    public byte[] getSecretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private static String generate() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
