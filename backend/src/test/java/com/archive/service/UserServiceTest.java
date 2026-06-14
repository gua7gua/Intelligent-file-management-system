package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserStatusRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.UserStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.UserMapper;
import com.archive.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserService service;
    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;
    private AuditService auditService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        auditService = mock(AuditService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new UserService(userMapper, roleMapper, userRoleMapper, auditService, jdbcTemplate);
    }

    @Test
    void createUser_绑定角色并写user_roles() {
        UserCreateRequest req = newUserCreateReq("chen.front", List.of("front_archivist"));
        when(userMapper.selectOne(any())).thenReturn(null);
        // 模拟 MyBatis-Plus insert 回填自增 id
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(1L);
            return 1;
        });
        Role r = new Role();
        r.setId((short) 2);
        r.setRoleCode("front_archivist");
        when(roleMapper.selectOne(any())).thenReturn(r);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        UserInfoResponse resp = service.createUser(req);

        verify(userMapper).insert(any(User.class));
        verify(jdbcTemplate).update(contains("INSERT INTO user_roles"), eq(1L), any());
        assertThat(resp.getLoginName()).isEqualTo("chen.front");
    }

    @Test
    void createUser_角色不存在抛校验失败() {
        UserCreateRequest req = newUserCreateReq("bad.user", List.of("ghost_role"));
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(1L);
            return 1;
        });
        when(roleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void updateStatus_禁用最后一个sys_admin抛冲突() {
        User u = new User();
        u.setId(1L);
        u.setStatus(UserStatus.active);
        when(userMapper.selectById(1L)).thenReturn(u);
        UserRole ur = new UserRole();
        ur.setRoleId((short) 1);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));
        Role sysAdmin = new Role();
        sysAdmin.setId((short) 1);
        sysAdmin.setRoleCode("sys_admin");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(sysAdmin));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        UserStatusRequest req = new UserStatusRequest();
        req.setStatus("disabled");
        req.setReason("离岗");

        assertThatThrownBy(() -> service.updateStatus(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_CONFLICT);
    }

    @Test
    void updateStatus_正常禁用并审计() {
        User u = new User();
        u.setId(1L);
        u.setStatus(UserStatus.active);
        when(userMapper.selectById(1L)).thenReturn(u);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        UserStatusRequest req = new UserStatusRequest();
        req.setStatus("disabled");
        req.setReason("离岗");

        UserInfoResponse resp = service.updateStatus(1L, req);

        assertThat(resp.getStatus()).isEqualTo("disabled");
        verify(userMapper).updateById(any(User.class));
        verify(auditService).log(eq("M14"), eq("update_user_status"), eq("user"), eq(1L), any());
    }

    private UserCreateRequest newUserCreateReq(String loginName, List<String> roles) {
        UserCreateRequest req = new UserCreateRequest();
        req.setUserType("internal");
        req.setLoginName(loginName);
        req.setRealName("小陈");
        req.setPassword("123456");
        req.setDataScope("all");
        req.setRoleCodes(roles);
        return req;
    }
}
