package com.zidong.portal.bot;

/**
 * 非主节点执行「绑定 / 解绑」时抛出。
 *
 * <p>扫码登录必须收敛到主节点：同一个 bot 账号重复扫码会让旧 token 失效，
 * 这里把当前主节点实例 ID 带回给调用方，便于管理台提示或重试。</p>
 */
public class BotNotLeaderException extends RuntimeException {

    /** 当前主节点实例 ID，可能为空（租约表未初始化） */
    private final String leaderInstanceId;

    public BotNotLeaderException(String leaderInstanceId) {
        super("当前实例不是 Bot 主节点，扫码登录需由主节点执行");
        this.leaderInstanceId = leaderInstanceId;
    }

    public String leaderInstanceId() {
        return leaderInstanceId;
    }
}
