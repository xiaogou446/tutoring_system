package com.lin.webtemplate.infrastructure.repository;

import com.lin.webtemplate.infrastructure.entity.TutoringInfoEntity;
import com.lin.webtemplate.infrastructure.mapper.TutoringInfoMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 功能：结构化家教信息仓储，封装查询与落库访问。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class TutoringInfoRepository {

    private final TutoringInfoMapper tutoringInfoMapper;

    public TutoringInfoRepository(TutoringInfoMapper tutoringInfoMapper) {
        this.tutoringInfoMapper = tutoringInfoMapper;
    }

    public void upsert(TutoringInfoEntity entity) {
        tutoringInfoMapper.upsert(entity);
    }

    public TutoringInfoEntity findById(Long id) {
        return tutoringInfoMapper.findById(id);
    }

    public List<TutoringInfoEntity> queryPage(TutoringInfoQueryCondition condition, int offset, int limit) {
        return tutoringInfoMapper.queryPage(condition, offset, limit);
    }

    public long count(TutoringInfoQueryCondition condition) {
        return tutoringInfoMapper.count(condition);
    }
}
