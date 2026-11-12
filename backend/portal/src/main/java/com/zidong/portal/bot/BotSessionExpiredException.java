package com.zidong.portal.bot;

/**
 * Bot 会话已过期（微信接口 getupdates 返回 ret=-14）。
 *
 * <p>SDK 与底层协议都没有 token 自动刷新机制，这个异常的唯一处理方式是：
 * 丢弃本地凭证、重新扫码登录（多实例下由主节点负责）。</p>
 *
 * <p>单独定义成类型而不是靠错误信息字符串判断，是为了让监听线程能精确捕获并触发重登，
 * 不会把网络抖动等普通异常也当成会话失效。</p>
 */
public class BotSessionExpiredException extends IllegalStateException {

    public BotSessionExpiredException(String message) {
        super(message);
    }
}
