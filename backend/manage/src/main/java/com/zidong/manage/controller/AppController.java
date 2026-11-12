package com.zidong.manage.controller;

import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.AddAppDto;
import com.zidong.dao.dto.EditAppDto;
import com.zidong.dao.vo.AppDetailVO;
import com.zidong.dao.vo.AppVO;
import com.zidong.manage.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 应用管理 */
@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
public class AppController extends BaseController {

    private final AppService appService;


    /** 应用列表 */
    @GetMapping("/listAll")
    public List<AppVO> listAll() {
        requestLog();
        return appService.listAll();
    }

    /** 应用详情 */
    @GetMapping("/detail/{id}")
    public AppDetailVO detail(@PathVariable Long id) {
        requestLog(id);
        return appService.detail(id);
    }

    /** 新建应用 */
    @PostMapping("/add")
    public void create(@RequestBody AddAppDto request) {
        requestLog(request);
        validate(request);
        checkAgents(request);
        appService.create(request);
    }

    /** 编辑应用 */
    @PostMapping("/edit")
    public void update(@RequestBody EditAppDto request) {
        requestLog(request);
        validate(request);
        checkAgents(request);
        appService.update(request);
    }

    /** 删除应用 */
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        requestLog(id);
        appService.delete(id);
    }

    /**
     * 多 Agent 开关与 Agent 选择的联动校验。
     *
     * <p>「开关开启时 agents 必填」是跨字段约束，`validate` 的 bean validation 覆盖不到，单独调一次。
     */
    private void checkAgents(AddAppDto request) {
        String message = request.checkMultiAgent();
        if (message != null) {
            throw new BizException(message);
        }
    }
}
