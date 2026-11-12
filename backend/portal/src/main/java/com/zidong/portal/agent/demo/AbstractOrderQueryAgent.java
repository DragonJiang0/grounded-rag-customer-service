package com.zidong.portal.agent.demo;

import com.alibaba.fastjson2.JSON;
import com.embabel.agent.api.common.Ai;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.entity.DemoOrder;
import com.zidong.dao.mapper.DemoOrderMapper;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.ChatMemoryKey;
import com.zidong.portal.agent.service.ChatOptions;
import com.zidong.portal.agent.service.LLMService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 订单查询 Agent 的公共实现：提取、查询、作答逻辑只有这一份。
 *
 * <p>提取用内置提示词抽订单号，LLM 只给候选，最终以 {@code demo_order} 能否查到为准：
 * 抽错、改写、编造都会在查询这一步被拦下，不会返回不存在的订单。</p>
 *
 * <p>作答参考 {@code AbstractDefaultAgent} 的模式：查询结果作为唯一事实材料，
 * 与提示词一起封装成 langchain4j 消息交给模型，整理成 Markdown 格式的客服回复；
 * 真连接走流式，noop 走一次性生成，会话记忆按 {@link ChatMemoryKey} 读写。
 * 调用方提示词（调试变体传入）替换内置格式化提示词，未传时用内置提示词，不产生额外分叉。</p>
 */
public abstract class AbstractOrderQueryAgent {

    /** 注入提示词的当前时间格式：带星期，与 {@code AbstractDefaultAgent} 一致 */
    private static final DateTimeFormatter CURRENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.CHINA);

    /**
     * 内置提取提示词。
     *
     * <p>明确要求「找不到返回空串」：只写「提取订单号」时模型会为了完成任务而编一个号，
     * 这与 {@code AbstractDefaultAgent} 里关键词映射必须给零匹配出口是同一个道理。</p>
     */
    private static final String EXTRACT_PROMPT = """
            从用户输入中提取订单号。
            订单号通常以 SO 开头，后接 12 位数字，例如 SO202610170001。
            只输出 JSON：{"orderNo": "提取到的订单号"}。
            如果没有找到订单号，输出 {"orderNo": ""}。
            不要编造，不要改写用户没有提供的号码。
            <用户输入>
            {userInput}
            </用户输入>
            """;

    /**
     * 内置作答提示词：把查询结果整理成 Markdown 格式的客服回复。
     * 调试入口可用调用方提示词整体替换，替换时同样不得编造材料之外的信息。
     */
    private static final String FORMAT_PROMPT = """
            你是电商客服，把 <查询结果> 里的订单数据整理成一条 Markdown 格式的客服回复。
            要求：
            1. 第一行加粗订单号，例如：**订单号：SO202610170001**；
            2. 用表格展示字段（表头：字段 | 内容），只列查询结果里存在的字段；
            3. 时间统一格式化为 yyyy年MM月dd日 HH:mm，空值显示「暂无」；
            4. 金额保留两位小数并带单位「元」；
            5. 状态字段前加图标：✅ 已完成　🚚 处理中　❌ 已取消；
            6. 不得编造或改写 <查询结果> 之外的信息，订单号等编号原样保留；
            7. 表格后另起一行写一句不超过 20 字的简短提示；
            8. 只输出回复正文，不要解释，不要输出 JSON。
            """;

    /** 作答材料：查询结果是唯一事实来源，当前时间一并注入，模型只引用不推算 */
    private static final String MATERIALS = """
            <查询结果> {data} </查询结果>
            <当前时间> {currentTime} </当前时间>
            """;

    private final DemoOrderMapper demoOrderMapper;

    /** 作答统一走 portal 直连的 langchain4j 模型，与 {@code AbstractDefaultAgent} 同一条链路 */
    private final LLMService llmService;

    protected AbstractOrderQueryAgent(DemoOrderMapper demoOrderMapper, LLMService llmService) {
        this.demoOrderMapper = demoOrderMapper;
        this.llmService = llmService;
    }

    /**
     * 执行订单查询：内置提示词抽订单号 → 查 {@code demo_order} → 交给模型整理成回复。
     *
     * @param formatPrompt 作答提示词：null/空串用内置格式化提示词，调试变体传调用方提示词
     * @param chatOptions  本次对话参数（温度、记忆轮数等），由子类按入口组装
     * @param memoryKey    会话记忆键（归属应用 + 会话标识），由子类按入口组装
     */
    protected AgentResult doQuery(String userInput, Ai ai, MessageSink sink,
                                  String formatPrompt, ChatOptions chatOptions, ChatMemoryKey memoryKey) {

        if (sink != null) sink.status("正在提取订单号");
        String orderNo = extractOrderNo(userInput, ai);

        // 没有订单号就追问，不猜
        if (orderNo == null) {
            return new AgentResult("请提供订单号，我再帮您查询订单状态。");
        }

        if (sink != null) sink.status("正在查询订单 " + orderNo);
        DemoOrder order = demoOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return new AgentResult("未查询到订单 " + orderNo + "，请核对订单号是否正确。");
        }

        // 查询结果的 JSON 既是材料也是「无提示词直返」的兜底回复，与改造前行为一致
        String dataJson = JSON.toJSONString(order);
        return answer(sink, formatPrompt, dataJson, dataJson, chatOptions, memoryKey);
    }

    /**
     * 作答：参考 {@code AbstractDefaultAgent.doAnswer}。
     *
     * <p>只有系统侧 = 作答提示词 + 材料（查询结果 JSON、当前时间），不带用户侧消息——
     * 查询结果已在材料里，模型只负责按提示词整理成回复；
     * 真连接走流式逐块推送，noop 走一次性生成，两条链路都按 memoryKey 读写历史。</p>
     *
     * @param directText 提示词为空时的兜底回复（代码拼装的事实文本，省一次模型调用）
     */
    private AgentResult answer(MessageSink sink, String formatPrompt,
                               String directText, String dataJson,
                               ChatOptions chatOptions, ChatMemoryKey memoryKey) {
        String prompt = StringUtils.defaultIfBlank(formatPrompt, FORMAT_PROMPT);

        if (StringUtils.isBlank(prompt)) {
            return new AgentResult(directText);
        }

        List<ChatMessage> systemMessages = Arrays.<ChatMessage>asList(
                SystemMessage.from(prompt),
                SystemMessage.from(MATERIALS
                        .replace("{data}", dataJson)
                        .replace("{currentTime}", nowText())));

        // 不带用户侧消息：查询结果已在系统材料里，模型只负责按提示词整理成回复
        return sink != null && !sink.isNoop()
                ? new AgentResult(llmService.stream(memoryKey, systemMessages, null, chatOptions, sink))
                : new AgentResult(llmService.chat(memoryKey, systemMessages, null, chatOptions));
    }

    /**
     * 用 LLM 从用户输入中提取订单号。
     *
     * @return 提取到的订单号；用户没提或模型没抽到时为 null
     */
    private String extractOrderNo(String userInput, Ai ai) {
        if (StringUtils.isBlank(userInput)) {
            return null;
        }
        String prompt = EXTRACT_PROMPT.replace("{userInput}", userInput);
        OrderParams params = ai.withDefaultLlm().createObject(prompt, OrderParams.class);
        if (params == null) {
            return null;
        }
        return StringUtils.isBlank(params.orderNo()) ? null : params.orderNo().trim();
    }

    /** 当前时间，按服务器时区格式化，作为「事实」注入，模型只引用不推算 */
    private static String nowText() {
        return LocalDateTime.now().format(CURRENT_TIME_FORMAT);
    }

    /**
     * 普通入口（多 Agent 选中后执行）的对话参数：不读应用的 chat options 配置，
     * 记忆按默认轮数开启，模型参数全部走 bean 默认值。
     */
    protected static ChatOptions optionsOf(String taskId) {
        return new ChatOptions(taskId, true, ChatOptions.DEFAULT_MEMORY_ROUNDS, null, null, null);
    }

    /** LLM 提取结果：字段名与提示词里约定的 JSON 一致 */
    public record OrderParams(String orderNo) {
    }
}
