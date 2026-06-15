package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.ResetPasswordRequest;
import com.archive.entity.User;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicPasswordService {

    private final UserMapper userMapper;
    private final SmsCodeService smsCodeService;
    private final AuditService auditService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 3.6 公众找回密码 */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!smsCodeService.verify(req.getPhone(), req.getSmsCode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "验证码错误或已失效");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("phone", req.getPhone())
                .isNull("deleted_at")
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        if (user.getUserType() == UserType.internal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "内部账号请联系系统管理员重置");
        }
        if (user.getStatus() != null && "disabled".equals(user.getStatus().name())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        auditService.log("M01", "reset_password", "user", user.getId(),
                Map.of("phone", req.getPhone()));
    }
}
