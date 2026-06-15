package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.PublicRegisterRequest;
import com.archive.dto.response.PublicRegisterResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.enums.DataScope;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 公众注册（§3.4）。
 * 校验短信验证码 → 手机号唯一 → 创建 userType=public 账号 → 绑定 public_user 角色 → 审计。
 */
@Service
@RequiredArgsConstructor
public class PublicRegistrationService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final SmsCodeService smsCodeService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public PublicRegisterResponse register(PublicRegisterRequest req) {
        if (!smsCodeService.verify(req.getPhone(), req.getSmsCode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "验证码错误或已失效");
        }

        // 手机号唯一（含未删除账号）
        User existing = userMapper.selectOne(new QueryWrapper<User>()
                .eq("phone", req.getPhone())
                .isNull("deleted_at")
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "该手机号已注册");
        }

        User user = new User();
        user.setUserType(UserType.public_);
        user.setLoginName(req.getPhone()); // 公众账号以手机号为登录名
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setMaxSecurityLevel(0);
        user.setDataScope(DataScope.own_org);
        user.setStatus(UserStatus.active);
        userMapper.insert(user);

        bindPublicUserRole(user.getId());

        auditService.log("M01", "register", "user", user.getId(),
                Map.of("phone", req.getPhone(), "realName", req.getRealName() != null ? req.getRealName() : ""));

        PublicRegisterResponse vo = new PublicRegisterResponse();
        vo.setId(user.getId());
        vo.setLoginName(user.getLoginName());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setUserType(user.getUserType().getValue());
        vo.setStatus(user.getStatus().getValue());
        return vo;
    }

    private void bindPublicUserRole(Long userId) {
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_code", "public_user"));
        if (role == null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "系统未配置 public_user 角色");
        }
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id, created_at) VALUES (?, ?, now())",
                userId, role.getId());
    }
}
