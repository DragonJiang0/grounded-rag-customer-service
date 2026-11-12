package com.zidong.portal.service;

import com.zidong.common.exception.BizException;
import com.zidong.dao.mapper.ChatMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 会话记忆的业务入口。
 *
 * <p>记忆的读写由 langchain4j 的 {@code PersistentChatMemoryStore} 负责，
 * 这里只对外提供「按应用 + 会话标识判断记忆是否已存在」这类查询能力，
 * 供控制器在进入 Agent 执行前做前置校验。</p>
 *
 * <p>会话一律按 {@code (appId, taskId)} 定位：客户端只能在自己应用的范围内续接会话，
 * 传入的 taskId 若不属于该应用，一律视为不存在，与存储层 {@code chat_memory} 的读写键保持一致。</p>
 */
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatMemoryMapper chatMemoryMapper;

    /** 该会话是否已有记忆记录（taskId 为空一律视为不存在） */
    public boolean exists(Long appId, String taskId) {
        return StringUtils.isNotBlank(taskId)
                && chatMemoryMapper.findByAppIdAndMemoryId(appId, taskId) != null;
    }

    /**
     * 解析本次请求实际使用的会话标识，各入口共用的入口校验。
     *
     * <p>带了 taskId 说明是续接已有会话，校验该应用下确实存在这份记忆；没带则视为新会话，
     * 生成一个 UUID 作为标识，由调用方回传给客户端，供下一轮续接。</p>
     *
     * @param appId  所属应用 ID
     * @param taskId 请求携带的会话/任务 ID，可为空
     * @return 本次会话实际使用的 taskId（新会话为生成的 UUID）
     * @throws BizException 带了 taskId 但该应用下没有对应记录时抛出
     */
    public String resolveTaskId(Long appId, String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return UUID.randomUUID().toString();
        }
        requireExists(appId, taskId);
        return taskId;
    }

    /**
     * 校验会话记忆存在，不存在则抛业务异常。
     *
     * @param appId  所属应用 ID
     * @param taskId 会话/任务 ID
     * @throws BizException taskId 为空或该应用下没有对应记录时抛出
     */
    public void requireExists(Long appId, String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw BizException.badRequest("taskId 不能为空");
        }
        if (!exists(appId, taskId)) {
            throw BizException.badRequest("taskId 不存在：" + taskId);
        }
    }
}
