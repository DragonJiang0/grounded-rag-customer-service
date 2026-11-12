package com.zidong.portal.agent.service;

import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.agent.ChatMemoryKey;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * portal 侧直连 OpenAI 兼容模型（langchain4j）的统一入口。
 *
 * <p>入参不再是拼好的提示词字符串，而是 langchain4j 的消息对象：系统侧走 {@code SystemMessage}
 * （角色提示词、标准答案材料），用户侧走 {@code UserMessage}；本类只负责按序组装消息、发请求并维护记忆。</p>
 *
 * <p>所有方法都带一个 {@link ChatMemoryKey}：它同时承载归属应用与会话标识，读写 {@link ChatMemory}
 * 的窗口大小由本次调用的 {@link ChatOptions#round()} 决定，历史本身存在共享的
 * {@link ChatMemoryStore}（见 {@code ChatMemoryConfig}）里；请求前把窗口内的历史一起发给模型，
 * 拿到完整回复后再把本轮「用户问 + 模型答」追加进记忆，从而支持多轮对话。</p>
 */
@Service
@RequiredArgsConstructor
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    private final OpenAiChatModel chatModel;

    private final OpenAiStreamingChatModel streamingChatModel;

    /** 历史按 memoryKey（归属应用 + 会话标识）隔离：同一个 key 复用同一份历史；窗口大小每次调用按 ChatOptions 现算 */
    private final ChatMemoryStore chatMemoryStore;

    // ==================== 非流式 ====================

    /**
     * 非流式生成回复。
     *
     * @param memoryKey      会话记忆键（归属应用 + 会话标识），不可为空
     * @param systemMessages 系统消息（角色提示词、标准答案材料等），可为空
     * @param userMessage    本轮用户消息，可为 null（仅按系统材料作答、无需用户侧消息时）
     * @param options        本次请求的模型参数，null 时按 {@link ChatOptions#defaults()} 处理
     * @return 模型输出的完整文本
     */
    public String chat(ChatMemoryKey memoryKey, List<ChatMessage> systemMessages, UserMessage userMessage, ChatOptions options) {
        ChatMemory memory = memoryOf(memoryKey, options);

        ChatResponse response = chatModel.chat(request(memory, systemMessages, userMessage, options));
        String answer = response.aiMessage().text();

        remember(memory, userMessage, answer);
        return answer;
    }

    // ==================== 流式 ====================

    /**
     * 流式生成回复，每个增量文本块直接作为 {@code message} 事件推给 sink。
     *
     * @param memoryKey      会话记忆键（归属应用 + 会话标识），不可为空
     * @param systemMessages 系统消息（角色提示词、标准答案材料等），可为空
     * @param userMessage    本轮用户消息，可为 null（仅按系统材料作答、无需用户侧消息时）
     * @param options        本次请求的模型参数，null 时按 {@link ChatOptions#defaults()} 处理
     * @param sink           过程推送通道（null 或 noop 时只收集不推送）
     * @return 模型输出的完整文本
     */
    public String stream(ChatMemoryKey memoryKey, List<ChatMessage> systemMessages, UserMessage userMessage,
                         ChatOptions options, MessageSink sink) {
        return stream(memoryKey, systemMessages, userMessage, options, chunk -> {
            if (sink != null && !sink.isNoop()) {
                sink.message(chunk);
            }
        });
    }

    /**
     * 流式生成回复：逐块回调 {@code onChunk}，同时在方法内部把全部增量文本拼接起来返回。
     *
     * <p>系统消息、窗口内的历史消息在前，本轮用户消息在后，一起发给模型。</p>
     *
     * @param memoryKey      会话记忆键（归属应用 + 会话标识），不可为空
     * @param systemMessages 系统消息（角色提示词、标准答案材料等），可为空
     * @param userMessage    本轮用户消息
     * @param options        本次请求的模型参数，null 时按 {@link ChatOptions#defaults()} 处理
     * @param onChunk        每个增量文本块的回调，可为 null（此时只收集不回调）
     * @return 模型输出的完整文本
     */
    public String stream(ChatMemoryKey memoryKey, List<ChatMessage> systemMessages, UserMessage userMessage,
                         ChatOptions options, Consumer<String> onChunk) {
        ChatMemory memory = memoryOf(memoryKey, options);
        ChatRequest chatRequest = request(memory, systemMessages, userMessage, options);

        StringBuilder answer = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];

        // langchain4j 的流式回调发生在模型自己的 IO 线程上，这里阻塞等它结束再返回
        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse == null) {
                    return;
                }
                answer.append(partialResponse);
                if (onChunk != null) {
                    onChunk.accept(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 回调在模型 IO 线程上，记忆在这里追加；只有拿到完整回复才落历史
                remember(memory, userMessage, answer.toString());
                done.countDown();
            }

            @Override
            public void onError(Throwable error) {
                failure[0] = error;
                done.countDown();
            }
        });

        await(done);
        if (failure[0] != null) {
            throw new IllegalStateException("流式调用大模型失败", failure[0]);
        }
        return answer.toString();
    }

    // ==================== 记忆 ====================

    /**
     * 按 memoryKey 建出本次调用要用的会话记忆。
     *
     * <p>{@code MessageWindowChatMemory} 的 id 不允许为 null，因此调用方必须给出非空的
     * {@link ChatMemoryKey}。窗口大小取本次调用的 {@link ChatOptions#round()}，它就是消息条数；
     * 缺省或非正数时兜底为 1——langchain4j 的 maxMessages 只接受正数，
     * 传 0 或 null 会在 build() 里直接抛 IllegalArgumentException。</p>
     *
     * <p>只有 {@link ChatOptions#memoryEnabled()} 为 true 时才挂上共享的 {@link ChatMemoryStore}，
     * 历史才会跨调用持久化、同一会话换个参数调用也能读到同一份历史；未启用时走
     * {@code MessageWindowChatMemory} 自带的默认存储，记忆仅在本次调用内生效。</p>
     */
    private ChatMemory memoryOf(ChatMemoryKey memoryKey, ChatOptions options) {
        // options 为 null 时按 ChatOptions#defaults() 处理，与本类其他方法保持一致
        ChatOptions opts = options == null ? ChatOptions.defaults() : options;
        MessageWindowChatMemory.Builder builder = MessageWindowChatMemory.builder()
                .id(memoryKey)
                .maxMessages(Math.max(opts.round(), 1));
        // 启用对话时才接入共享存储：历史才会被读写并跨调用复用，否则不设置
        if (opts.memoryEnabled()) {
            builder.chatMemoryStore(chatMemoryStore);
        }
        return builder.build();
    }

    /**
     * 组装请求：系统消息在前，窗口内的历史消息居中，本轮用户消息在最后。流式/非流式共用。
     * <p>这里必须复制一份新列表：入参 {@code systemMessages} 由调用方持有，
     * 直接在其上追加历史会把历史也一并写回调用方。</p>
     */
    private ChatRequest request(ChatMemory memory, List<ChatMessage> systemMessages, UserMessage userMessage,
                                ChatOptions options) {
        List<ChatMessage> messages = new ArrayList<>(systemMessages);
        if (memory != null) {
            messages.addAll(memory.messages());
        }
        // userMessage 允许为 null：仅按系统材料作答、无需用户侧消息的场景（如查询结果格式化）
        if (userMessage != null) {
            messages.add(userMessage);
        }
        return chatRequest(messages, options);
    }

    /**
     * 统一拼装请求体，两种链路共用。
     * <p>注意一旦用了 {@code parameters(...)}，那种逐个 setter 的写法会被整体忽略，两种写法不能混用。</p>
     */
    private ChatRequest chatRequest(List<ChatMessage> messages, ChatOptions options) {
        ChatOptions opts = options == null ? ChatOptions.defaults() : options;

        return ChatRequest.builder()
                .messages(messages)
                .parameters(OpenAiChatRequestParameters.builder()
                        .temperature(opts.temperature())
                        .topP(opts.topP())
                        // 非 null 且 <=0 的 maxOutputTokens 一律视为未设置：
                        // max_tokens=0 意味着零 token 预算，模型不会输出任何内容
                        .maxOutputTokens(opts.maxOutputTokens() == null || opts.maxOutputTokens() <= 0
                                ? null
                                : opts.maxOutputTokens())
                        .build())
                .build();
    }

    /**
     * 本轮问答落进记忆：本轮用户消息 + 模型答，流式/非流式共用。
     * <p>系统消息每轮都会重新拼装，不进记忆，避免重复堆积挤掉真正的历史。</p>
     * <p>窗口满了由 {@code MessageWindowChatMemory} 自己淘汰最旧的消息。</p>
     */
    private void remember(ChatMemory memory, ChatMessage userMessage, String answer) {
        if (memory == null) {
            return;
        }
        // 本轮没有用户侧消息（userMessage 为 null）时只落模型回复，避免把 null 加进记忆
        if (userMessage != null) {
            memory.add(userMessage);
        }
        memory.add(AiMessage.from(answer));
    }

    private static void await(CountDownLatch done) {
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待流式响应被中断", e);
        }
    }
}
