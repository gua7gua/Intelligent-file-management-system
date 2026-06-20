package com.archive.service;

import cn.dev33.satoken.stp.StpUtil;
import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.dto.request.LoginRequest;
import com.archive.dto.response.LoginResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.archive.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        if (!"active".equals(user.getStatus() != null ? user.getStatus().name() : null)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        List<Role> roles = getUserRoles(user.getId());
        Set<String> roleCodes = roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        validatePortalAccess(req.getPortal(), roleCodes);

        StpUtil.login(user.getId());
        StpUtil.getSession().set("roles", new ArrayList<>(roleCodes));
        // Sa-Token Session 内部为 ConcurrentHashMap，不允许 null 值；公众账号 organizationId 可能为空，需守卫
        StpUtil.getSession().set("maxSecurityLevel", user.getMaxSecurityLevel() != null ? user.getMaxSecurityLevel() : 0);
        StpUtil.getSession().set("dataScope", user.getDataScope() != null ? user.getDataScope().name() : "own_org");
        StpUtil.getSession().set("organizationId", user.getOrganizationId() != null ? user.getOrganizationId() : 0L);
        StpUtil.getSession().set("userType", user.getUserType() != null ? user.getUserType().name() : "internal");

        LoginResponse resp = new LoginResponse();
        resp.setToken(StpUtil.getTokenValue());

        LoginResponse.UserInfoResponse userInfo = new LoginResponse.UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setRealName(user.getRealName());
        userInfo.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        userInfo.setLoginName(user.getLoginName());
        userInfo.setEmployeeNo(user.getEmployeeNo());
        userInfo.setPhone(user.getPhone());
        userInfo.setDepartmentName(user.getDepartmentName());
        userInfo.setRoles(new ArrayList<>(roleCodes));
        userInfo.setOrganizationId(user.getOrganizationId());
        userInfo.setMaxSecurityLevel(user.getMaxSecurityLevel());
        userInfo.setDataScope(user.getDataScope() != null ? user.getDataScope().name() : null);
        resp.setUser(userInfo);
        resp.setDefaultRoute(getDefaultRoute(req.getPortal()));

        return resp;
    }

    public void logout() {
        StpUtil.logout();
    }

    public LoginResponse.UserInfoResponse me() {
        long userId = AuthContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<Role> roles = getUserRoles(userId);

        LoginResponse.UserInfoResponse resp = new LoginResponse.UserInfoResponse();
        resp.setId(user.getId());
        resp.setRealName(user.getRealName());
        resp.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        resp.setLoginName(user.getLoginName());
        resp.setEmployeeNo(user.getEmployeeNo());
        resp.setPhone(user.getPhone());
        resp.setOrganizationId(user.getOrganizationId());
        resp.setDepartmentName(user.getDepartmentName());
        resp.setMaxSecurityLevel(user.getMaxSecurityLevel());
        resp.setDataScope(user.getDataScope() != null ? user.getDataScope().name() : null);
        resp.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        resp.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    private List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new QueryWrapper<UserRole>().eq("user_id", userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Short> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds);
    }

    private void validatePortalAccess(String portal, Set<String> roleCodes) {
        Set<String> allowed = switch (portal) {
            case "admin" -> Set.of(
                    RoleCode.front_archivist.name(),
                    RoleCode.back_archivist.name(),
                    RoleCode.sys_admin.name(),
                    RoleCode.director.name());
            case "transfer" -> Set.of(RoleCode.transfer_user.name());
            case "internal" -> Set.of(RoleCode.internal_reader.name());
            case "public" -> Set.of(RoleCode.public_user.name());
            case "approval" -> Set.of(RoleCode.director.name());
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的门户类型: " + portal);
        };
        if (roleCodes.stream().noneMatch(allowed::contains)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权访问此门户");
        }
    }

    private String getDefaultRoute(String portal) {
        return switch (portal) {
            case "admin" -> "/admin/overview";
            case "transfer" -> "/transfer/dashboard";
            case "internal" -> "/internal/search";
            case "public" -> "/public/search";
            case "approval" -> "/approval/pending";
            default -> "/";
        };
    }
}
