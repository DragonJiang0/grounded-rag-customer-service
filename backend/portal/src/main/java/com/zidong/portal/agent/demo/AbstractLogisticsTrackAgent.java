package com.zidong.portal.agent.demo;

import com.alibaba.fastjson2.JSON;
import com.embabel.agent.api.common.Ai;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.entity.DemoLogistics;
import com.zidong.dao.mapper.DemoLogisticsMapper;
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
 * 物流跟踪 Agent 的公共实现：提取、查询、作答逻辑只有这一份。
 *
 * <p>提取用内置提示词抽单号，LLM 只给候选，最终以 {@code demo_logistics} 能否查到为准。</p>
 *
 * <p>作答参考 {@code AbstractDefaultAgent} 的模式：查询结果作为唯一事实材料，
 * 与提示词一起封装成 langchain4j 消息交给模型，整理成 Markdown 格式的客服回复；
 * 真连接走流式，noop 走一次性生成，会话记忆按 {@link ChatMemoryKey} 读写。
 * 调用方提示词（调试变体传入）替换内置格式化提示词，未传时用内置提示词，不产生额外分叉。</p>
 */
public abstract class AbstractLogisticsTrackAgent {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 注入提示词的当前时间格式：带星期，与 {@code AbstractDefaultAgent} 一致 */
    private static final DateTimeFormatter CURRENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.CHINA);

    /**
     * 内置提取提示词：物流单号的形态与订单号不同，规则由本 Agent 自己维护，
     * 新增承运商时只改这里，不影响其他 Agent。
     */
    private static final String EXTRACT_PROMPT = """
            从用户输入中提取物流单号。
            物流单号通常是 2 位大写字母加 13 位数字，例如 SF1234567890123、YT9876543210987、JD5555666677778。
            只输出 JSON：{"trackingNo": "提取到的物流单号"}。
            如果没有找到物流单号，输出 {"trackingNo": ""}。
            不要编造，不要改写用户没有提供的号码。
            <用户输入>
            {userInput}
            </用户输入>
            """;

    /**
     * 内置作答提示词：把查询结果整理成 Markdown 格式的客服回复。
     * 提取与查询只负责「取事实」，回复长什么样由这里决定；
     * 调试入口可用调用方提示词整体替换，替换时同样不得编造材料之外的信息。
     */
    private static final String FORMAT_PROMPT = """
            你是快递客服，把 <查询结果> 里的物流数据整理成一条 Markdown 格式的客服回复。
            要求：
            1. 第一行加粗物流单号，例如：**物流单号：SF1234567890123**；
            2. 用表格展示字段（表头：字段 | 内容），只列查询结果里存在的字段；
            3. 时间统一格式化为 yyyy年MM月dd日 HH:mm，空值显示「待定」；
            4. 状态字段前加图标：✅ 已签收　🚚 运输中　📦 已揽收；
            5. 不得编造或改写 <查询结果> 之外的信息，单号等编号原样保留；
            6. 表格后另起一行写一句不超过 20 字的简短提示；
            7. 只输出回复正文，不要解释，不要输出 JSON。
            """;

    /** 作答材料：查询结果是唯一事实来源，当前时间一并注入，模型只引用不推算 */
    private static final String MATERIALS = """
            <查询结果> {data} </查询结果>
            <当前时间> {currentTime} </当前时间>
            """;

    private final DemoLogisticsMapper demoLogisticsMapper;

    /** 作答统一走 portal 直连的 langchain4j 模型，与 {@code AbstractDefaultAgent} 同一条链路 */
    private final LLMService llmService;

    protected AbstractLogisticsTrackAgent(DemoLogisticsMapper demoLogisticsMapper, LLMService llmService) {
        this.demoLogisticsMapper = demoLogisticsMapper;
        this.llmService = llmService;
    }

    /**
     * 执行物流跟踪：内置提示词抽单号 → 查 {@code demo_logistics} → 交给模型整理成回复。
     *
     * @param formatPrompt 作答提示词：null/空串用内置格式化提示词，调试变体传调用方提示词
     * @param chatOptions  本次对话参数（温度、记忆轮数等），由子类按入口组装
     * @param memoryKey    会话记忆键（归属应用 + 会话标识），由子类按入口组装
     */
    protected AgentResult doTrack(String userInput, Ai ai, MessageSink sink,
                                  String formatPrompt, ChatOptions chatOptions, ChatMemoryKey memoryKey) {

        if (sink != null) sink.status("正在提取物流单号");
        String trackingNo = extractTrackingNo(userInput, ai);

        // 没有物流单号就追问，不猜
        if (trackingNo == null) {
            return new AgentResult("请提供物流单号，我再帮您查询配送进度。");
        }

        if (sink != null) sink.status("正在查询物流轨迹 " + trackingNo);
        DemoLogistics logistics = demoLogisticsMapper.findByTrackingNo(trackingNo);
        if (logistics == null) {
            return new AgentResult("未查询到物流单号 " + trackingNo + "，请核对单号是否正确。");
        }

        // 提示词为空时退化为代码拼装的结果文本，与 AbstractDefaultAgent 的「无提示词直返」一致
        String directText = "物流单号 " + logistics.getTrackingNo() + " 查询结果：承运商「"
                + logistics.getCarrier() + "」，状态「" + logistics.getStatus() + "」，最新位置 "
                + logistics.getLocation() + "，预计送达 " + expectedText(logistics) + "。";
        return answer(sink, formatPrompt, directText,
                JSON.toJSONString(logistics), chatOptions, memoryKey);
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
     * 用 LLM 从用户输入中提取物流单号。
     *
     * @return 提取到的物流单号；用户没提或模型没抽到时为 null
     */
    private String extractTrackingNo(String userInput, Ai ai) {
        if (StringUtils.isBlank(userInput)) {
            return null;
        }
        String prompt = EXTRACT_PROMPT.replace("{userInput}", userInput);
        LogisticsParams params = ai.withDefaultLlm().createObject(prompt, LogisticsParams.class);
        if (params == null) {
            return null;
        }
        return StringUtils.isBlank(params.trackingNo()) ? null : params.trackingNo().trim();
    }

    private static String expectedText(DemoLogistics logistics) {
        return logistics.getExpectedTime() == null
                ? "待定"
                : logistics.getExpectedTime().format(TIME_FORMAT);
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
    public record LogisticsParams(String trackingNo) {
    }
}
