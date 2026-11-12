package com.zidong.dao.support;

import com.alibaba.fastjson2.JSON;
import com.zidong.dao.dto.ChatHistoryItemDto;
import com.zidong.dao.entity.ChatHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话分段的内存合并。
 *
 * <p>{@code chat_history.history} 是 TEXT（上限约 64KB），一次会话攒满后会被拆成
 * {@code conversation_id} 相同、{@code seq} 递增的多行。库表按分段存储，
 * 但对页面仍然只暴露「一条会话」，所以查询后需要在这里合并：
 * <ul>
 *     <li>同一个 conversationId 的分段按 seq 升序拼接 history 数组；</li>
 *     <li>会话的 id / createdAt 取第一段，保证页面拿到的 id 稳定、删除能命中整段会话；</li>
 *     <li>title 取第一个非空标题（第一段写入的提问）；</li>
 *     <li>任一分段被标记删除，合并后的会话即为已删除。</li>
 * </ul>
 *
 * <p>合并结果保持入参中会话首次出现的顺序，调用方可以用它还原分页 / 倒序顺序。
 */
public final class ChatHistorySegmentMerger {

    private static final Logger log = LoggerFactory.getLogger(ChatHistorySegmentMerger.class);

    /** 历史数据未写 seq 时，按第一段处理 */
    private static final int DEFAULT_SEQ = 1;

    private ChatHistorySegmentMerger() {
    }

    /**
     * 合并分段行：入参可以是任意顺序，输出与入参会话数量一致（每个 conversationId 一条）。
     *
     * @param rows 同一批查询出来的 chat_history 行，可为空
     * @return 合并后的会话列表，保持会话首次出现的顺序
     */
    public static List<ChatHistory> merge(List<ChatHistory> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, List<ChatHistory>> grouped = new LinkedHashMap<>();
        for (ChatHistory row : rows) {
            if (row == null) {
                continue;
            }
            grouped.computeIfAbsent(keyOf(row), key -> new ArrayList<>()).add(row);
        }

        List<ChatHistory> merged = new ArrayList<>(grouped.size());
        for (List<ChatHistory> segments : grouped.values()) {
            merged.add(mergeOne(segments));
        }
        return merged;
    }

    /** 会话标识为空时按行自身分组，避免不同会话被误合并 */
    private static String keyOf(ChatHistory row) {
        String conversationId = row.getConversationId();
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        return "id:" + row.getId();
    }

    private static ChatHistory mergeOne(List<ChatHistory> segments) {
        segments.sort(Comparator
                .comparingInt((ChatHistory h) -> h.getSeq() == null ? DEFAULT_SEQ : h.getSeq())
                .thenComparingLong(h -> h.getId() == null ? 0L : h.getId()));
        ChatHistory first = segments.get(0);

        List<ChatHistoryItemDto> items = new ArrayList<>();
        boolean deleted = false;
        for (ChatHistory segment : segments) {
            items.addAll(parseItems(segment));
            deleted = deleted || Boolean.TRUE.equals(segment.getCustomerDeleted());
        }

        return ChatHistory.builder()
                .id(first.getId())
                .conversationId(first.getConversationId())
                .appId(first.getAppId())
                .title(firstTitle(segments))
                .history(JSON.toJSONString(items))
                .createdAt(first.getCreatedAt())
                .channel(first.getChannel())
                .tag(first.getTag())
                .customerDeleted(deleted)
                .seq(first.getSeq() == null ? DEFAULT_SEQ : first.getSeq())
                .build();
    }

    private static String firstTitle(List<ChatHistory> segments) {
        for (ChatHistory segment : segments) {
            if (segment.getTitle() != null && !segment.getTitle().isBlank()) {
                return segment.getTitle();
            }
        }
        return null;
    }

    /** 解析单个分段的 history；坏数据跳过而不是让整条会话查不出来 */
    private static List<ChatHistoryItemDto> parseItems(ChatHistory segment) {
        String history = segment.getHistory();
        if (history == null || history.isBlank()) {
            return List.of();
        }
        try {
            List<ChatHistoryItemDto> parsed = JSON.parseArray(history, ChatHistoryItemDto.class);
            return parsed == null ? List.of() : parsed;
        } catch (Exception ex) {
            log.warn("合并会话分段: history 解析失败，已跳过该分段, id={}, seq={}",
                    segment.getId(), segment.getSeq(), ex);
            return List.of();
        }
    }
}
