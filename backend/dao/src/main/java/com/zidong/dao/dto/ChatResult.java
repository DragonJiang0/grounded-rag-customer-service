package com.zidong.dao.dto;

/**
 * 对话同步接口（{@code /api/chat/block}）的返回体。
 *
 * <p>把「本次任务/会话标识」和「答复正文」一起返回：调用方拿到 taskId 后，
 * 下一轮原样带回来即可续接同一段会话记忆。</p>
 *
 * @param taskId  任务/会话 ID：请求带了就原样回传，没带由服务端生成一个
 * @param message Agent 的最终答复正文
 */
public record ChatResult(String taskId, String message) {
}
