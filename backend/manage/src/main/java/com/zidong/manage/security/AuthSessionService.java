package com.zidong.manage.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.SysLoginSession;
import com.zidong.dao.mapper.SysLoginSessionMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** 登录会话管理 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    /** 登录态持久化（支持分布式） */
    private final SysLoginSessionMapper sysLoginSessionMapper;

    /** 会话滑动过期时间：30 分钟 */
    private static final long SESSION_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    /** 剩余有效期不足该阈值（秒）时才续期，避免每次请求都写库 */
    private static final long RENEW_THRESHOLD_SECONDS = 600L;

    /** 过期会话定时清理间隔：5 分钟（须为编译期常量，@Scheduled 注解属性要求） */
    private static final long SESSION_CLEANUP_INTERVAL_MILLIS = 5 * 60 * 1000L;

    private static final Logger log = LoggerFactory.getLogger(AuthSessionService.class);


    /** 建立会话：先封装登录态记录并落库，再由该记录返回登录态 */
    public SysLoginSession createSession(Long userId, String username) {
        LocalDateTime now = LocalDateTime.now();
        SysLoginSession loginSession = SysLoginSession.builder()
                .token(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .username(username)
                .expireTime(now.plus(SESSION_TIMEOUT_MILLIS, ChronoUnit.MILLIS))
                .createdAt(now)
                .updatedAt(now)
                .build();
        sysLoginSessionMapper.insert(loginSession);

        return  loginSession;
    }



    /** 按 uuid 查库取回登录态：查不到或已过期返回 null，剩余不足 600 秒则续期 */
    public SysLoginSession resolveSession(String uuid) {
        SysLoginSession loginSession = sysLoginSessionMapper.selectOne(
                Wrappers.<SysLoginSession>lambdaQuery().eq(SysLoginSession::getToken, uuid));
        if (loginSession == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = loginSession.getExpireTime();
        // 已过期：不再续期，按未登录处理
        if (expireTime == null || !expireTime.isAfter(now)) {
            return null;
        }

        // 剩余有效期不足 600 秒才续期：过期时间顺延 30 分钟
        if (ChronoUnit.SECONDS.between(now, expireTime) < RENEW_THRESHOLD_SECONDS) {
            expireTime = now.plus(SESSION_TIMEOUT_MILLIS, ChronoUnit.MILLIS);
            loginSession.setExpireTime(expireTime);
            loginSession.setUpdatedAt(now);
            sysLoginSessionMapper.updateById(loginSession);
        }

        return  loginSession;
    }

    /** 销毁会话：按 token 删除登录态记录 */
    public void invalidate(String token) {
        if (token == null) {
            return;
        }
        sysLoginSessionMapper.delete(Wrappers.<SysLoginSession>lambdaQuery()
                .eq(SysLoginSession::getToken, token));
    }

    /** 定时清理过期会话：直接删除过期时间已过的记录 */
    @Scheduled(fixedDelay = SESSION_CLEANUP_INTERVAL_MILLIS)
    public void cleanExpiredSessions() {
        int deleted = sysLoginSessionMapper.delete(Wrappers.<SysLoginSession>lambdaQuery()
                .lt(SysLoginSession::getExpireTime, LocalDateTime.now()));
        if (deleted > 0) {
            log.debug("清理过期登录态 {} 条", deleted);
        }
    }
}
