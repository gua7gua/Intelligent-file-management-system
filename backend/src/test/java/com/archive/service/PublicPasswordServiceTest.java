package com.archive.service;

import com.archive.dto.request.ResetPasswordRequest;
import com.archive.entity.User;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublicPasswordServiceTest {

    private PublicPasswordService service;
    private UserMapper userMapper;
    private SmsCodeService smsCodeService;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        smsCodeService = mock(SmsCodeService.class);
        auditService = mock(AuditService.class);
        service = new PublicPasswordService(userMapper, smsCodeService, auditService);
    }

    @Test
    void resetPassword_验证码错误抛校验失败() {
        when(smsCodeService.verify("13800000005", "000000")).thenReturn(false);
        assertThatThrownBy(() -> service.resetPassword(req("13800000005", "000000", "newPass1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetPassword_内部账号拒绝() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        User u = user("13800000005", UserType.internal);
        when(userMapper.selectOne(any())).thenReturn(u);
        assertThatThrownBy(() -> service.resetPassword(req("13800000005", "123456", "newPass1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内部账号");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetPassword_账号不存在() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.resetPassword(req("13800000005", "123456", "newPass1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void resetPassword_公众账号重置成功并写审计() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        User u = user("13800000005", UserType.public_);
        u.setId(5L);
        when(userMapper.selectOne(any())).thenReturn(u);

        service.resetPassword(req("13800000005", "123456", "newPass1"));

        verify(userMapper).updateById(any(User.class));
        verify(auditService).log(eq("M01"), eq("reset_password"), eq("user"), eq(5L), any());
    }

    private ResetPasswordRequest req(String phone, String code, String pwd) {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setPhone(phone);
        r.setSmsCode(code);
        r.setNewPassword(pwd);
        return r;
    }

    private User user(String phone, UserType type) {
        User u = new User();
        u.setPhone(phone);
        u.setUserType(type);
        u.setStatus(UserStatus.active);
        return u;
    }
}
