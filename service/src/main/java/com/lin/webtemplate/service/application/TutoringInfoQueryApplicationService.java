package com.lin.webtemplate.service.application;

import com.lin.webtemplate.facade.dto.TutoringInfoPageView;
import com.lin.webtemplate.facade.dto.TutoringInfoView;
import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import com.lin.webtemplate.infrastructure.entity.TutoringInfoEntity;
import com.lin.webtemplate.infrastructure.repository.ArticleRawRepository;
import com.lin.webtemplate.infrastructure.repository.TutoringInfoQueryCondition;
import com.lin.webtemplate.infrastructure.repository.TutoringInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能：结构化家教查询应用服务，负责解析同步、分页检索与详情读取。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Service
public class TutoringInfoQueryApplicationService {

    private static final DateTimeFormatter OUTPUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArticleRawRepository articleRawRepository;

    private final TutoringInfoRepository tutoringInfoRepository;

    private final TutoringInfoParseService tutoringInfoParseService;

    public TutoringInfoQueryApplicationService(ArticleRawRepository articleRawRepository,
                                               TutoringInfoRepository tutoringInfoRepository,
                                               TutoringInfoParseService tutoringInfoParseService) {
        this.articleRawRepository = articleRawRepository;
        this.tutoringInfoRepository = tutoringInfoRepository;
        this.tutoringInfoParseService = tutoringInfoParseService;
    }

    @Transactional
    public TutoringInfoPageView queryPage(String keyword,
                                          String district,
                                          String grade,
                                          String subject,
                                          String sort,
                                          int pageNo,
                                          int pageSize) {
        syncAllFromRaw();

        int safePageNo = pageNo <= 0 ? 1 : pageNo;
        int safePageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNo - 1) * safePageSize;

        TutoringInfoQueryCondition condition = new TutoringInfoQueryCondition();
        condition.setKeyword(trimToNull(keyword));
        condition.setDistrict(trimToNull(district));
        condition.setGrade(trimToNull(grade));
        condition.setSubject(trimToNull(subject));
        condition.setSort(normalizeSort(sort));

        long total = tutoringInfoRepository.count(condition);
        List<TutoringInfoEntity> entities = tutoringInfoRepository.queryPage(condition, offset, safePageSize);

        TutoringInfoPageView pageView = new TutoringInfoPageView();
        pageView.setTotal(total);
        pageView.setPageNo(safePageNo);
        pageView.setPageSize(safePageSize);
        pageView.setRecords(toViews(entities));
        return pageView;
    }

    @Transactional
    public TutoringInfoView queryDetail(Long id) {
        if (id == null || id <= 0) {
            return null;
        }

        syncOneFromRaw(id);
        TutoringInfoEntity entity = tutoringInfoRepository.findById(id);
        if (entity == null) {
            return null;
        }
        return toView(entity);
    }

    private void syncAllFromRaw() {
        List<ArticleRawEntity> rawEntities = articleRawRepository.findAll();
        for (ArticleRawEntity rawEntity : rawEntities) {
            tutoringInfoRepository.upsert(tutoringInfoParseService.parse(rawEntity));
        }
    }

    private void syncOneFromRaw(Long id) {
        ArticleRawEntity rawEntity = articleRawRepository.findById(id);
        if (rawEntity == null) {
            return;
        }
        tutoringInfoRepository.upsert(tutoringInfoParseService.parse(rawEntity));
    }

    private List<TutoringInfoView> toViews(List<TutoringInfoEntity> entities) {
        List<TutoringInfoView> views = new ArrayList<>();
        for (TutoringInfoEntity entity : entities) {
            views.add(toView(entity));
        }
        return views;
    }

    private TutoringInfoView toView(TutoringInfoEntity entity) {
        TutoringInfoView view = new TutoringInfoView();
        view.setId(entity.getId());
        view.setSourceUrl(entity.getSourceUrl());
        view.setTitle(entity.getTitle());
        view.setPublishTime(entity.getPublishTime() == null ? null : entity.getPublishTime().format(OUTPUT_TIME));
        view.setContentText(entity.getContentText());
        view.setDistrict(entity.getDistrict());
        view.setGrade(entity.getGrade());
        view.setSubject(entity.getSubject());
        view.setSalary(entity.getSalary());
        view.setTeacherGender(entity.getTeacherGender());
        view.setContact(entity.getContact());
        return view;
    }

    private String normalizeSort(String sort) {
        if ("salary_high".equals(sort) || "salary_low".equals(sort)) {
            return sort;
        }
        return "latest";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
