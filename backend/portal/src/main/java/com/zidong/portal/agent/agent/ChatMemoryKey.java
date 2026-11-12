package com.zidong.portal.agent.agent;

/**
 * 会话记忆的复合键：在 taskId 之外额外带上应用归属。
 *
 * <p>langchain4j 的 {@code ChatMemoryStore} 只会把 {@code MessageWindowChatMemory} 的 id 原样回传，
 * 它本身没有「应用」这个概念，所以应用 ID 只能随这个 id 对象一路带进存储层，
 * 由 {@code PersistentChatMemoryStore} 拆开后，作为 {@code chat_memory} 的读写键
 * {@code (app_id, memory_id)} 使用：同一应用下 taskId 定位一份历史，
 * 不同应用即使 taskId 相同也互不串扰，删除应用时也能按 app_id 整片清理。</p>
 *
 * <p>真正作为 {@code memory_id} 入库的仍是 {@link #taskId()}，对外暴露给客户端的会话标识不变。</p>
 *
 * @param appId  所属应用 ID，不可为空
 * @param taskId 会话/任务 ID，即历史沿用至今的 memoryId
 */
public record ChatMemoryKey(Long appId, String taskId) {
}
