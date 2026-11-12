package com.zidong.dao.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 微信个人号「扫码人」更新请求：按凭证 ID 覆盖展示名称，允许传空串清空 */
@Getter
@Setter
public class UpdateBotCredentialScannerDto {

    /** 扫码人展示名称，可为空串（表示清空），最长 64 个字符 */
    @Size(max = 64, message = "扫码人不能超过 64 个字符")
    private String scannerName;
}
