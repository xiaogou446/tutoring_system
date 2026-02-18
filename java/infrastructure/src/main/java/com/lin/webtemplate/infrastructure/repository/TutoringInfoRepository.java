package com.lin.webtemplate.infrastructure.repository;

import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.dataobject.TutoringInfoDO;
import com.lin.webtemplate.infrastructure.mapper.TutoringInfoMapper;

/**
 * 功能：结构化家教信息仓储，提供写入与检索所需的数据库访问能力。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class TutoringInfoRepository {

    @Resource
    private TutoringInfoMapper tutoringInfoMapper;

    public long countByCondition(String contentKeyword,
                                 String gradeKeyword,
                                 String subjectKeyword,
                                 String addressKeyword,
                                 String timeKeyword,
                                 String salaryKeyword,
                                 String teacherKeyword) {
        return tutoringInfoMapper.countByCondition(contentKeyword, gradeKeyword, subjectKeyword,
                addressKeyword, timeKeyword, salaryKeyword, teacherKeyword);
    }

    public List<TutoringInfoDO> pageQuery(String contentKeyword,
                                          String gradeKeyword,
                                          String subjectKeyword,
                                          String addressKeyword,
                                          String timeKeyword,
                                          String salaryKeyword,
                                          String teacherKeyword,
                                          String sortOrder,
                                          int offset,
                                          int limit) {
        return tutoringInfoMapper.pageQuery(contentKeyword, gradeKeyword, subjectKeyword,
                addressKeyword, timeKeyword, salaryKeyword, teacherKeyword, sortOrder, offset, limit);
    }

}
