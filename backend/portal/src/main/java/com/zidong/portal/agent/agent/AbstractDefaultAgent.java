package com.zidong.portal.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.embabel.agent.api.common.Ai;
import com.zidong.common.constant.PromptConstants;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.service.ChatOptions;
import com.zidong.portal.agent.service.LLMService;
import com.zidong.dao.mapper.AppCorpusGroupMapper;
import com.zidong.dao.mapper.CorpusMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 问答 Agent 的共享骨架：把「命中问题 → 组织答案」这套逻辑只写一份，两个入口共用。
 *
 * <p>本类不是 Agent：没有 {@code @Agent}、也没有 {@code @Action}。
 * 它只提供模板方法，具体的 {@code @Action} 由子类声明薄包装后调用，
 * 这样子类的动作签名可以各自不同（例如外部提示词入口要多带一个参数），
 * 而流程、提示词拼装、模型调用方式、兜底话术全都保持一致。
 *
 * <p>子类唯一需要决定的是「提示词模板从哪儿来」：数据库配置还是调用方传入。
 */
public abstract class AbstractDefaultAgent {

    protected static final Logger log = LoggerFactory.getLogger(AbstractDefaultAgent.class);

    /** 未命中任何答案时的兜底话术 */
    protected static final String NO_ANSWER_REPLY = "未找到任何答案";

    /**
     * 命中关键词的映射提示词，占位符为 {userInput} 与 {systemKeyword}。
     *
     * <p>必须给出「零匹配」出口：只说「最多返回 N 个」时，模型会把它当成目标去凑数，
     * 对完全无关的输入（如自我介绍、闲聊）也硬返回几个关键词，最终答非所问。
     */
    protected static final String MAPPING_KEYWORD = """
                    从<系统关键词>中找出与<用户输入>语义一致的关键词，原封不动返回，最多 3 个，以字符串数组返回。
                    判断规则：
                    1. 只返回与用户输入表达同一件事的关键词；只是字面相似、语义无关的，不要返回。
                    2. 用户输入是打招呼、自我介绍、闲聊或与业务无关的内容时，没有任何关键词匹配，必须返回空数组 []。
                    3. 宁可少返回，也不要凑数；没有把握就不要返回。
                    <用户输入>
                    {userInput}
                     </用户输入>
                    <系统关键词>
                    {systemKeyword}
                    </系统关键词>
            """;

    /** 注入提示词的当前时间格式：带星期，便于同时回答「现在几点/今天几号/星期几」 */
    private static final DateTimeFormatter CURRENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.CHINA);

    private static  final String MATERIALS= """
            <标准答案>  {answers}  </标准答案> 
            <系统关键词> {systemKeyword} </系统关键词>
            <当前时间> {currentTime} </当前时间>
            """;

    private final AppCorpusGroupMapper appCorpusGroupMapper;

    private final CorpusMapper corpusMapper;

    /** 流式作答统一走 portal 直连的 langchain4j 模型，不再依赖 embabel 的 Ai */
    private final LLMService llmService;

    protected AbstractDefaultAgent(AppCorpusGroupMapper appCorpusGroupMapper,
                                   CorpusMapper corpusMapper,
                                   LLMService llmService) {
        this.appCorpusGroupMapper = appCorpusGroupMapper;
        this.corpusMapper = corpusMapper;
        this.llmService = llmService;
    }

    /**
     * 命中问题：把用户输入映射成系统关键词，再按关键词取标准答案。
     *
     * @param userInput 用户原始输入
     * @param ai        模型调用入口
     * @param sink      过程推送通道，可为 null
     * @param appId     应用 ID，决定在哪个语料范围内匹配
     * @return 命中结果，未命中时 answers 为空集合
     */
    protected HitQuestion doHitQuestion(String userInput, Ai ai, MessageSink sink, Long appId) {
        List<String> questions = questionsOf(appId);

        String prompt = MAPPING_KEYWORD
                .replace("{userInput}", StringUtils.defaultString(userInput))
                .replace("{systemKeyword}", JSON.toJSONString(questions));

        String[] hitQuestion = ai.withDefaultLlm().createObject(prompt, String[].class);

        // 模型返回的只是候选；只有确实存在于该应用语料问题里的关键词才放行，
        // 改写、编造、跨应用的词一律丢弃，避免拿去做精确匹配时命中无关语料
        Set<String> allowed = new HashSet<>(questions);
        List<String> candidates = hitQuestion == null ? List.of() : Arrays.stream(hitQuestion)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> keywords = candidates.stream()
                .filter(allowed::contains)
                .toList();
        List<String> rejected = candidates.stream()
                .filter(keyword -> !allowed.contains(keyword))
                .toList();
        if (!rejected.isEmpty()) {
            log.info("丢弃不在语料表中的关键词:{}", rejected);
        }

        if (keywords.isEmpty()) {
            log.info("关键词映射为空, questions={}", questions.size());
            if (sink != null) sink.status("未命中问题");
            return new HitQuestion(userInput, List.of(),questions);
        }
        if (sink != null) sink.status("已命中问题");

        log.info("问题：{},命中关键词:{}",userInput, keywords);
        List<String> answers = corpusMapper.listAnswersByQuestions(keywords);
        log.info("命中答案数量:{}", answers.size());

        return new HitQuestion(userInput, answers, questions);
    }

    /**
     * 组织答案：命中为空走兜底话术；提示词为空直接把标准答案当回复；否则交给模型（流式优先）。
     *
     * <p>作答统一走 portal 直连的 langchain4j 模型，并带上会话记忆：真连接走流式，noop 走一次性生成，
     * 两条链路都按 {@link ChatMemoryKey} 读写历史，避免同一个入口换条链路就丢上下文。</p>
     *
     * <p>提示词不再以字符串形式往下传：渲染后的角色提示词与标准答案材料封装成 {@code SystemMessage}，
     * 用户原始输入封装成 {@code UserMessage}，下游只认消息列表。</p>
     *
     * @param hitQuestion    命中结果
     * @param sink           过程推送通道，真连接走流式，noop 走一次性生成
     * @param systemPrompt   提示词模板（含占位符），两个入口的差异只在这里体现
     * @param chatOptions    本次请求的对话参数，为 null 时用模型默认值
     * @param memoryKey      会话记忆键，承载（归属应用 + 会话标识）；不可为空
     * @return 最终答复
     */
    protected AgentResult doAnswer(HitQuestion hitQuestion, MessageSink sink, String systemPrompt,
                                   ChatOptions chatOptions, ChatMemoryKey memoryKey) {
        String prompt = render(systemPrompt, hitQuestion);

        // 没有提示词说明该入口不依赖大模型组织语言：直接把命中的标准答案当成回复，省掉一次模型调用
        if (StringUtils.isBlank(prompt)) {
            log.info("提示词为空，直接返回命中答案, input={}, 答案数={}", hitQuestion.input(), hitQuestion.answers().size());
            return new AgentResult(answersText(hitQuestion));
        }

        log.debug("prompt={}", prompt);
        String materials = MATERIALS.replace("{answers}", hitQuestion.answers.stream().collect(Collectors.joining("\n")))
                .replace("{systemKeyword}", hitQuestion.questions().stream().collect(Collectors.joining("\n")))
                .replace("{currentTime}", nowText());

        // 这里就完成提示词到 langchain4j 消息的封装：系统侧（角色提示词 + 标准答案材料）走 SystemMessage，
        // 用户原始输入走 UserMessage，LLMService 只认消息列表，不再关心提示词是怎么拼出来的
        List<ChatMessage> systemMessages = Arrays.<ChatMessage>asList(
                SystemMessage.from(prompt), SystemMessage.from(materials));
        // UserMessage 与 SystemMessage 一样不接受空白文本，用户输入为空时用占位符兜底
        UserMessage userMessage = UserMessage.from(StringUtils.defaultIfBlank(hitQuestion.input(), "-"));

        // 黑板里现在始终有 sink（无连接时是 noop 占位）：真连接走流式，noop 走一次性生成
        AgentResult agentResult = sink != null && !sink.isNoop()
                ? new AgentResult(llmService.stream(memoryKey, systemMessages, userMessage, chatOptions, sink))
                : new AgentResult(llmService.chat(memoryKey, systemMessages, userMessage, chatOptions));
        return agentResult;
    }

    /**
     * 用本次问答内容替换模板中的占位符，两个入口共用同一份替换逻辑。
     *
     * @param template    提示词模板，可为 null
     * @param hitQuestion 命中结果，用户问题、标准答案、系统关键词都从这里取
     * @return 可直接交给大模型的提示词；模板为空时返回空串
     */
    protected String render(String template, HitQuestion hitQuestion) {
        List<String> questions = hitQuestion.questions() == null ? List.of() : hitQuestion.questions();
        return StringUtils.defaultString(template)
                .replace(PromptConstants.QUESTION_PLACEHOLDER, StringUtils.defaultString(hitQuestion.input))
                .replace(PromptConstants.ANSWER_PLACEHOLDER, answersText(hitQuestion))
                .replace(PromptConstants.SYSTEM_KEYWORD, JSON.toJSONString(questions));
    }

    /**
     * 命中答案拼成占位符/回复用的文本；未命中任何答案时退化为兜底话术，避免替换成空串。
     */
    private static String answersText(HitQuestion hitQuestion) {
        List<String> answers = hitQuestion.answers();
        if (answers == null || answers.isEmpty()) {
            log.info("没有命中答案，补上兜底话术, input={}", hitQuestion.input());
            return NO_ANSWER_REPLY;
        }
        return String.join("\n", answers);
    }

    /**
     * 当前时间，按服务器时区格式化。
     *
     * <p>作为「事实」在拼提示词时就注入，模型只负责引用，不得自行推算或改写；
     * 这样「现在几点了」这类无参诉求无需模型调用工具即可确定性作答。</p>
     */
    private static String nowText() {
        return LocalDateTime.now().format(CURRENT_TIME_FORMAT);
    }

    /**
     * 取该应用下的全部语料问题。
     * <p>appId 为空说明调用方还没准备好应用上下文，此时视为没有可选关键词，按未命中处理。
     */
    private List<String> questionsOf(Long appId) {

        return appCorpusGroupMapper.listQuestionsByAppId(appId);
    }

    /**
     * 命中结果：input 为用户原始输入，answers 为命中的答案（未命中时为空集合）
     */
    public record HitQuestion(String input, List<String> answers,List<String> questions) {
    }
}
