package com.zidong.dao.vo;

import lombok.Getter;
import lombok.Setter;

/** 当前登录用户信息 */
@Getter
@Setter
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;
}
