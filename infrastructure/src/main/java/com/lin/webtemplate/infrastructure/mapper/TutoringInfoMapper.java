package com.lin.webtemplate.infrastructure.mapper;

import com.lin.webtemplate.infrastructure.entity.TutoringInfoEntity;
import com.lin.webtemplate.infrastructure.repository.TutoringInfoQueryCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 功能：结构化家教信息 Mapper，提供 upsert、分页与详情查询能力。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface TutoringInfoMapper {

    int upsert(TutoringInfoEntity entity);

    TutoringInfoEntity findById(@Param("id") Long id);

    List<TutoringInfoEntity> queryPage(@Param("condition") TutoringInfoQueryCondition condition,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    long count(@Param("condition") TutoringInfoQueryCondition condition);
}
