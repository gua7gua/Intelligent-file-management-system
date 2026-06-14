package com.archive.service;

import com.archive.dto.response.RoleResponse;
import com.archive.entity.Role;
import com.archive.mapper.RoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {

    @Test
    void listRoles_返回预设角色及展示名() {
        RoleMapper mapper = mock(RoleMapper.class);
        Role r = new Role();
        r.setId((short) 1);
        r.setRoleCode("sys_admin");
        when(mapper.selectList(null)).thenReturn(List.of(r));

        List<RoleResponse> result = new RoleService(mapper).listRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleCode()).isEqualTo("sys_admin");
        assertThat(result.get(0).getRoleName()).isEqualTo("系统管理员");
    }
}
