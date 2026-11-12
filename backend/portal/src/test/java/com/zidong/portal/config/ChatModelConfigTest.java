package com.zidong.portal.config;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 langchain4j 的 {@code customOpenAiChatModel} 能真正打到 OpenAI 兼容服务。
 *
 * <p>属于集成测试：会发起真实网络请求、消耗模型额度，依赖 application.yml 中
 * {@code portal.ai.chat} 的 api-key / base-url 必须有效。</p>
 */
@SpringBootTest
@Tag("integration")
class ChatModelConfigTest {

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private OpenAiCustomProperties properties;

    @Test
    @DisplayName("简写调用：chat(String)")
    void shouldChatWithSimpleApi() {
        String answer = chatModel.chat("你好，请只回复两个字：收到");

        assertThat(answer)
                .as("模型返回内容不应为空，为空通常意味着 endpoint 或 api-key 配错")
                .isNotBlank();

        System.out.println("=== langchain4j chat(String) ===\n" + answer + "\n=================================");
    }

    @Test
    @DisplayName("完整调用：ChatRequest + OpenAiChatRequestParameters")
    void shouldChatWithFullParameters() {
        ChatRequest chatRequest = ChatRequest.builder()
                // messages 只接受 ChatMessage（或其 List），没有 String 重载，
                // 纯文本用 UserMessage.from(...) / SystemMessage.from(...) 包一层
                .messages(
                        SystemMessage.from("你是一个简洁的助手。"),
                        UserMessage.from("你好"))
                // 通用参数 + OpenAI 专有参数一次性设置。
                // 注意：一旦用了 parameters(...)，上面那些逐个 setter 的分支会被整体忽略，
                // 两种写法只能二选一，不能混用
                .parameters(OpenAiChatRequestParameters.builder()
                        .modelName(properties.getModel())
                        .temperature(0.7)
                        .topP(0.9)
                        .frequencyPenalty(0.0)
                        .presencePenalty(0.0)
                        .maxOutputTokens(64)
                        .stopSequences(List.of("完毕"))
                        // 供应商私有参数（SiliconFlow / DeepSeek 的 top_k、repetition_penalty 等）
                        // 走 customParameters，会作为任意字段平铺进请求体。
                        // 不能用 .topK(...)：OpenAI 协议客户端会直接抛 UnsupportedFeatureException
                        .customParameters(Map.of("top_k", 20))
                        .build())
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);
        String answer = chatResponse.aiMessage().text();

        assertThat(answer)
                .as("模型返回内容不应为空")
                .isNotBlank();

        System.out.println("=== langchain4j chat(ChatRequest) ===\n"
                + answer
                + "\nmodel=" + chatResponse.modelName()
                + ", finishReason=" + chatResponse.finishReason()
                + ", tokenUsage=" + chatResponse.tokenUsage()
                + "\n====================================");
    }

    /*
     * 其余可选参数的正确写法（本项目暂未用到，留作参考）：
     *
     *   .toolSpecifications(List.of(ToolSpecification.builder()...build()))
     *   .toolChoice(ToolChoice.REQUIRED)                     // AUTO / REQUIRED
     *   .responseFormat(ResponseFormat.JSON)                 // 或 ResponseFormat.jsonSchema(JsonSchema.builder()...)
     *
     * OpenAI 专有项：
     *   .seed(42)                                            // 可复现输出
     *   .maxCompletionTokens(256)                            // 注意与 maxOutputTokens 不是同一个字段
     *   .parallelToolCalls(false)
     */
}
