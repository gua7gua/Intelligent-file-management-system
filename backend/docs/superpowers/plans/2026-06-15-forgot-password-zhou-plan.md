# 忘记密码（公众找回密码）实施计划（feat/forgot-password-zhou）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 §3.5 / §3.6 后端：`POST /api/public/auth/sms-code`（阿里云短信认证发码）+ `POST /api/public/auth/reset-password`（仅公众账号重置）。验证码全由阿里云托管，后端不存验证码、不建表。

**Architecture:** 抽象 `SmsCodeService` 接口 + 双实现（`AliyunSmsCodeService` 走阿里云 dypnsapi；`FallbackSmsCodeService` 配置缺失时固定码 + 日志），用 `@ConditionalOnProperty` 切换。`PublicPasswordService` 编排重置主流程（校验码 → 仅公众账号 → BCrypt 加密 → 写审计）。内存限流 bean 满足 §3.5 同手机号/IP 限流。匿名访问靠 `/api/public/**` 白名单。

**Tech Stack:** Spring Boot 3.5.3 / Java 17 / Sa-Token 1.44（仅用 BCryptPasswordEncoder，不引 Spring Security）/ 阿里云 `dypnsapi20170525:1.2.3`（Maven Central 已核实）/ JUnit5 + Mockito。

**分支：** 从最新 `develop` 切 `feat/forgot-password-zhou`。

**依据设计：** [2026-06-15-supplement-backend-design.md §2](../specs/2026-06-15-supplement-backend-design.md)

**SDK 类名（经阿里云官方文档核实）：** `com.aliyun.dypnsapi20170525.Client`（`com.aliyun.teaopenapi.models.Config` 构造）、`models.SendSmsVerifyCodeRequest`、`models.CheckSmsVerifyCodeRequest`、响应 `getBody().getVerifyResult()`。实现加依赖后用编译核对最终签名。

---

## 文件结构

| 文件 | 责任 |
|------|------|
| `config/AliyunSmsProperties.java` | `@ConfigurationProperties("aliyun.sms-auth")`：accessKeyId/secret/endpoint/signName/templateCode/enabled |
| `service/SmsCodeService.java` | 接口：`send(phone, scene)` / `verify(phone, code)` |
| `service/AliyunSmsCodeService.java` | 阿里云实现（`@ConditionalOnProperty enabled=true`） |
| `service/FallbackSmsCodeService.java` | 固定码 `123456` + 日志（`@ConditionalOnProperty enabled=false`） |
| `config/SmsRateLimiter.java` | 内存限流：同手机号 60s 冷却、同 IP 每小时 10 次 |
| `service/PublicPasswordService.java` | 重置主流程：校验码→查公众账号→加密→审计 |
| `controller/PublicAuthController.java` | `/api/public/auth/sms-code`、`/api/public/auth/reset-password` |
| `dto/request/SmsCodeRequest.java` | phone + scene |
| `dto/request/ResetPasswordRequest.java` | phone + smsCode + newPassword |
| `dto/response/SmsCodeResponse.java` | 发码结果（含 fallback 标志，便于前端调试） |
| 测试：`FallbackSmsCodeServiceTest`、`PublicPasswordServiceTest`、`SmsRateLimiterTest` |
| `pom.xml` | 加 `com.aliyun:dypnsapi20170525:1.2.3` |
| `application.yml` | 加 `aliyun.sms-auth` 块 |

**关键约束：**
- 路径在 `/api/public/**` 白名单内（[SaTokenConfig](../../../src/main/java/com/archive/config/SaTokenConfig.java) 已放行）；
- reset-password 仅 `userType=public` 账号可重置；`internal` → 走 §22.6，抛 BUSINESS_CONFLICT；
- fallback 固定码 `123456`；
- 限流命中抛 `TOO_MANY_REQUESTS`；
- 审计 `auditService.log("M01", "reset_password", "user", userId, {phone})`（未登录，actorType 自动 system）。

## Task 1: 依赖、配置、DTO

**Files:**
- Modify: `pom.xml`（加 dypnsapi 依赖 + version 属性）
- Modify: `src/main/resources/application.yml`（加 aliyun.sms-auth 块）
- Create: `src/main/java/com/archive/config/AliyunSmsProperties.java`
- Create: `src/main/java/com/archive/dto/request/SmsCodeRequest.java`
- Create: `src/main/java/com/archive/dto/request/ResetPasswordRequest.java`
- Create: `src/main/java/com/archive/dto/response/SmsCodeResponse.java`

- [ ] **Step 1: pom.xml 加依赖**

在 `<properties>` 加：`<aliyun-dypnsapi.version>1.2.3</aliyun-dypnsapi.version>`
在 `<dependencies>` 加：
```xml
<!-- 阿里云短信认证（公众找回密码） -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>dypnsapi20170525</artifactId>
    <version>${aliyun-dypnsapi.version}</version>
</dependency>
```

- [ ] **Step 2: application.yml 加配置块（文件末尾追加）**

```yaml
aliyun:
  sms-auth:
    enabled: ${ALIYUN_SMS_ENABLED:false}
    access-key-id: ${ALIYUN_SMS_ACCESS_KEY:}
    access-key-secret: ${ALIYUN_SMS_SECRET:}
    endpoint: dypnsapi.aliyuncs.com
    sign-name: ${ALIYUN_SMS_SIGN_NAME:速通互联验证码}
    template-code: ${ALIYUN_SMS_TEMPLATE_CODE:100003}
```
> `enabled` 默认 false：本地/CI 无 AK/SK 自动走 fallback；生产置 true + 配置真实 AK/SK。

- [ ] **Step 3: `AliyunSmsProperties.java`**

```java
package com.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.sms-auth")
public class AliyunSmsProperties {
    private boolean enabled = false;
    private String accessKeyId;
    private String accessKeySecret;
    private String endpoint = "dypnsapi.aliyuncs.com";
    private String signName;
    private String templateCode;
}
```

- [ ] **Step 4: DTO**

`SmsCodeRequest.java`：
```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsCodeRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "^(register|forgot_password)$", message = "场景非法")
    private String scene;
}
```

`ResetPasswordRequest.java`：
```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String smsCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String newPassword;
}
```

`SmsCodeResponse.java`：
```java
package com.archive.dto.response;

import lombok.Data;

@Data
public class SmsCodeResponse {
    /** 是否走了 fallback（固定码）路径，便于前端调试 */
    private boolean fallback;
    private String message;

    public static SmsCodeResponse of(boolean fallback, String message) {
        SmsCodeResponse r = new SmsCodeResponse();
        r.fallback = fallback;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS（依赖成功下载，DTO 编译通过）

- [ ] **Step 6: 提交**

```bash
git add pom.xml src/main/resources/application.yml src/main/java/com/archive/config/AliyunSmsProperties.java src/main/java/com/archive/dto/request/SmsCodeRequest.java src/main/java/com/archive/dto/request/ResetPasswordRequest.java src/main/java/com/archive/dto/response/SmsCodeResponse.java
git commit -m "feat(auth): 接入阿里云短信认证SDK与配置"
```

## Task 2: SmsCodeService 接口与双实现

**Files:**
- Create: `src/main/java/com/archive/service/SmsCodeService.java`
- Create: `src/main/java/com/archive/service/AliyunSmsCodeService.java`
- Create: `src/main/java/com/archive/service/FallbackSmsCodeService.java`
- Test: `src/test/java/com/archive/service/FallbackSmsCodeServiceTest.java`

- [ ] **Step 1: 接口**

```java
package com.archive.service;

/**
 * 短信验证码服务。验证码的生成/存储/过期/一次性校验由实现方托管
 * （阿里云实现托管于阿里云；fallback 实现为固定码）。
 */
public interface SmsCodeService {
    /** 发送验证码 */
    void send(String phone, String scene);

    /** 校验验证码（一次性） */
    boolean verify(String phone, String code);
}
```

- [ ] **Step 2: 写 fallback 失败测试 `FallbackSmsCodeServiceTest`**

```java
package com.archive.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FallbackSmsCodeServiceTest {

    @Test
    void fallback_固定码123456校验通过() {
        FallbackSmsCodeService svc = new FallbackSmsCodeService();
        svc.send("13800000005", "forgot_password"); // 仅日志，无异常即通过
        assertThat(svc.verify("13800000005", "123456")).isTrue();
        assertThat(svc.verify("13800000005", "000000")).isFalse();
    }
}
```

- [ ] **Step 3: 实现 `FallbackSmsCodeService`**

```java
package com.archive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 配置缺失（aliyun.sms-auth.enabled=false）时的兜底实现：固定验证码 123456 + 日志输出。
 * 仅用于本地/CI 联调，不可用于生产。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "aliyun.sms-auth", name = "enabled", havingValue = "false", matchIfMissing = true)
public class FallbackSmsCodeService implements SmsCodeService {

    private static final String FALLBACK_CODE = "123456";

    @Override
    public void send(String phone, String scene) {
        log.warn("[SMS-FALLBACK] phone={} scene={} 固定验证码={}", phone, scene, FALLBACK_CODE);
    }

    @Override
    public boolean verify(String phone, String code) {
        return FALLBACK_CODE.equals(code);
    }
}
```

- [ ] **Step 4: 实现 `AliyunSmsCodeService`**

```java
package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.config.AliyunSmsProperties;
import com.archive.exception.BusinessException;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云「短信认证」实现：SendSmsVerifyCode 发码（验证码由阿里云动态生成），
 * CheckSmsVerifyCode 校验。后端不存验证码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aliyun.sms-auth", name = "enabled", havingValue = "true")
public class AliyunSmsCodeService implements SmsCodeService {

    private final AliyunSmsProperties props;
    private Client client;

    @PostConstruct
    void initClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(props.getAccessKeyId())
                .setAccessKeySecret(props.getAccessKeySecret());
        config.endpoint = props.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public void send(String phone, String scene) {
        try {
            SendSmsVerifyCodeRequest req = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(props.getSignName())
                    .setTemplateCode(props.getTemplateCode())
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                    .setCodeType(1); // 纯数字
            SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCode(req);
            log.info("[SMS-ALIYUN] 发码响应 phone={} code={} message={}",
                    phone, resp.getBody().getCode(), resp.getBody().getMessage());
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 发码失败 phone={}", phone, e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "短信发送失败，请稍后再试");
        }
    }

    @Override
    public boolean verify(String phone, String code) {
        try {
            CheckSmsVerifyCodeRequest req = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setVerifyCode(code);
            CheckSmsVerifyCodeResponse resp = client.checkSmsVerifyCode(req);
            Boolean ok = resp.getBody().getVerifyResult();
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 校验失败 phone={}", phone, e);
            return false;
        }
    }
}
```

> 实现编译后若 SDK 类名/方法签名与 1.2.3 jar 有差异（如 `getVerifyResult` 字段名），以编译错误为准微调，不改业务语义。

- [ ] **Step 5: 运行 fallback 测试**

Run: `mvn -q -Dtest=FallbackSmsCodeServiceTest test`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/archive/service/SmsCodeService.java src/main/java/com/archive/service/FallbackSmsCodeService.java src/main/java/com/archive/service/AliyunSmsCodeService.java src/test/java/com/archive/service/FallbackSmsCodeServiceTest.java
git commit -m "feat(auth): 实现短信验证码服务接口与阿里云及兜底双实现"
```

## Task 3: SmsRateLimiter（内存限流）

**Files:**
- Create: `src/main/java/com/archive/config/SmsRateLimiter.java`
- Test: `src/test/java/com/archive/config/SmsRateLimiterTest.java`

> 单实例内存方案；注入 `LongSupplier` 时钟便于单测。声明多实例局限。

- [ ] **Step 1: 写失败测试**

```java
package com.archive.config;

import com.archive.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsRateLimiterTest {

    private final AtomicLong clock = new AtomicLong(1_000_000L);

    private SmsRateLimiter newLimiter() {
        return new SmsRateLimiter(() -> clock.get());
    }

    @Test
    void 同手机号60秒内第二次发码被拒() {
        SmsRateLimiter l = newLimiter();
        l.acquire("13800000005", "1.1.1.1");
        assertThatThrownBy(() -> l.acquire("13800000005", "1.1.1.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    void 超过60秒可再次发码() {
        SmsRateLimiter l = newLimiter();
        l.acquire("13800000005", "1.1.1.1");
        clock.addAndGet(61_000L);
        assertThatCode(() -> l.acquire("13800000005", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void 同IP每小时超过10次被拒() {
        SmsRateLimiter l = newLimiter();
        for (int i = 0; i < 10; i++) {
            l.acquire("1380000000" + i, "9.9.9.9"); // 不同手机号，同 IP
        }
        assertThatThrownBy(() -> l.acquire("1380000009", "9.9.9.9"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 超过1小时IP计数窗口滑动() {
        SmsRateLimiter l = newLimiter();
        for (int i = 0; i < 10; i++) l.acquire("1380000000" + i, "9.9.9.9");
        clock.addAndGet(60 * 60 * 1000L + 1); // 1 小时 + 1ms
        assertThatCode(() -> l.acquire("1380000009", "9.9.9.9")).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 实现 `SmsRateLimiter`**

```java
package com.archive.config;

import com.archive.common.ErrorCode;
import com.archive.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 短信验证码内存限流：同手机号 60s 冷却；同 IP 每小时 10 次。
 * 单实例方案，多实例部署需换 Redis。
 */
@Component
public class SmsRateLimiter {

    private static final long PHONE_COOLDOWN_MS = 60_000L;
    private static final long IP_WINDOW_MS = 60 * 60 * 1000L;
    private static final int IP_HOURLY_LIMIT = 10;

    private final LongSupplier clock;
    private final Map<String, Long> phoneLastSend = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipSendTimes = new ConcurrentHashMap<>();

    public SmsRateLimiter() {
        this(System::currentTimeMillis);
    }

    public SmsRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** 校验并记录一次发送；超限抛 TOO_MANY_REQUESTS */
    public void acquire(String phone, String ip) {
        long now = clock.getAsLong();

        Long last = phoneLastSend.get(phone);
        if (last != null && now - last < PHONE_COOLDOWN_MS) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }

        List<Long> times = ipSendTimes.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (times) {
            prune(times, now);
            if (times.size() >= IP_HOURLY_LIMIT) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "该IP请求过于频繁，请稍后再试");
            }
            times.add(now);
        }
        phoneLastSend.put(phone, now);
    }

    private void prune(List<Long> times, long now) {
        Iterator<Long> it = times.iterator();
        while (it.hasNext()) {
            if (now - it.next() > IP_WINDOW_MS) {
                it.remove();
            } else {
                break;
            }
        }
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn -q -Dtest=SmsRateLimiterTest test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/config/SmsRateLimiter.java src/test/java/com/archive/config/SmsRateLimiterTest.java
git commit -m "feat(auth): 新增短信验证码内存限流组件"
```

## Task 4: PublicPasswordService（TDD）

**Files:**
- Create: `src/main/java/com/archive/service/PublicPasswordService.java`
- Test: `src/test/java/com/archive/service/PublicPasswordServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.archive.service;

import com.archive.dto.request.ResetPasswordRequest;
import com.archive.entity.User;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.archive.exception.BusinessException;
import com.archive.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublicPasswordServiceTest {

    private PublicPasswordService service;
    private UserMapper userMapper;
    private SmsCodeService smsCodeService;
    private AuditService auditService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void resetPassword_内部账号拒绝() {
        when(smsCodeService.verify(any(), any())).thenReturn(true);
        User u = user("13800000005", UserType.internal);
        when(userMapper.selectOne(any())).thenReturn(u);
        assertThatThrownBy(() -> service.resetPassword(req("13800000005", "123456", "newPass1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内部账号");
        verify(userMapper, never()).updateById(any());
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
```

- [ ] **Step 2: 实现 `PublicPasswordService`**

```java
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
```

- [ ] **Step 3: 运行测试**

Run: `mvn -q -Dtest=PublicPasswordServiceTest test`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/archive/service/PublicPasswordService.java src/test/java/com/archive/service/PublicPasswordServiceTest.java
git commit -m "feat(auth): 实现公众找回密码主流程"
```

## Task 5: PublicAuthController

**Files:**
- Create: `src/main/java/com/archive/controller/PublicAuthController.java`

- [ ] **Step 1: 实现控制器**

```java
package com.archive.controller;

import com.archive.common.R;
import com.archive.config.SmsRateLimiter;
import com.archive.dto.request.ResetPasswordRequest;
import com.archive.dto.request.SmsCodeRequest;
import com.archive.dto.response.SmsCodeResponse;
import com.archive.service.PublicPasswordService;
import com.archive.service.SmsCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
@Tag(name = "公众认证", description = "发送验证码、找回密码（匿名）")
public class PublicAuthController {

    private final SmsCodeService smsCodeService;
    private final PublicPasswordService publicPasswordService;
    private final SmsRateLimiter rateLimiter;

    @Value("${aliyun.sms-auth.enabled:false}")
    private boolean aliyunEnabled;

    @PostMapping("/sms-code")
    @Operation(summary = "发送短信验证码")
    public R<SmsCodeResponse> smsCode(@Valid @RequestBody SmsCodeRequest req, HttpServletRequest http) {
        rateLimiter.acquire(req.getPhone(), clientIp(http));
        smsCodeService.send(req.getPhone(), req.getScene());
        String msg = aliyunEnabled ? "验证码已发送" : "验证码已发送（fallback 固定码 123456）";
        return R.ok(SmsCodeResponse.of(!aliyunEnabled, msg));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "公众找回密码")
    public R<Boolean> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        publicPasswordService.resetPassword(req);
        return R.ok(true);
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
```

- [ ] **Step 2: 编译 + 全量单测**

Run: `mvn -q test`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/archive/controller/PublicAuthController.java
git commit -m "feat(auth): 新增公众发送验证码与找回密码接口"
```

## Task 6: 运行时验证

- [ ] **Step 1: 启动后端（fallback 模式，默认 enabled=false）**

Run（backend）: `mvn spring-boot:run`
Expected: 启动成功，`FallbackSmsCodeService` 生效（日志可见 `@ConditionalOnProperty` 选中）。

- [ ] **Step 2: 发送验证码（fallback）**

```bash
curl -s -X POST http://localhost:8080/api/public/auth/sms-code \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000005","scene":"forgot_password"}' | jq .
```
Expected: `code:OK`，`data.fallback:true`，后端日志打印固定码 `123456`。

- [ ] **Step 3: 限流验证（60s 内第二次 → 429）**

```bash
curl -s -X POST http://localhost:8080/api/public/auth/sms-code -H 'Content-Type: application/json' \
  -d '{"phone":"13800000005","scene":"forgot_password"}' | jq .
```
Expected: `code:TOO_MANY_REQUESTS`。

- [ ] **Step 4: 重置公众账号 `zhou.public`**

```bash
curl -s -X POST http://localhost:8080/api/public/auth/reset-password -H 'Content-Type: application/json' \
  -d '{"phone":"<zhou.public 手机号>","smsCode":"123456","newPassword":"newPass123"}' | jq .
```
> 种子公众账号 `zhou.public` 的手机号以 V16 为准（实现时查 `SELECT login_name, phone FROM users WHERE user_type='public'`）。Expected: `data:true`。

- [ ] **Step 5: 用新密码登录验证**

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"loginName":"zhou.public","password":"newPass123"}' | jq .
```
Expected: 登录成功返回 token。

- [ ] **Step 6: 内部账号拒绝重置**

用任一内部账号手机号调 reset-password → `BUSINESS_CONFLICT 内部账号`。

- [ ] **Step 7: 验证码错误 → VALIDATION_FAILED**

```bash
curl -s -X POST http://localhost:8080/api/public/auth/reset-password -H 'Content-Type: application/json' \
  -d '{"phone":"<公众手机号>","smsCode":"000000","newPassword":"newPass123"}' | jq .
```

- [ ] **Step 8: 审计写入**

`SELECT actor_type, module_name, operation_type FROM audit_logs WHERE operation_type='reset_password';`
Expected: actor_type=system（未登录）、module=M01。

- [ ] **Step 9: 记录验证报告并提交**

写入 `backend/docs/runtime-verification-forgot-password-zhou-06-15.md`。
```bash
git add backend/docs/runtime-verification-forgot-password-zhou-06-15.md
git commit -m "docs(auth): 记录忘记密码运行时验证结果"
```

> 阿里云实网（`ALIYUN_SMS_ENABLED=true` + 真实 AK/SK + 大陆测试号）为可选验证：若有凭据则补一次真实发码/校验；否则以 fallback 路径为主验证，报告中标注阿里云实网未验证（环境依赖）。
> 运行时验证全绿后，模块二视为基本完成。**push / PR 等用户查看后再操作。**


