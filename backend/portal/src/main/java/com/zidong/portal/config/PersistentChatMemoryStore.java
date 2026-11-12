package com.zidong.portal.config;

import com.zidong.dao.entity.ChatMemoryRecord;
import com.zidong.dao.mapper.ChatMemoryMapper;
import com.zidong.portal.agent.agent.ChatMemoryKey;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 基于 MySQL 的 langchain4j 会话记忆存储。
 *
 * <p>langchain4j 的 {@link ChatMemoryStore} 只有「整段读、整段写、整段删」三个动作，
 * 所以这里按 {@code (app_id, memory_id)} 在 {@code chat_memory} 表里存一行，行内 {@code messages}
 * 列放整个消息列表的 JSON。窗口滑动、消息淘汰都由 {@code MessageWindowChatMemory} 在内存里算完，
 * 再把结果整段交回这里覆盖，存储层不需要理解消息语义。</p>
 *
 * <p>序列化/反序列化统一用 langchain4j 自带的
 * {@link ChatMessageSerializer} / {@link ChatMessageDeserializer}：
 * 它们会带上消息类型（user / ai / system / tool 等），比自定义 JSON 更稳，
 * 换 langchain4j 版本时行为也一致。</p>
 *
 * <p>{@link ChatMemoryStore} 的入参在接口上只能声明为 {@code Object}（langchain4j 定的签名，改不了），
 * 但本项目的约定是：走到这里的 id 一律是 {@link ChatMemoryKey}。因此三个动作都直接把它转成
 * {@link ChatMemoryKey} 再拆键，用其中的 {@code (appId, taskId)} 定位 {@code chat_memory} 里的一行——
 * 不同应用即使 taskId 相同也各自一份历史，删除应用时按 appId 整片清理。</p>
 *
 * <p>本类只做持久化，不持有窗口；实例由 {@code ChatMemoryConfig} 装配成单例，
 * 每次调用新建的窗口对象都共用它，历史才串得起来。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryMapper chatMemoryMapper;

    /**
     * 读取某会话的全部历史消息。
     *
     * <p>没有记录（新会话）时返回空列表而不是 {@code null}：调用方
     * {@code MessageWindowChatMemory} 会直接把返回值当消息集合用，返回 null 会 NPE。</p>
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryKey) {
        ChatMemoryKey key = (ChatMemoryKey) memoryKey;
        ChatMemoryRecord record = chatMemoryMapper.findByAppIdAndMemoryId(key.appId(), key.taskId());
        if (record == null || record.getMessages() == null || record.getMessages().isBlank()) {
            return Collections.emptyList();
        }

        try {
            return ChatMessageDeserializer.messagesFromJson(record.getMessages());
        } catch (RuntimeException e) {
            // 存量 JSON 与当前 langchain4j 版本不兼容（例如升级后消息结构变化）时，
            // 不让整个对话因为一段坏历史直接失败：记日志并当作新会话从空开始
            log.warn("会话记忆反序列化失败，按空历史处理。memoryKey={}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * 整段覆盖某会话的历史消息：不存在则插入，存在则更新。
     *
     * <p>每轮对话结束后由 {@code MessageWindowChatMemory} 带着裁剪好的完整列表调进来，
     * 所以这里是全量替换，不是追加。</p>
     */
    @Override
    public void updateMessages(Object memoryKey, List<ChatMessage> messages) {
        ChatMemoryKey key = (ChatMemoryKey) memoryKey;
        String json = ChatMessageSerializer.messagesToJson(messages == null ? List.of() : messages);
        chatMemoryMapper.upsert(key.appId(), key.taskId(), json);
    }

    /**
     * 删除某会话的历史消息（清空会话）。记录本就不存在时静默返回，保持幂等。
     */
    @Override
    public void deleteMessages(Object memoryKey) {
        ChatMemoryKey key = (ChatMemoryKey) memoryKey;
        chatMemoryMapper.deleteByAppIdAndMemoryId(key.appId(), key.taskId());
    }
}
