package com.zidong.portal.config;

import com.zidong.dao.mapper.ChatMemoryMapper;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * langchain4j 对话记忆存储。
 *
 * <p>这里提供共享的 {@link ChatMemoryStore}：历史按会话 id 落 MySQL 的 {@code chat_memory} 表
 * （见 {@link PersistentChatMemoryStore}），服务重启不丢，多实例部署也能共用同一份历史
 * （已用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 保证并发写不会互相覆盖成半截会话）。</p>
 *
 * <p>窗口大小（保留多少轮）不再在启动期写死，而是由调用方通过
 * {@code ChatOptions.memoryRounds} 逐次指定，窗口对象由 {@code LLMService} 按该参数构建，
 * 因此同一会话在不同调用里可以有不同的记忆长度。</p>
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 记忆存储。
     *
     * <p>必须是共享的单例：每次调用都会新构建一个窗口对象，历史全靠这个 store 按 id 存取；
     * 如果每个窗口自带一份 store，记忆就永远是空的。</p>
     */
    @Bean
    public ChatMemoryStore chatMemoryStore(ChatMemoryMapper chatMemoryMapper) {
        return new PersistentChatMemoryStore(chatMemoryMapper);
    }
}
