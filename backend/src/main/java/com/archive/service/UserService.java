package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.UserCreateRequest;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageResult<UserInfoResponse> listUsers(int pageNo, int pageSize, String keyword, String status) {
        Page<User> page = new Page<>(pageNo, pageSize);
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("real_name", keyword)
                    .or().like("login_name", keyword)
                    .or().like("phone", keyword));
        }
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        qw.orderByDesc("created_at");

        Page<User> result = userMapper.selectPage(page, qw);
        List<UserInfoResponse> voList = result.getRecords().stream()
                .map(this::toUserInfoResponse)
                .collect(Collectors.toList());

        return new PageResult<>(voList, pageNo, pageSize, result.getTotal());
    }

    @Transactional
    public UserInfoResponse createUser(UserCreateRequest req) {
        User existing = userMapper.selectOne(
                new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "登录名已存在");
        }

        User user = new User();
        user.setUserType(UserType.valueOf(req.getUserType()));
        user.setLoginName(req.getLoginName());
        user.setEmployeeNo(req.getEmployeeNo());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setOrganizationId(req.getOrganizationId());
        user.setDepartmentName(req.getDepartmentName());
        user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        user.setDataScope(DataScope.valueOf(req.getDataScope()));
        user.setStatus(UserStatus.active);

        userMapper.insert(user);
        return toUserInfoResponse(user);
    }

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
        if (req.getStatus() != null) user.setStatus(UserStatus.valueOf(req.getStatus()));

        userMapper.updateById(user);
        return toUserInfoResponse(user);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword != null ? newPassword : "123456"));
        userMapper.updateById(user);
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

        List<UserRole> urs = userRoleMapper.selectList(
                new QueryWrapper<UserRole>().eq("user_id", user.getId()));
        if (!urs.isEmpty()) {
            List<Short> roleIds = urs.stream().map(UserRole::getRoleId).toList();
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        } else {
            vo.setRoles(List.of());
        }
        return vo;
    }
}
