package com.zidong.portal.bot;

/**
 * Bot 凭证已不可用（微信接口 getupdates 响应异常，如 ret=null）。
 *
 * <p>与 {@link BotSessionExpiredException}（ret=-14，服务端明确会话过期）不同，
 * 这类异常通常是 token 已被服务端作废但响应里没有明确的过期码，
 * 继续用该 token 长轮询只会反复失败。处理方式：把 {@code bot_credential} 标记为无效
 * （invalid=1），丢弃本地凭证并退出该监听循环，等待重新扫码；
 * 启动/恢复时也会因 invalid=1 跳过该凭证。</p>
 *
 * <p>单独定义成类型而不是靠错误信息字符串判断，是为了让监听线程能精确捕获，
 * 不会把网络抖动等普通异常也当成凭证失效。</p>
 */
public class BotCredentialInvalidException extends IllegalStateException {

    public BotCredentialInvalidException(String message) {
        super(message);
    }
}
