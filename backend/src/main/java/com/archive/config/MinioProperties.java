package com.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 连接配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** MinIO S3 API 端点。 */
    private String endpoint = "http://localhost:9001";

    /** 访问密钥，对应 MINIO_ROOT_USER。 */
    private String accessKey = "minioadmin";

    /** 秘密密钥，对应 MINIO_ROOT_PASSWORD。 */
    private String secretKey = "minioadmin123";
}
