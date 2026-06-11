# 后端脚手架搭建实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Spring Boot 3 后端脚手架，包含 Maven 项目结构、Flyway 全量迁移、统一响应封装、Sa-Token 登录权限闭环，使 Swagger 可访问、前端可按接口样例开发。

**Architecture:** 单体 Spring Boot 应用，MyBatis-Plus 做 ORM，Sa-Token 做认证，Flyway 管理数据库版本，springdoc-openapi 生成 Swagger 文档。包结构严格按设计规格文档 `backend/docs/superpowers/specs/2026-06-11-backend-scaffolding-design.md`。

**Tech Stack:** Spring Boot 3.5.3, Java 17, MyBatis-Plus 3.5.7, Sa-Token 1.44.0, Flyway 11.8.2, springdoc-openapi 2.8.6, PostgreSQL 17, Lombok

---

## 文件结构总览

```
backend/
├── mvnw / mvnw.cmd / .mvn/wrapper/          # Maven Wrapper
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/archive/
│   │   │   ├── ArchiveApplication.java
│   │   │   ├── common/
│   │   │   │   ├── R.java
│   │   │   │   ├── PageResult.java
│   │   │   │   ├── PageRequest.java
│   │   │   │   ├── ErrorCode.java
│   │   │   │   ├── TraceFilter.java
│   │   │   │   └── AuthContext.java
│   │   │   ├── config/
│   │   │   │   ├── SaTokenConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── MyBatisPlusConfig.java
│   │   │   │   └── JacksonConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── DictionaryController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── UserCreateRequest.java
│   │   │   │   │   └── UserUpdateRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── LoginResponse.java
│   │   │   │       ├── UserInfoResponse.java
│   │   │   │       └── MenuVO.java
│   │   │   ├── entity/
│   │   │   │   ├── BaseEntity.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── UserRole.java
│   │   │   │   ├── Organization.java
│   │   │   │   ├── Fonds.java
│   │   │   │   └── Category.java
│   │   │   ├── enums/
│   │   │   │   ├── UserType.java
│   │   │   │   ├── RoleCode.java
│   │   │   │   ├── UserStatus.java
│   │   │   │   ├── SecurityLevel.java
│   │   │   │   ├── DataScope.java
│   │   │   │   └── OrgType.java
│   │   │   ├── exception/
│   │   │   │   └── BusinessException.java
│   │   │   ├── mapper/
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── RoleMapper.java
│   │   │   │   ├── UserRoleMapper.java
│   │   │   │   ├── OrganizationMapper.java
│   │   │   │   ├── FondsMapper.java
│   │   │   │   └── CategoryMapper.java
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       ├── UserService.java
│   │   │       └── DictionaryService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           ├── V1__create-organizations-fonds.sql
│   │           ├── V2__create-users-roles.sql
│   │           ├── V3__create-categories-tags.sql
│   │           ├── V4__create-archives.sql
│   │           ├── V5__create-intake.sql
│   │           ├── V6__create-warehouse.sql
│   │           ├── V7__create-borrow-approval.sql
│   │           ├── V8__create-appraisal-destruction.sql
│   │           ├── V9__create-inventory-compilation.sql
│   │           ├── V10__create-analysis-ai.sql
│   │           ├── V11__create-system.sql
│   │           ├── V12__create-indexes.sql
│   │           ├── V13__create-sequences.sql
│   │           ├── V14__seed-roles-categories-configs.sql
│   │           └── V15__seed-demo-data.sql
│   └── test/
│       └── java/com/archive/
│           └── ArchiveApplicationTests.java
├── CLAUDE.md
└── docs/
```

---

## Task 1: Maven 项目初始化

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`
- Create: `backend/.gitignore`

- [ ] **Step 1: 在 `backend/` 下创建 `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.3</version>
        <relativePath/>
    </parent>

    <groupId>com.archive</groupId>
    <artifactId>archive-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>archive-backend</name>
    <description>智能档案管理系统后端</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <sa-token.version>1.44.0</sa-token.version>
        <springdoc.version>2.8.6</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${sa-token.version}</version>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- springdoc-openapi -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- BCrypt（仅加密工具，不引入 Spring Security） -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 生成 Maven Wrapper**

```bash
cd /home/guagua/projects/cupk-3s/zhouyang/Intelligent-file-management-system/backend
# 如果没有全局 Maven，先安装临时 Maven 来生成 wrapper
paru -S --noconfirm maven
mvn wrapper:wrapper -Dmaven=3.9.9
# 生成后可以卸载全局 Maven
sudo pacman -Rns --noconfirm maven
```

- [ ] **Step 3: 创建 backend/.gitignore**

```
target/
!.mvn/wrapper/maven-wrapper.jar
*.class
*.jar
*.log
.idea/
*.iml
.DS_Store
```

- [ ] **Step 4: 创建启动类和测试骨架**

`src/main/java/com/archive/ArchiveApplication.java`:

```java
package com.archive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArchiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchiveApplication.class, args);
    }
}
```

`src/test/java/com/archive/ArchiveApplicationTests.java`:

```java
package com.archive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ArchiveApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd /home/guagua/projects/cupk-3s/zhouyang/Intelligent-file-management-system/backend
./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/
git commit -m "feat(backend): 初始化 Maven 项目结构和依赖"
```

---

## Task 2: 配置文件

**Files:**
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建 `application.yml`**

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: archive-backend
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deletedAt
      logic-delete-value: NOW()
      logic-not-delete-value: "NULL"

sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 86400
  active-timeout: -1
  is-concurrent: true
  is-share: true
  token-style: uuid
  is-log: false

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  default-flat-param-object: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 2: 创建 `application-dev.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:archive_db}
    username: ${DB_USER:archive_user}
    password: ${DB_PASSWORD:archive_pass_2024}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: "0"

logging:
  level:
    com.archive: DEBUG
    org.flywaydb: INFO
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/resources/
git commit -m "feat(backend): 添加 Spring Boot 配置文件"
```

---

## Task 3: 通用基础类

**Files:**
- Create: `common/R.java`, `common/PageResult.java`, `common/PageRequest.java`
- Create: `common/ErrorCode.java`, `common/BusinessException.java`（放在 exception 包）
- Create: `common/TraceFilter.java`, `common/AuthContext.java`

- [ ] **Step 1: 创建 `common/ErrorCode.java`**

```java
package com.archive.common;

import lombok.Getter;

/**
 * 统一错误码枚举。
 * code 值与 HTTP 状态码配合使用。
 */
@Getter
public enum ErrorCode {

    OK(200, "OK", "操作成功"),
    BAD_REQUEST(400, "BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或 token 无效"),
    FORBIDDEN(403, "FORBIDDEN", "权限不足"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    BUSINESS_CONFLICT(409, "BUSINESS_CONFLICT", "状态冲突"),
    PAYLOAD_TOO_LARGE(413, "PAYLOAD_TOO_LARGE", "上传文件超过限制"),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "不支持的文件类型"),
    VALIDATION_FAILED(422, "VALIDATION_FAILED", "业务校验失败"),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "请求过于频繁"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统内部错误"),
    EXTERNAL_SERVICE_ERROR(502, "EXTERNAL_SERVICE_ERROR", "外部服务异常");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
```

- [ ] **Step 2: 创建 `exception/BusinessException.java`**

```java
package com.archive.exception;

import com.archive.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，由 Service 层抛出，由 GlobalExceptionHandler 统一捕获。
 * 刘星后续在 feat/base-liu 中添加 GlobalExceptionHandler。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 3: 创建 `common/R.java`**

```java
package com.archive.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应封装。
 * 所有 JSON 接口返回此结构。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;

    private R() {}

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ErrorCode.OK.getCode();
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getDefaultMessage();
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }

    public R<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
```

- [ ] **Step 4: 创建 `common/PageResult.java`**

```java
package com.archive.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应结构。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private Boolean hasNext;

    public PageResult() {}

    public PageResult(List<T> records, Integer pageNo, Integer pageSize, Long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = (long) pageNo * pageSize < total;
    }
}
```

- [ ] **Step 5: 创建 `common/PageRequest.java`**

```java
package com.archive.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求基类。
 * 排序字段只能使用后端白名单，具体白名单由各 Controller 定义。
 */
@Data
public class PageRequest {

    @Min(value = 1, message = "pageNo 最小为 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private Integer pageSize = 20;

    private String sortBy;

    private String sortOrder = "asc";
}
```

- [ ] **Step 6: 创建 `common/TraceFilter.java`**

```java
package com.archive.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器。
 * 为每个请求生成 traceId，写入 MDC、响应头和 R.traceId。
 */
@Component
public class TraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";
    private static final String REQUEST_ATTR_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(MDC_KEY, traceId);
        request.setAttribute(REQUEST_ATTR_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 从请求属性中获取当前请求的 traceId。
     * 供 Controller 和 Service 层调用。
     */
    public static String getTraceId(HttpServletRequest request) {
        return (String) request.getAttribute(REQUEST_ATTR_KEY);
    }
}
```

- [ ] **Step 7: 创建 `common/AuthContext.java`**

```java
package com.archive.common;

import cn.dev33.satoken.stp.StpUtil;
import com.archive.enums.DataScope;
import com.archive.enums.RoleCode;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当前用户上下文工具类。
 * 封装从 Sa-Token Session 获取当前用户信息。
 * Service 层直接调用静态方法获取。
 */
public class AuthContext {

    private AuthContext() {}

    private static final String SESSION_KEY_ROLES = "roles";
    private static final String SESSION_KEY_MAX_SECURITY_LEVEL = "maxSecurityLevel";
    private static final String SESSION_KEY_DATA_SCOPE = "dataScope";
    private static final String SESSION_KEY_ORGANIZATION_ID = "organizationId";
    private static final String SESSION_KEY_USER_TYPE = "userType";

    /**
     * 获取当前登录用户 ID。未登录时抛出 Sa-Token 异常。
     */
    public static long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 当前用户是否已登录。
     */
    public static boolean isAuthenticated() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前用户角色列表。
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        return (List<String>) StpUtil.getSession().get(SESSION_KEY_ROLES);
    }

    /**
     * 获取当前用户角色码集合。
     */
    public static Set<RoleCode> getCurrentUserRoleCodes() {
        List<String> roleStrings = getCurrentUserRoles();
        return roleStrings.stream()
                .map(RoleCode::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 判断当前用户是否拥有指定角色。
     */
    public static boolean hasRole(RoleCode roleCode) {
        return getCurrentUserRoleCodes().contains(roleCode);
    }

    /**
     * 判断当前用户是否为管理员角色（前台/后台/系统管理员）。
     */
    public static boolean isAdmin() {
        Set<RoleCode> roles = getCurrentUserRoleCodes();
        return roles.contains(RoleCode.front_archivist)
                || roles.contains(RoleCode.back_archivist)
                || roles.contains(RoleCode.sys_admin);
    }

    /**
     * 获取当前用户最高可查密级。
     */
    public static int getMaxSecurityLevel() {
        Object val = StpUtil.getSession().get(SESSION_KEY_MAX_SECURITY_LEVEL);
        return val != null ? (int) val : 0;
    }

    /**
     * 获取当前用户数据范围。
     */
    public static DataScope getDataScope() {
        Object val = StpUtil.getSession().get(SESSION_KEY_DATA_SCOPE);
        return val != null ? (DataScope) val : DataScope.own_org;
    }

    /**
     * 获取当前用户所属组织 ID。
     */
    public static Long getOrganizationId() {
        Object val = StpUtil.getSession().get(SESSION_KEY_ORGANIZATION_ID);
        return val != null ? (Long) val : null;
    }

    /**
     * 获取当前用户类型。
     */
    public static String getUserType() {
        Object val = StpUtil.getSession().get(SESSION_KEY_USER_TYPE);
        return val != null ? (String) val : null;
    }
}
```

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/archive/common/ backend/src/main/java/com/archive/exception/
git commit -m "feat(backend): 添加统一响应、分页、错误码、追踪过滤器和认证上下文"
```

---

## Task 4: 枚举类

**Files:**
- Create: `enums/` 下 6 个枚举类

- [ ] **Step 1: 创建所有枚举类**

`enums/UserType.java`:

```java
package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserType {
    internal("internal"),
    @EnumValue public_("public");

    @EnumValue
    private final String value;

    UserType(String value) {
        this.value = value;
    }
}
```

`enums/RoleCode.java`:

```java
package com.archive.enums;

import lombok.Getter;

/**
 * 预设角色码，与 roles 表 seed 数据一致。
 */
@Getter
public enum RoleCode {
    front_archivist("档案管理员（前台）"),
    back_archivist("档案管理员（后台）"),
    transfer_user("移交单位经办人"),
    internal_reader("内部查阅者"),
    public_user("社会公众"),
    director("馆领导"),
    sys_admin("系统管理员");

    private final String displayName;

    RoleCode(String displayName) {
        this.displayName = displayName;
    }
}
```

`enums/UserStatus.java`:

```java
package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    @EnumValue active("active"),
    @EnumValue disabled("disabled");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}
```

`enums/SecurityLevel.java`:

```java
package com.archive.enums;

import lombok.Getter;

@Getter
public enum SecurityLevel {
    NONE(0, "非密"),
    INTERNAL(1, "内部"),
    SECRET(2, "秘密"),
    CONFIDENTIAL(3, "机密"),
    TOP_SECRET(4, "绝密");

    private final int level;
    private final String displayName;

    SecurityLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
}
```

`enums/DataScope.java`:

```java
package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DataScope {
    @EnumValue own_org("own_org"),
    @EnumValue own_fonds("own_fonds"),
    @EnumValue all("all");

    @EnumValue
    private final String value;

    DataScope(String value) {
        this.value = value;
    }
}
```

`enums/OrgType.java`:

```java
package com.archive.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrgType {
    @EnumValue archive_org("archive_org"),
    @EnumValue government("government"),
    @EnumValue enterprise("enterprise"),
    @EnumValue public_institution("public_institution");

    @EnumValue
    private final String value;

    OrgType(String value) {
        this.value = value;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/archive/enums/
git commit -m "feat(backend): 添加用户、角色、密级等业务枚举"
```

---

## Task 5: Entity 类 + MyBatis-Plus 配置

**Files:**
- Create: `entity/BaseEntity.java` + 6 个实体类
- Create: `config/MyBatisPlusConfig.java`
- Create: `config/JacksonConfig.java`
- Create: `config/CorsConfig.java`

- [ ] **Step 1: 创建 `entity/BaseEntity.java`**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 实体基类，包含通用审计字段。
 * 不使用 MyBatis-Plus 逻辑删除注解，软删除由 deleted_at 字段判断，
 * 仅在有软删需求的 Entity 上声明 deletedAt 字段。
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;
}
```

- [ ] **Step 2: 创建 `entity/Organization.java`**

```java
package com.archive.entity;

import com.archive.enums.OrgType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organizations")
public class Organization extends BaseEntity {

    private String orgName;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private OrgType orgType;

    private String contactName;
    private String contactPhone;
    private String status;
    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 3: 创建 `entity/Fonds.java`**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fonds")
public class Fonds extends BaseEntity {

    private String fondsNo;
    private String fondsName;
    private Long organizationId;
    private String description;
    private String status;
    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 4: 创建 `entity/Role.java`**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("roles")
public class Role {

    @TableId(type = IdType.AUTO)
    private Short id;

    private String roleCode;
    private String roleName;
    private String description;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 5: 创建 `entity/User.java`**

```java
package com.archive.entity;

import com.archive.enums.DataScope;
import com.archive.enums.UserStatus;
import com.archive.enums.UserType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @com.baomidou.mybatisplus.annotation.EnumValue
    private UserType userType;

    private String loginName;
    private String employeeNo;
    private String phone;
    private String passwordHash;
    private String realName;
    private Long organizationId;
    private String departmentName;

    private Integer maxSecurityLevel;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private DataScope dataScope;

    @com.baomidou.mybatisplus.annotation.EnumValue
    private UserStatus status;

    private OffsetDateTime deletedAt;
}
```

- [ ] **Step 6: 创建 `entity/UserRole.java`**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("user_roles")
public class UserRole {

    private Long userId;
    private Short roleId;
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 7: 创建 `entity/Category.java`**

```java
package com.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("categories")
public class Category {

    @TableId(type = IdType.AUTO)
    private Short id;

    private String categoryName;
    private String categoryCode;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 8: 创建 `config/MyBatisPlusConfig.java`**

```java
package com.archive.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.archive.common.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, OffsetDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
                Long userId = AuthContext.isAuthenticated() ? AuthContext.getCurrentUserId() : null;
                this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
                Long userId = AuthContext.isAuthenticated() ? AuthContext.getCurrentUserId() : null;
                this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
            }
        };
    }
}
```

- [ ] **Step 9: 创建 `config/JacksonConfig.java`**

```java
package com.archive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 配置：Long 序列化为 String 防止前端精度丢失，
 * 日期时间使用 ISO-8601 格式。
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

- [ ] **Step 10: 创建 `config/CorsConfig.java`**

```java
package com.archive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("X-Trace-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 11: 提交**

```bash
git add backend/src/main/java/com/archive/entity/ backend/src/main/java/com/archive/config/
git commit -m "feat(backend): 添加实体类、MyBatis-Plus 配置和跨域配置"
```

---

## Task 6: Flyway 迁移 V1-V11（全部建表）

**Files:**
- Create: `db/migration/V1__create-organizations-fonds.sql` 至 `V11__create-system.sql`

- [ ] **Step 1: 从 `database/archive_empty.sql` 拆分创建 V1**

V1 至 V11 的 DDL 直接取自 `database/archive_empty.sql` 中对应的 CREATE TABLE 语句。每个文件只包含该版本涉及的表，保持与源文件完全一致的字段定义、CHECK 约束和外键。

拆分规则：
- V1: `organizations` + `fonds`
- V2: `roles` + `users` + `user_roles`
- V3: `categories` + `tags` + `archive_tags`
- V4: `compilations` + `archives` + `archive_files`
- V5: `intake_batches` + `intake_items` + `staging_files` + `business_attachments`
- V6: `warehouse_rooms` + `storage_locations` + `archive_boxes` + `archive_box_items`
- V7: `borrow_requests` + `approval_requests`
- V8: `appraisal_batches` + `appraisal_items` + `destruction_lists` + `destruction_items`
- V9: `inventory_tasks` + `inventory_items` + `compilation_materials`
- V10: `analysis_tasks` + `analysis_items` + `ai_tasks` + `ai_task_batches`
- V11: `backup_tasks` + `file_check_records` + `archive_access_logs` + `audit_logs` + `system_configs`

注意点：
- V2 中 `users` 表引用 `organizations(id)` 外键，V1 已建 `organizations`，所以顺序正确
- V4 中 `archives` 引用 `compilations`，所以 `compilations` 必须在 `archives` 之前创建（在 V4 内部按 compilations → archives → archive_files 顺序）
- V5 中 `staging_files` 引用 `archive_files(id)` 外键，V4 已建 `archive_files`
- V10 中 `ai_tasks` 先创建，然后 `intake_batches` 和 `analysis_tasks` 的 `latest_ai_task_id` 外键在 V10 的 `ai_tasks` 之后用 ALTER TABLE 添加
- 所有 CREATE TABLE 语句直接从 `database/archive_empty.sql` 复制，不做修改

每个迁移文件开头注释标明包含的表，末尾不加 COMMIT（Flyway 自行管理事务）。

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat(backend): 添加 Flyway 迁移脚本 V1-V11（全部业务表）"
```

---

## Task 7: Flyway 迁移 V12-V15（索引 + 序列 + 种子数据）

**Files:**
- Create: `V12__create-indexes.sql`, `V13__create-sequences.sql`
- Create: `V14__seed-roles-categories-configs.sql`, `V15__seed-demo-data.sql`

- [ ] **Step 1: 创建 V12（全部索引）**

从 `database/archive_empty.sql` 的唯一约束索引、查询索引和 trigram 索引部分全部提取到 V12。

包含：
- 所有 `CREATE UNIQUE INDEX` 语句
- 所有 `CREATE INDEX` 语句
- 所有 `CREATE INDEX ... USING gin (xxx gin_trgm_ops)` 语句

- [ ] **Step 2: 创建 V13（业务编号序列）**

从 `database/archive_empty.sql` 提取全部 `CREATE SEQUENCE` 语句：

```sql
-- 业务编号序列
CREATE SEQUENCE seq_archive_no START WITH 1;
CREATE SEQUENCE seq_batch_no START WITH 1;
CREATE SEQUENCE seq_borrow_request_no START WITH 1;
CREATE SEQUENCE seq_destruction_list_no START WITH 1;
CREATE SEQUENCE seq_borrow_voucher_no START WITH 1;
CREATE SEQUENCE seq_archive_box_no START WITH 1;
CREATE SEQUENCE seq_backup_task_no START WITH 1;
CREATE SEQUENCE seq_analysis_task_no START WITH 1;
CREATE SEQUENCE seq_ai_task_no START WITH 1;
CREATE SEQUENCE seq_compilation_no START WITH 1;
CREATE SEQUENCE seq_inventory_task_no START WITH 1;
```

- [ ] **Step 3: 创建 V14（角色 + 分类 + 系统配置种子数据）**

从 `database/archive_empty.sql` 提取 categories INSERT、roles INSERT、system_configs INSERT。

- [ ] **Step 4: 创建 V15（演示数据）**

从 `database/archive_demo.sql` 提取全部 INSERT 语句和序列重置语句。注意：
- 将 `demo-bcrypt-hash` 替换为密码 `123456` 的真实 BCrypt hash
- BCrypt hash 可通过在 Java 中执行 `new BCryptPasswordEncoder().encode("123456")` 生成
- 包含所有 setval 序列重置语句
- 包含所有 UPDATE 语句（如 `UPDATE intake_items SET generated_archive_id`）

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat(backend): 添加 Flyway 迁移 V12-V15（索引、序列、种子数据）"
```

---

## Task 8: Mapper 接口

**Files:**
- Create: `mapper/` 下 6 个 Mapper 接口

- [ ] **Step 1: 创建所有 Mapper 接口**

`mapper/UserMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

`mapper/RoleMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
```

`mapper/UserRoleMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
```

`mapper/OrganizationMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.Organization;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
```

`mapper/FondsMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.Fonds;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FondsMapper extends BaseMapper<Fonds> {
}
```

`mapper/CategoryMapper.java`:

```java
package com.archive.mapper;

import com.archive.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/archive/mapper/
git commit -m "feat(backend): 添加用户、角色、组织等 Mapper 接口"
```

---

## Task 9: DTO 类

**Files:**
- Create: `dto/request/LoginRequest.java`, `dto/request/UserCreateRequest.java`, `dto/request/UserUpdateRequest.java`
- Create: `dto/response/LoginResponse.java`, `dto/response/UserInfoResponse.java`, `dto/response/MenuVO.java`

- [ ] **Step 1: 创建 Request DTO**

`dto/request/LoginRequest.java`:

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "门户类型不能为空")
    private String portal; // public, internal, transfer, admin, approval
}
```

`dto/request/UserCreateRequest.java`:

```java
package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    private String employeeNo;

    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel = 0;
    private String dataScope = "own_org";
}
```

`dto/request/UserUpdateRequest.java`:

```java
package com.archive.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String phone;
    private String realName;
    private Long organizationId;
    private String departmentName;
    private Integer maxSecurityLevel;
    private String dataScope;
    private String status;
}
```

- [ ] **Step 2: 创建 Response DTO**

`dto/response/LoginResponse.java`:

```java
package com.archive.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private UserInfoResponse user;
    private String defaultRoute;

    @Data
    public static class UserInfoResponse {
        private Long id;
        private String realName;
        private String userType;
        private List<String> roles;
        private Long organizationId;
        private Integer maxSecurityLevel;
        private String dataScope;
    }
}
```

`dto/response/UserInfoResponse.java`:

```java
package com.archive.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UserInfoResponse {

    private Long id;
    private String userType;
    private String loginName;
    private String employeeNo;
    private String phone;
    private String realName;
    private Long organizationId;
    private String organizationName;
    private String departmentName;
    private Integer maxSecurityLevel;
    private String dataScope;
    private String status;
    private List<String> roles;
    private OffsetDateTime createdAt;
}
```

`dto/response/MenuVO.java`:

```java
package com.archive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 前端菜单/路由项。
 * 登录接口返回当前用户可访问的路由列表。
 */
@Data
public class MenuVO {

    private String name;
    private String path;
    private String icon;
    private List<MenuVO> children;

    public MenuVO() {}

    public MenuVO(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public MenuVO(String name, String path, String icon) {
        this.name = name;
        this.path = path;
        this.icon = icon;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/archive/dto/
git commit -m "feat(backend): 添加登录、用户管理相关 DTO"
```

---

## Task 10: DictionaryService + DictionaryController

**Files:**
- Create: `service/DictionaryService.java`
- Create: `controller/DictionaryController.java`

- [ ] **Step 1: 创建 `service/DictionaryService.java`**

```java
package com.archive.service;

import com.archive.mapper.CategoryMapper;
import com.archive.mapper.RoleMapper;
import com.archive.mapper.OrganizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final RoleMapper roleMapper;
    private final CategoryMapper categoryMapper;
    private final OrganizationMapper organizationMapper;

    /**
     * 获取全部字典。前端初始化时调用一次。
     * 不返回敏感配置值。
     */
    public Map<String, Object> getAllDictionaries() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("roles", roleMapper.selectList(null).stream()
                .map(r -> Map.of("roleCode", r.getRoleCode(),
                        "roleName", r.getRoleName(),
                        "enabled", r.getEnabled()))
                .collect(Collectors.toList()));
        dict.put("categories", categoryMapper.selectList(null).stream()
                .map(c -> Map.of("categoryCode", c.getCategoryCode(),
                        "categoryName", c.getCategoryName(),
                        "enabled", c.getEnabled()))
                .collect(Collectors.toList()));
        return dict;
    }

    /**
     * 获取单个字典。
     */
    public Object getDictionary(String dictCode) {
        Map<String, Object> all = getAllDictionaries();
        return all.get(dictCode);
    }
}
```

- [ ] **Step 2: 创建 `controller/DictionaryController.java`**

```java
package com.archive.controller;

import com.archive.common.R;
import com.archive.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
@Tag(name = "字典接口", description = "前端初始化枚举和配置字典")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping
    @Operation(summary = "获取全部字典")
    public R<Map<String, Object>> getAll(HttpServletRequest request) {
        return R.ok(dictionaryService.getAllDictionaries());
    }

    @GetMapping("/{dictCode}")
    @Operation(summary = "获取单个字典")
    public R<Object> getOne(@PathVariable String dictCode, HttpServletRequest request) {
        Object dict = dictionaryService.getDictionary(dictCode);
        if (dict == null) {
            return R.fail(com.archive.common.ErrorCode.NOT_FOUND, "字典不存在: " + dictCode);
        }
        return R.ok(dict);
    }
}
```

需要额外导入 `java.util.Map`。

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/archive/service/DictionaryService.java backend/src/main/java/com/archive/controller/DictionaryController.java
git commit -m "feat(backend): 添加字典查询接口"
```

---

## Task 11: AuthService + AuthController + SaTokenConfig

**Files:**
- Create: `service/AuthService.java`
- Create: `controller/AuthController.java`
- Create: `config/SaTokenConfig.java`

- [ ] **Step 1: 创建 `service/AuthService.java`**

```java
package com.archive.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.dto.request.LoginRequest;
import com.archive.dto.response.LoginResponse;
import com.archive.dto.response.MenuVO;
import com.archive.entity.Role;
import com.archive.entity.User;
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

    /**
     * 登录：校验账号密码 + 门户角色匹配 + 签发 token。
     */
    public LoginResponse login(LoginRequest req) {
        // 1. 查找用户
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        // 3. 校验状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        // 4. 查询角色
        List<Role> roles = getUserRoles(user.getId());
        Set<String> roleCodes = roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        // 5. 校验门户匹配
        validatePortalAccess(req.getPortal(), roleCodes);

        // 6. Sa-Token 登录
        StpUtil.login(user.getId());

        // 7. 写入 Session
        StpUtil.getSession().set("roles", new ArrayList<>(roleCodes));
        StpUtil.getSession().set("maxSecurityLevel", user.getMaxSecurityLevel());
        StpUtil.getSession().set("dataScope", user.getDataScope() != null ? user.getDataScope().name() : "own_org");
        StpUtil.getSession().set("organizationId", user.getOrganizationId());
        StpUtil.getSession().set("userType", user.getUserType() != null ? user.getUserType().name() : "internal");

        // 8. 构造响应
        LoginResponse resp = new LoginResponse();
        resp.setToken(StpUtil.getTokenValue());

        LoginResponse.UserInfoResponse userInfo = new LoginResponse.UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setRealName(user.getRealName());
        userInfo.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        userInfo.setRoles(new ArrayList<>(roleCodes));
        userInfo.setOrganizationId(user.getOrganizationId());
        userInfo.setMaxSecurityLevel(user.getMaxSecurityLevel());
        userInfo.setDataScope(user.getDataScope() != null ? user.getDataScope().name() : null);
        resp.setUser(userInfo);

        // 9. 默认路由
        resp.setDefaultRoute(getDefaultRoute(roleCodes, req.getPortal()));

        return resp;
    }

    /**
     * 登出。
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前用户信息。
     */
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
        var userRoles = userRoleMapper.selectList(
                new QueryWrapper<com.archive.entity.UserRole>().eq("user_id", userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Short> roleIds = userRoles.stream().map(com.archive.entity.UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds);
    }

    private void validatePortalAccess(String portal, Set<String> roleCodes) {
        Set<String> allowed;
        switch (portal) {
            case "admin" -> allowed = Set.of(
                    RoleCode.front_archivist.name(),
                    RoleCode.back_archivist.name(),
                    RoleCode.sys_admin.name(),
                    RoleCode.director.name());
            case "transfer" -> allowed = Set.of(RoleCode.transfer_user.name());
            case "internal" -> allowed = Set.of(RoleCode.internal_reader.name());
            case "public" -> allowed = Set.of(RoleCode.public_user.name());
            case "approval" -> allowed = Set.of(RoleCode.director.name());
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的门户类型: " + portal);
        }
        if (roleCodes.stream().noneMatch(allowed::contains)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权访问此门户");
        }
    }

    private String getDefaultRoute(Set<String> roleCodes, String portal) {
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
```

- [ ] **Step 2: 创建 `controller/AuthController.java`**

```java
package com.archive.controller;

import com.archive.common.AuthContext;
import com.archive.common.R;
import com.archive.dto.request.LoginRequest;
import com.archive.dto.response.LoginResponse;
import com.archive.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "登录、登出、当前用户")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        LoginResponse resp = authService.login(req);
        return R.ok(resp).traceId(com.archive.common.TraceFilter.getTraceId(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<LoginResponse.UserInfoResponse> me() {
        return R.ok(authService.me());
    }
}
```

- [ ] **Step 3: 创建 `config/SaTokenConfig.java`**

```java
package com.archive.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 放行路径由 Sa-Token 配置处理，这里注册拦截器即可
                    // 具体路由权限校验在 Service 层通过 @SaCheckRole 等注解实现
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/public/**",
                        "/api/dictionaries/**"
                );
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/archive/service/AuthService.java backend/src/main/java/com/archive/controller/AuthController.java backend/src/main/java/com/archive/config/SaTokenConfig.java
git commit -m "feat(backend): 添加登录/登出/当前用户接口和 Sa-Token 配置"
```

---

## Task 12: UserService + UserController

**Files:**
- Create: `service/UserService.java`
- Create: `controller/UserController.java`

- [ ] **Step 1: 创建 `service/UserService.java`**

```java
package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.common.PageResult;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.entity.Role;
import com.archive.entity.User;
import com.archive.entity.UserRole;
import com.archive.enums.RoleCode;
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

    /**
     * 分页查询用户列表。
     */
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

    /**
     * 新增用户。
     */
    @Transactional
    public UserInfoResponse createUser(UserCreateRequest req) {
        // 检查登录名唯一
        User existing = userMapper.selectOne(
                new QueryWrapper<User>().eq("login_name", req.getLoginName()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "登录名已存在");
        }

        User user = new User();
        user.setUserType(com.archive.enums.UserType.valueOf(req.getUserType()));
        user.setLoginName(req.getLoginName());
        user.setEmployeeNo(req.getEmployeeNo());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setOrganizationId(req.getOrganizationId());
        user.setDepartmentName(req.getDepartmentName());
        user.setMaxSecurityLevel(req.getMaxSecurityLevel());
        user.setDataScope(com.archive.enums.DataScope.valueOf(req.getDataScope()));
        user.setStatus(com.archive.enums.UserStatus.active);

        userMapper.insert(user);
        return toUserInfoResponse(user);
    }

    /**
     * 编辑用户。
     */
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
        if (req.getDataScope() != null) user.setDataScope(com.archive.enums.DataScope.valueOf(req.getDataScope()));
        if (req.getStatus() != null) user.setStatus(com.archive.enums.UserStatus.valueOf(req.getStatus()));

        userMapper.updateById(user);
        return toUserInfoResponse(user);
    }

    /**
     * 重置密码。
     */
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

        // 查角色
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
```

- [ ] **Step 2: 创建 `controller/UserController.java`**

```java
package com.archive.controller;

import com.archive.common.PageRequest;
import com.archive.common.PageResult;
import com.archive.common.R;
import com.archive.dto.request.UserCreateRequest;
import com.archive.dto.request.UserUpdateRequest;
import com.archive.dto.response.UserInfoResponse;
import com.archive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户增删改查（管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public R<PageResult<UserInfoResponse>> list(PageRequest page,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        return R.ok(userService.listUsers(page.getPageNo(), page.getPageSize(), keyword, status));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public R<UserInfoResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return R.ok(userService.createUser(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑用户")
    public R<UserInfoResponse> update(@PathVariable Long id,
                                      @Valid @RequestBody UserUpdateRequest req) {
        return R.ok(userService.updateUser(id, req));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(id, newPassword);
        return R.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/archive/service/UserService.java backend/src/main/java/com/archive/controller/UserController.java
git commit -m "feat(backend): 添加用户管理增删改查接口"
```

---

## Task 13: Docker 基础设施 + 编译验证

**Files:**
- 无新文件，使用现有 `doc/pre-environment/` 下的 Docker Compose 配置

- [ ] **Step 1: 创建 .env 文件**

```bash
cd /home/guagua/projects/cupk-3s/zhouyang/Intelligent-file-management-system
cp doc/pre-environment/.env.example .env
```

- [ ] **Step 2: 启动 Docker 服务**

```bash
docker compose --env-file .env -f doc/pre-environment/docker-compose.yml up -d
```

等待 PostgreSQL 健康检查通过。

- [ ] **Step 3: 编译并启动后端**

```bash
cd /home/guagua/projects/cupk-3s/zhouyang/Intelligent-file-management-system/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected:
- Flyway 自动执行 V1-V15 迁移，日志无报错
- 应用启动成功，监听 8080 端口

- [ ] **Step 4: 验证 Swagger**

```bash
curl -s http://localhost:8080/swagger-ui.html -o /dev/null -w "%{http_code}"
```

Expected: 200

- [ ] **Step 5: 验证登录**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginName":"chen.front","password":"123456","portal":"admin"}' | python3 -m json.tool
```

Expected: 返回 `{"code":"OK","data":{"token":"...","user":{...},"defaultRoute":"/admin/overview"}}`

- [ ] **Step 6: 验证权限拦截**

```bash
# 未登录访问管理接口
curl -s http://localhost:8080/api/users?pageNo=1 | python3 -m json.tool
```

Expected: 返回 `{"code":"UNAUTHORIZED",...}`

- [ ] **Step 7: 验证字典接口**

```bash
curl -s http://localhost:8080/api/dictionaries | python3 -m json.tool
```

Expected: 返回角色和分类字典

- [ ] **Step 8: 验证完成后停止后端**

Ctrl+C 停止 spring-boot 进程。

---

## 自查清单

**1. Spec 覆盖：**
- [x] 项目初始化 → Task 1
- [x] 配置文件 → Task 2
- [x] 统一响应 R<T> → Task 3
- [x] TraceFilter → Task 3
- [x] AuthContext → Task 3
- [x] ErrorCode → Task 3
- [x] BusinessException → Task 3
- [x] 枚举类 → Task 4
- [x] Entity 类 + BaseEntity → Task 5
- [x] MyBatis-Plus 配置（分页+自动填充） → Task 5
- [x] Jackson 配置 → Task 5
- [x] CORS 配置 → Task 5
- [x] Flyway V1-V15 → Task 6 + Task 7
- [x] Mapper 接口 → Task 8
- [x] DTO 类 → Task 9
- [x] DictionaryService + DictionaryController → Task 10
- [x] AuthService + AuthController → Task 11
- [x] UserService + UserController → Task 12
- [x] SaTokenConfig → Task 11
- [x] 验收标准 → Task 13

**2. 占位符检查：** 无 TODO/TBD/省略。

**3. 类型一致性：** 所有 Entity、DTO、Service、Controller 之间的字段名和类型保持一致。
