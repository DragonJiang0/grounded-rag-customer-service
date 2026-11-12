package com.zidong.manage.controller;

import com.zidong.dao.dto.AddCorpusGroupDto;
import com.zidong.dao.dto.EditCorpusGroupDto;
import com.zidong.dao.entity.CorpusGroup;
import com.zidong.manage.service.CorpusGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 语料分组 */
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class CorpusGroupController extends BaseController {

    private final CorpusGroupService groupService;


    @GetMapping("/all")
    public List<CorpusGroup> listAll() {
        requestLog();
        return groupService.listAll();
    }

    @PostMapping("/add")
    public void create(@RequestBody AddCorpusGroupDto request) {
        requestLog(request);
        validate(request);
        groupService.create(request);
    }

    @PostMapping("/edit")
    public CorpusGroup update(@RequestBody EditCorpusGroupDto request) {
        requestLog(request);
        validate(request);
        return groupService.update(request.getId(), request);
    }

    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        requestLog(id);
        groupService.delete(id);
    }
}
