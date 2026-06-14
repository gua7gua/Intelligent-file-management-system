package com.archive.service;

import com.archive.dto.response.RoleResponse;
import com.archive.entity.Role;
import com.archive.enums.RoleCode;
import com.archive.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 预设角色查询。本期不做逐按钮自定义授权。 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<RoleResponse> listRoles() {
        List<Role> roles = roleMapper.selectList(null);
        return roles.stream().map(r -> {
            RoleResponse resp = new RoleResponse();
            resp.setId(r.getId() != null ? r.getId().longValue() : null);
            resp.setRoleCode(r.getRoleCode());
            resp.setRoleName(resolveDisplayName(r));
            resp.setDescription(r.getDescription());
            return resp;
        }).collect(Collectors.toList());
    }

    private String resolveDisplayName(Role r) {
        if (r.getRoleName() != null && !r.getRoleName().isBlank()) {
            return r.getRoleName();
        }
        try {
            return RoleCode.valueOf(r.getRoleCode()).getDisplayName();
        } catch (Exception e) {
            return r.getRoleCode();
        }
    }
}
