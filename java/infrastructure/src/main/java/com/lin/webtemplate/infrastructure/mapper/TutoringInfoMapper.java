package com.lin.webtemplate.infrastructure.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lin.webtemplate.infrastructure.dataobject.TutoringInfoDO;

/**
 * 功能：结构化家教信息表的 MyBatis Mapper（XML 定义 SQL）。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface TutoringInfoMapper {

    long countByCondition(@Param("contentKeyword") String contentKeyword,
                          @Param("gradeKeyword") String gradeKeyword,
                          @Param("subjectKeyword") String subjectKeyword,
                          @Param("addressKeyword") String addressKeyword,
                          @Param("timeKeyword") String timeKeyword,
                          @Param("salaryKeyword") String salaryKeyword,
                          @Param("teacherKeyword") String teacherKeyword);

    List<TutoringInfoDO> pageQuery(@Param("contentKeyword") String contentKeyword,
                                   @Param("gradeKeyword") String gradeKeyword,
                                   @Param("subjectKeyword") String subjectKeyword,
                                   @Param("addressKeyword") String addressKeyword,
                                   @Param("timeKeyword") String timeKeyword,
                                   @Param("salaryKeyword") String salaryKeyword,
                                   @Param("teacherKeyword") String teacherKeyword,
                                   @Param("sortOrder") String sortOrder,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);
}
