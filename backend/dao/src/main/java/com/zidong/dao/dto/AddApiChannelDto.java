package com.zidong.dao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zidong.dao.entity.AppApiAccess;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** API 渠道的新增请求 */
@Getter
@Setter
public class AddApiChannelDto {

    @NotBlank(message = "请输入渠道名称")
    @Size(max = 64, message = "渠道名称不能超过 64 个字符")
    private String name;

    /** 过期时间，选填：不传表示长期有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;

    /** 按本请求构建待保存的接入实体 */
    public AppApiAccess toSave(Long appId, String apiKey) {
        LocalDateTime now = LocalDateTime.now();
        return AppApiAccess.builder()
                .appId(appId)
                .name(name.trim())
                .apiKey(apiKey)
                .expireAt(expireAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
