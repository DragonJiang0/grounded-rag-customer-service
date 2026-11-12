package com.zidong.manage.service;

import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.ChangePasswordDto;
import com.zidong.dao.entity.SysUser;
import com.zidong.dao.mapper.SysUserMapper;
import com.zidong.manage.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 用户管理 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 修改当前登录用户的密码：原密码需与库中密文匹配 */
    @Transactional
    public void changePassword(ChangePasswordDto request) {
        Long userId = SecurityUtils.currentUser().getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BizException.badRequest("原密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
