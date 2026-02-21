package com.lin.webtemplate.infrastructure.repository;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.dataobject.AdminUserDO;
import com.lin.webtemplate.infrastructure.mapper.AdminUserMapper;

/**
 * 功能：后台管理员账号仓储，封装登录账号读取与维护的数据访问。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Repository
public class AdminUserRepository {

    @Resource
    private AdminUserMapper adminUserMapper;

    public AdminUserDO findByUsername(String username) {
        return adminUserMapper.selectByUsername(username);
    }

    public Long insert(AdminUserDO adminUserDO) {
        adminUserMapper.insert(adminUserDO);
        return adminUserDO.getId();
    }

    public void updateLastLoginAt(Long id) {
        adminUserMapper.updateLastLoginAt(id);
    }

    public void updateCredentialsAndStatus(Long id,
                                           String passwordHash,
                                           String status) {
        adminUserMapper.updateCredentialsAndStatus(id, passwordHash, status);
    }

}
