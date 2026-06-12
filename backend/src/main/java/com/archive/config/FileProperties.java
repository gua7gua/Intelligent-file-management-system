package com.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件上传与扫描配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** MinIO bucket 名称。 */
    private String bucket = "archive-files";

    /** 单文件最大字节数，默认 500MB。 */
    private long maxSize = 524288000L;

    /** 允许的文件扩展名白名单。 */
    private List<String> allowedExtensions = List.of(
            "pdf", "doc", "docx", "xls", "xlsx",
            "jpg", "jpeg", "png", "tiff", "tif",
            "mp4", "avi", "ofd"
    );

    /** 病毒扫描配置。 */
    private Scan scan = new Scan();

    @Data
    public static class Scan {
        /** 是否启用 ClamAV 扫描。开发期关闭，生产环境改为 true。 */
        private boolean enabled = false;
        /** ClamAV clamd 主机。 */
        private String host = "localhost";
        /** ClamAV clamd TCP 端口。 */
        private int port = 3310;
        /** 扫描超时秒数。 */
        private int timeout = 60;
    }
}
