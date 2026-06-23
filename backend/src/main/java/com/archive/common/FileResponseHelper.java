package com.archive.common;

import com.archive.entity.ArchiveFile;
import com.archive.service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件流式响应构造（预览/下载统一走后端代理流）。
 * <p>
 * 预览/下载不再返回 minio 预签名 URL（浏览器无法解析容器内地址 minio:9000），
 * 改为后端读取 minio 对象流，以 Blob 形式回给前端，前端用 objectURL 渲染。
 */
public final class FileResponseHelper {

    private FileResponseHelper() {
    }

    /**
     * 构造 minio 对象的流式响应。
     *
     * @param minioService minio 操作服务
     * @param file         已通过权限/状态校验的档案文件
     * @param inline       true=预览（inline，浏览器内嵌显示），false=下载（attachment）
     */
    public static ResponseEntity<InputStreamResource> stream(MinioService minioService,
                                                             ArchiveFile file, boolean inline) {
        InputStream is = minioService.getObjectStream(file.getBucketName(), file.getObjectKey());
        String filename = (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                ? "file" : file.getOriginalFilename();
        // RFC 5987 编码，兼容中文文件名
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(resolveMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded)
                .body(new InputStreamResource(is));
    }

    /** 解析 MIME 类型，空或非法时回退到 application/octet-stream。 */
    private static MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
