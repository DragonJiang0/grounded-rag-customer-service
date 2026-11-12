package com.zidong.portal.utils;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.streaming.StreamingPromptRunner;
import com.embabel.common.ai.model.LlmOptions;

import java.util.function.Consumer;

/**
 * 流式回复工具

 */
public final class ReplyStreams {

    private ReplyStreams() {
    }

    /**
     * 流式生成回复正文，逐块回调。
     */
    public static String stream(Ai ai, String prompt, Consumer<String> onChunk) {
        StreamingPromptRunner streaming = (StreamingPromptRunner) ai.withLlm(LlmOptions.withDefaultLlm());

        StringBuilder answer = new StringBuilder();
        streaming.streaming()
                .withPrompt(prompt)
                .generateStream()
                .doOnNext(chunk -> {
                    if (chunk == null) {
                        return;
                    }
                    answer.append(chunk);
                    if (onChunk != null) {
                        onChunk.accept(chunk);
                    }
                })
                .blockLast();

        return answer.toString();
    }
}
