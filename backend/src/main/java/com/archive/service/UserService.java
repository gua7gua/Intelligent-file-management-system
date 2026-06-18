package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.DataScope;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.archive.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ACTIVE_SYS_ADMIN_COUNT_SQL =
            "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'sys_admin' AND u.status = 'active'";

    private static final String RECENT_OPS_SQL =
            "SELECT module_name, operation_type, operated_at FROM audit_logs " +
            "WHERE actor_user_id = ? ORDER BY operated_at DESC LIMIT 10";

    // 删除前业务关联检查：借阅申请 / 审批申请 / 移交征集批次（含 transfer 与 collection 两种来源）
    private static final String BORROW_LINK_SQL =
            "SELECT COUNT(*) FROM borrow_requests WHERE borrower_id = ? AND deleted_at IS NULL";
    private static final String APPROVAL_LINK_SQL =
            "SELECT COUNT(*) FROM approval_requests WHERE (submitted_by = ? OR approved_by = ?) AND deleted_at IS NULL";
    private static final String INTAKE_LINK_SQL =
            "SELECT COUNT(*) FROM intake_batches WHERE (public_user_id = ? OR accepted_by = ? OR created_by = ?) " +
            "AND deleted_at IS NULL";

    // ==================== 22.1 查询用户 ====================

    public PageResult<UserInfoResponse> listUsers(int pageNo, int pageSize, String keyword, String status,
                                                   String userType, String roleCode, Long organizationId) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("real_name", keyword)
                    .or().like("login_name", keyword)
                    .or().like("phone", keyword)
                    .or().like("employee_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        if (userType != null && !userType.isBlank()) {
            qw.eq("user_type", userType);
        }
        if (organizationId != null) {
            qw.eq("organization_id", organizationId);
        }
        if (roleCode != null && !roleCode.isBlank()) {
            List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>()
                    .inSql("role_id", "SELECT id FROM roles WHERE role_code = '"
                            + roleCode.replace("'", "") + "'"));
            if (urs.isEmpty()) {
                return new PageResult<>(List.of(), pageNo, pageSize, 0L);
            }
            List<Long> userIds = urs.stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
            qw.in("id", userIds);
        }
        qw.orderByDesc("created_at");

        Page<User> result = userMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        List<UserInfoResponse> voList = result.getRecords().stream()
                .map(this::toUserInfoResponse)
                .collect(Collectors.toList());
        return new PageResult<>(voList, pageNo, pageSize, result.getTotal());
    }

    // ==================== 22.2 创建用户 ====================

    @Transactional
    public UserInfoResponse createUser(UserCreateRequest req) {
        User existing = userMapper.selectOne(new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "登录名已存在");
        }

        User user = new User();
        user.setUserType(UserType.valueOf(req.getUserType()));
        user.setLoginName(req.getLoginName());
        user.setEmployeeNo(req.getEmployeeNo());
        user.setPhone(req.getPhone());
        String password = req.resolvePassword();
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRealName(req.getRealName());
        user.setOrganizationId(req.getOrganizationId());
        user.setDepartmentName(req.getDepartmentName());
        user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        user.setDataScope(req.getDataScope() != null ? DataScope.valueOf(req.getDataScope()) : DataScope.own_org);
        user.setStatus(UserStatus.active);
        userMapper.insert(user);

        bindRoles(user.getId(), req.getRoleCodes());

        auditService.log("M14", "create_user", "user", user.getId(),
                Map.of("loginName", user.getLoginName()));
        return toUserInfoResponse(user);
    }

    // ==================== 22.4 更新用户 ====================

    @Transactional
    public UserInfoResponse updateUser(Long id, UserUpdateRequest req) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getOrganizationId() != null) user.setOrganizationId(req.getOrganizationId());
        if (req.getDepartmentName() != null) user.setDepartmentName(req.getDepartmentName());
        if (req.getMaxSecurityLevel() != null) user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        if (req.getDataScope() != null) user.setDataScope(DataScope.valueOf(req.getDataScope()));
        userMapper.updateById(user);

        if (req.getRoleCodes() != null) {
            rebindRoles(id, req.getRoleCodes());
        }
        auditService.log("M14", "update_user", "user", id, Map.of());
        return toUserInfoResponse(user);
    }

    // ==================== 22.3 用户详情 ====================

    public UserInfoResponse getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserInfoResponse vo = toUserInfoResponse(user);
        vo.setRecentOperations(loadRecentOperations(id));
        return vo;
    }

    // ==================== 22.5 启用/禁用 ====================

    @Transactional
    public UserInfoResponse updateStatus(Long id, UserStatusRequest req) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if ("disabled".equals(req.getStatus()) && isLastActiveSysAdmin(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "不能禁用最后一个启用的系统管理员");
        }
        user.setStatus(UserStatus.valueOf(req.getStatus()));
        userMapper.updateById(user);

        auditService.log("M14", "update_user_status", "user", id,
                Map.of("status", req.getStatus(),
                        "reason", req.getReason() != null ? req.getReason() : ""));
        return toUserInfoResponse(user);
    }

    // ==================== 22.6 重置密码 ====================

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword != null ? newPassword : "123456"));
        userMapper.updateById(user);
        auditService.log("M14", "reset_password", "user", id, Map.of());
    }

    // ==================== 22.7 删除用户（硬删除 + 留痕，D1） ====================

    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() != UserStatus.disabled) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "仅已禁用账号可删除");
        }
        if (hasBusinessLinks(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "账号存在业务关联，无法删除");
        }
        // 留痕：先采集被删用户身份写入审计 detail（操作人由 AuditService 自动取当前管理员），
        // 审计记录 business_id 存被删用户 id（无外键），即「谁删了谁」。
        Map<String, Object> detail = new HashMap<>();
        detail.put("deletedLoginName", user.getLoginName());
        detail.put("deletedRealName", user.getRealName());
        detail.put("deletedPhone", user.getPhone());

        try {
            // 硬删除：User 无 @TableLogic，deleteById 即物理删除；
            // user_roles 随 ON DELETE CASCADE 清理；audit_logs/archive_access_logs 经 V22 改 ON DELETE SET NULL 保留痕迹。
            userMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // 兜底：仍有未被 hasBusinessLinks 显式覆盖的业务表外键引用（如 archives.created_by），转为友好提示
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "账号存在业务关联，无法删除");
        }
        auditService.log("M14", "delete_user", "user", id, detail);
    }

    /** 校验该用户是否仍存在未软删的业务关联（借阅 / 审批 / 移交征集）。 */
    private boolean hasBusinessLinks(Long userId) {
        Long borrow = jdbcTemplate.queryForObject(BORROW_LINK_SQL, Long.class, userId);
        if (borrow != null && borrow > 0) return true;
        Long approval = jdbcTemplate.queryForObject(APPROVAL_LINK_SQL, Long.class, userId, userId);
        if (approval != null && approval > 0) return true;
        Long intake = jdbcTemplate.queryForObject(INTAKE_LINK_SQL, Long.class, userId, userId, userId);
        return intake != null && intake > 0;
    }

    // ==================== 角色绑定辅助（user_roles 无 @TableId，用 JdbcTemplate 操作） ====================

    private void bindRoles(Long userId, List<String> roleCodes) {
        if (userId == null || roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        for (String code : roleCodes) {
            Role role = mustGetRole(code);
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id, created_at) VALUES (?, ?, now())",
                    userId, role.getId());
        }
    }

    private void rebindRoles(Long userId, List<String> roleCodes) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        bindRoles(userId, roleCodes);
    }

    private Role mustGetRole(String roleCode) {
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_code", roleCode));
        if (role == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不存在或已禁用: " + roleCode);
        }
        return role;
    }

    /** 判断禁用该用户是否会使启用中的 sys_admin 归零。 */
    private boolean isLastActiveSysAdmin(Long userId) {
        List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", userId));
        if (urs.isEmpty()) {
            return false;
        }
        List<Short> roleIds = urs.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        boolean isAdmin = roles.stream().anyMatch(r -> "sys_admin".equals(r.getRoleCode()));
        if (!isAdmin) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(ACTIVE_SYS_ADMIN_COUNT_SQL, Long.class);
        return count != null && count <= 1;
    }

    private List<UserInfoResponse.RecentOperation> loadRecentOperations(Long userId) {
        try {
            return jdbcTemplate.query(RECENT_OPS_SQL, (rs, i) -> {
                UserInfoResponse.RecentOperation op = new UserInfoResponse.RecentOperation();
                op.setModuleName(rs.getString("module_name"));
                op.setOperationType(rs.getString("operation_type"));
                java.sql.Timestamp ts = rs.getTimestamp("operated_at");
                op.setOperatedAt(ts != null ? ts.toInstant().atOffset(java.time.ZoneOffset.UTC) : null);
                return op;
            }, userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        UserInfoResponse vo = new UserInfoResponse();
        vo.setId(user.getId());
        vo.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        vo.setLoginName(user.getLoginName());
        vo.setEmployeeNo(user.getEmployeeNo());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setOrganizationId(user.getOrganizationId());
        vo.setDepartmentName(user.getDepartmentName());
        vo.setMaxSecurityLevel(user.getMaxSecurityLevel());
        vo.setDataScope(user.getDataScope() != null ? user.getDataScope().name() : null);
        vo.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        vo.setCreatedAt(user.getCreatedAt());

        List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", user.getId()));
        if (!urs.isEmpty()) {
            List<Short> roleIds = urs.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        } else {
            vo.setRoles(List.of());
        }
        return vo;
    }
}
