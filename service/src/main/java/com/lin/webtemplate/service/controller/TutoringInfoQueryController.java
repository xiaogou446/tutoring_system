package com.lin.webtemplate.service.controller;

import com.lin.webtemplate.facade.dto.Result;
import com.lin.webtemplate.facade.dto.TutoringInfoPageView;
import com.lin.webtemplate.facade.dto.TutoringInfoView;
import com.lin.webtemplate.service.application.TutoringInfoQueryApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 功能：结构化家教查询 API，提供分页检索与详情读取。
 *
 * @author linyi
 * @since 2026-02-16
 */
@RestController
@RequestMapping("/api/tutoring/posts")
public class TutoringInfoQueryController {

    private final TutoringInfoQueryApplicationService tutoringInfoQueryApplicationService;

    public TutoringInfoQueryController(TutoringInfoQueryApplicationService tutoringInfoQueryApplicationService) {
        this.tutoringInfoQueryApplicationService = tutoringInfoQueryApplicationService;
    }

    @GetMapping
    public Result<TutoringInfoPageView> queryPage(@RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "district", required = false) String district,
                                                  @RequestParam(value = "grade", required = false) String grade,
                                                  @RequestParam(value = "subject", required = false) String subject,
                                                  @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
                                                  @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
                                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        TutoringInfoPageView pageView = tutoringInfoQueryApplicationService
                .queryPage(keyword, district, grade, subject, sort, pageNo, pageSize);
        return Result.ok(pageView);
    }

    @GetMapping("/{id}")
    public Result<TutoringInfoView> queryDetail(@PathVariable("id") Long id) {
        TutoringInfoView view = tutoringInfoQueryApplicationService.queryDetail(id);
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到结构化家教信息");
        }
        return Result.ok(view);
    }
}
