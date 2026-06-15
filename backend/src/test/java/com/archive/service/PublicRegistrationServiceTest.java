package com.archive.service;

import com.archive.dto.request.PublicRegisterRequest;
import com.archive.dto.response.PublicRegisterResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublicRegistrationServiceTest {

    private PublicRegistrationService service;
    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private SmsCodeService smsCodeService;
    private AuditService auditService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        smsCodeService = mock(SmsCodeService.class);
        auditService = mock(AuditService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new PublicRegistrationService(userMapper, roleMapper, smsCodeService, auditService, jdbcTemplate);
    }

    @Test
    void register_成功创建公众账号并绑定角色() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(100L);
            return 1;
        });
        Role role = new Role();
        role.setId((short) 6);
        role.setRoleCode("public_user");
        when(roleMapper.selectOne(any())).thenReturn(role);

        PublicRegisterResponse resp = service.register(req("18877600249", "1357", "pass1234", "测试用户"));

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getUserType()).isEqualTo("public");
        assertThat(resp.getLoginName()).isEqualTo("18877600249");
        verify(userMapper).insert(any(User.class));
        verify(jdbcTemplate).update(contains("INSERT INTO user_roles"), eq(100L), any());
        verify(auditService).log(eq("M01"), eq("register"), eq("user"), eq(100L), any());
    }

    @Test
    void register_验证码错误抛校验失败() {
        when(smsCodeService.verify(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.register(req("18877600249", "0000", "pass1234", "x")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_手机号已注册抛冲突() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        User exist = new User();
        exist.setId(1L);
        exist.setPhone("18877600249");
        when(userMapper.selectOne(any())).thenReturn(exist);

        assertThatThrownBy(() -> service.register(req("18877600249", "1357", "pass1234", "x")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已注册");
        verify(userMapper, never()).insert(any(User.class));
    }

    private PublicRegisterRequest req(String phone, String code, String pwd, String realName) {
        PublicRegisterRequest r = new PublicRegisterRequest();
        r.setPhone(phone);
        r.setSmsCode(code);
        r.setPassword(pwd);
        r.setRealName(realName);
        return r;
    }
}
