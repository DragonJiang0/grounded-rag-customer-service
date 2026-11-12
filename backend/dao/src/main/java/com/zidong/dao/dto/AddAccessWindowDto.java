package com.zidong.dao.dto;

import com.zidong.dao.entity.AppAccessWindow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 免密窗口的新增请求 */
@Getter
@Setter
public class AddAccessWindowDto {

    @NotBlank(message = "请输入免密窗口名称")
    @Size(max = 64, message = "免密窗口名称不能超过 64 个字符")
    private String name;

    /** 按本请求构建待保存的免密窗口实体 */
    public AppAccessWindow toSave(Long appId, String windowKey) {
        LocalDateTime now = LocalDateTime.now();
        return AppAccessWindow.builder()
                .appId(appId)
                .name(name.trim())
                .windowKey(windowKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
