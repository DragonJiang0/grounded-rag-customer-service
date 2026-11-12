package com.zidong.manage.controller;

import com.zidong.dao.dto.ChangePasswordDto;
import com.zidong.manage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    /** 修改当前登录用户密码 */
    @PostMapping("/changePassword")
    public void changePassword(@RequestBody ChangePasswordDto request) {
        // 密码不打印到日志
        requestLog();
        validate(request);
        userService.changePassword(request);
    }
}
