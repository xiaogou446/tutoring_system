package com.lin.webtemplate.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lin.webtemplate.infrastructure.dataobject.AdminUserDO;

/**
 * 功能：后台管理员账号表的 MyBatis Mapper（XML 定义 SQL）。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Mapper
public interface AdminUserMapper {

    AdminUserDO selectByUsername(@Param("username") String username);

    int insert(AdminUserDO adminUserDO);

    int updateLastLoginAt(@Param("id") Long id);

    int updateCredentialsAndStatus(@Param("id") Long id,
                                   @Param("passwordHash") String passwordHash,
                                   @Param("status") String status);
}
