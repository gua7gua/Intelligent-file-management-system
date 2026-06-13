package com.archive.common;

import cn.dev33.satoken.stp.StpUtil;
import com.archive.enums.DataScope;
import com.archive.enums.RoleCode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当前用户上下文工具类。
 */
public class AuthContext {

    private AuthContext() {}

    private static final String SESSION_KEY_ROLES = "roles";
    private static final String SESSION_KEY_MAX_SECURITY_LEVEL = "maxSecurityLevel";
    private static final String SESSION_KEY_DATA_SCOPE = "dataScope";
    private static final String SESSION_KEY_ORGANIZATION_ID = "organizationId";
    private static final String SESSION_KEY_USER_TYPE = "userType";

    public static long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    public static boolean isAuthenticated() {
        return StpUtil.isLogin();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        return (List<String>) StpUtil.getSession().get(SESSION_KEY_ROLES);
    }

    public static Set<RoleCode> getCurrentUserRoleCodes() {
        List<String> roleStrings = getCurrentUserRoles();
        return roleStrings.stream()
                .map(RoleCode::valueOf)
                .collect(Collectors.toSet());
    }

    public static boolean hasRole(RoleCode roleCode) {
        return getCurrentUserRoleCodes().contains(roleCode);
    }

    public static boolean isAdmin() {
        Set<RoleCode> roles = getCurrentUserRoleCodes();
        return roles.contains(RoleCode.front_archivist)
                || roles.contains(RoleCode.back_archivist)
                || roles.contains(RoleCode.sys_admin);
    }

    public static int getMaxSecurityLevel() {
        Object val = StpUtil.getSession().get(SESSION_KEY_MAX_SECURITY_LEVEL);
        return val != null ? (int) val : 0;
    }

    public static DataScope getDataScope() {
        Object val = StpUtil.getSession().get(SESSION_KEY_DATA_SCOPE);
        if (val == null) return DataScope.own_org;
        if (val instanceof DataScope ds) return ds;
        // AuthService 以 name() 字符串存入 session，需兼容字符串
        if (val instanceof String s) {
            try { return DataScope.valueOf(s); } catch (IllegalArgumentException e) { return DataScope.own_org; }
        }
        return DataScope.own_org;
    }

    public static Long getOrganizationId() {
        Object val = StpUtil.getSession().get(SESSION_KEY_ORGANIZATION_ID);
        return val != null ? (Long) val : null;
    }

    public static String getUserType() {
        Object val = StpUtil.getSession().get(SESSION_KEY_USER_TYPE);
        return val != null ? (String) val : null;
    }
}
