package com.zidong.dao.dto;

import com.zidong.dao.entity.AppPasswordWindow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 账号密码窗口的新增请求 */
@Getter
@Setter
public class AddPasswordWindowDto {

    @NotBlank(message = "请输入窗口名称")
    @Size(max = 64, message = "窗口名称不能超过 64 个字符")
    private String name;

    @NotBlank(message = "请输入登录账号")
    @Size(max = 64, message = "登录账号不能超过 64 个字符")
    private String username;

    /**
     * 按本请求构建待保存的账号密码窗口实体。
     *
     * <p>密码不由前端传入，由服务端初始化后以密文传入，窗口创建后即为启用状态。
     *
     * @param appId           所属应用 ID
     * @param encodedPassword 已加密的初始密码
     */
    public AppPasswordWindow toSave(Long appId, String encodedPassword) {
        LocalDateTime now = LocalDateTime.now();
        return AppPasswordWindow.builder()
                .appId(appId)
                .name(name.trim())
                .username(username.trim())
                .password(encodedPassword)
                .status(AppPasswordWindow.STATUS_ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
