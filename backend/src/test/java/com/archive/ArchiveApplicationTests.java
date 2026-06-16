package com.archive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用上下文装配 smoke test。
 * 使用 dev profile：dev 配置含完整 datasource/minio/clamav 等依赖，能真实验证全上下文装配。
 */
@SpringBootTest
@ActiveProfiles("dev")
class ArchiveApplicationTests {

    @Test
    void contextLoads() {
    }
}
