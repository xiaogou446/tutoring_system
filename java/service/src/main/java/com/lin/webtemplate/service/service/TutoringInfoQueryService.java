package com.lin.webtemplate.service.service;

import java.util.List;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.lin.webtemplate.infrastructure.dataobject.TutoringInfoDO;
import com.lin.webtemplate.infrastructure.repository.TutoringInfoRepository;
import com.lin.webtemplate.service.dto.TutoringInfoPageQueryDTO;
import com.lin.webtemplate.service.model.TutoringInfoItemModel;
import com.lin.webtemplate.service.model.TutoringInfoPageModel;

/**
 * 功能：家教信息分页检索服务，承接 H5 查询参数并组装返回模型。
 *
 * 服务层负责分页参数兜底、排序规范化以及数据映射，
 * 保证 Controller 保持薄层且 SQL 细节集中在基础设施层。
 *
 * @author linyi
 * @since 2026-02-17
 */
@Slf4j
@Service
public class TutoringInfoQueryService {

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private TutoringInfoRepository tutoringInfoRepository;

    public TutoringInfoPageModel pageQuery(TutoringInfoPageQueryDTO queryDTO) {
        int pageNo = normalizePageNo(queryDTO.getPageNo());
        int pageSize = normalizePageSize(queryDTO.getPageSize());
        int offset = (pageNo - 1) * pageSize;
        String sortOrder = normalizeSortOrder(queryDTO.getSortOrder());

        String contentKeyword = normalizeKeyword(queryDTO.getContentKeyword());
        String gradeKeyword = normalizeKeyword(queryDTO.getGradeKeyword());
        String subjectKeyword = normalizeKeyword(queryDTO.getSubjectKeyword());
        String addressKeyword = normalizeKeyword(queryDTO.getAddressKeyword());
        String timeKeyword = normalizeKeyword(queryDTO.getTimeKeyword());
        String salaryKeyword = normalizeKeyword(queryDTO.getSalaryKeyword());
        String teacherKeyword = normalizeKeyword(queryDTO.getTeacherKeyword());

        long total = tutoringInfoRepository.countByCondition(contentKeyword, gradeKeyword, subjectKeyword,
                addressKeyword, timeKeyword, salaryKeyword, teacherKeyword);

        List<TutoringInfoDO> queryResult = tutoringInfoRepository.pageQuery(contentKeyword, gradeKeyword,
                subjectKeyword, addressKeyword, timeKeyword, salaryKeyword, teacherKeyword,
                sortOrder, offset, pageSize);

        List<TutoringInfoItemModel> itemModels = queryResult.stream().map(this::toItemModel).toList();

        TutoringInfoPageModel pageModel = new TutoringInfoPageModel();
        pageModel.setPageNo(pageNo);
        pageModel.setPageSize(pageSize);
        pageModel.setTotal(total);
        pageModel.setRecords(itemModels);
        return pageModel;
    }

    private TutoringInfoItemModel toItemModel(TutoringInfoDO tutoringInfoDO) {
        TutoringInfoItemModel itemModel = new TutoringInfoItemModel();
        itemModel.setId(tutoringInfoDO.getId());
        itemModel.setSourceUrl(tutoringInfoDO.getSourceUrl());
        itemModel.setContentBlock(tutoringInfoDO.getContentBlock());
        itemModel.setPublishedAt(tutoringInfoDO.getPublishedAt());
        return itemModel;
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeSortOrder(String sortOrder) {
        // 仅允许白名单值，避免将任意字符串传入 SQL 排序分支。
        if ("asc".equalsIgnoreCase(sortOrder)) {
            return "asc";
        }
        return "desc";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        // 统一把空白字符串转为 null，减少 Mapper 条件判断分支。
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return null;
        }
        return trimmedKeyword;
    }
}
