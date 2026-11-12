package com.zidong.dao.dto;

import com.zidong.dao.entity.App;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 应用的编辑请求 */
@Getter
@Setter
public class EditAppDto extends AddAppDto {

    @NotNull(message = "缺少应用 ID")
    private Long id;

    /** 把本请求的字段写到已查出的实体上 */
    public App update(App app) {
        app.setName(getName().trim());
        app.setDescription(descriptionOrEmpty());
        // 开关为原始 boolean，Lombok 生成的读方法是 isMultiAgentEnabled；以提交值为准
        app.setMultiAgentEnabled(isMultiAgentEnabled());
        app.setUpdatedAt(LocalDateTime.now());
        return app;
    }
}
