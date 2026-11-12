package com.zidong.manage.controller;

import com.zidong.common.result.PageResult;
import com.zidong.dao.dto.AddCorpusDto;
import com.zidong.dao.dto.CorpusPageQueryDto;
import com.zidong.dao.dto.EditCorpusDto;
import com.zidong.dao.vo.CorpusVO;
import com.zidong.manage.service.CorpusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 语料管理 */
@RestController
@RequestMapping("/corpus")
@RequiredArgsConstructor
public class CorpusController extends BaseController {

    private final CorpusService corpusService;


    /** 分页查询 */
    @PostMapping("/page")
    public PageResult<CorpusVO> page(@RequestBody CorpusPageQueryDto request) {
        requestLog(request);
        validate(request);
        return corpusService.page(request);
    }

    @PostMapping("/add")
    public void create(@RequestBody AddCorpusDto request) {
        requestLog(request);
        validate(request);
        corpusService.create(request);
    }

    @PostMapping("/edit")
    public void update(@RequestBody EditCorpusDto request) {
        requestLog(request);
        validate(request);
        corpusService.update(request);
    }

    /** 切换启用状态 */
    @PostMapping("/toggle-enabled/{id}")
    public Boolean toggleEnabled(@PathVariable Long id) {
        requestLog(id);
        return corpusService.toggleEnabled(id);
    }

    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        requestLog(id);
        corpusService.delete(id);
    }
}
