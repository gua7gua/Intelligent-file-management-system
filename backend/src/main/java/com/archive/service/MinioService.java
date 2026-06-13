package com.archive.service;

import com.archive.config.MinioProperties;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 对象存储操作封装。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioProperties minioProperties;
    private MinioClient minioClient;

    @PostConstruct
    void init() {
        minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * 确保 bucket 存在，不存在则创建。
     */
    public void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建 MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket 检查/创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到 MinIO。
     *
     * @param bucket      目标 bucket
     * @param objectKey   对象路径
     * @param data        文件输入流
     * @param size        文件大小（字节），未知传 -1
     * @param contentType MIME 类型
     */
    public void putObject(String bucket, String objectKey, InputStream data,
                          long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(data, size, -1)
                            .contentType(contentType)
                            .build());
            log.debug("MinIO 上传成功: bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除 MinIO 对象。
     */
    public void deleteObject(String bucket, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            log.debug("MinIO 删除成功: bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名访问 URL（用于文件预览/下载）。
     *
     * @param bucket    bucket 名称
     * @param objectKey 对象路径
     * @return 预签名 URL 字符串
     */
    public String getPresignedUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(60 * 60) // 1 小时有效
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("生成预签名 URL 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制对象（暂存转正式时使用，本期预留）。
     */
    public void copyObject(String srcBucket, String srcKey,
                           String dstBucket, String dstKey) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(dstBucket)
                            .object(dstKey)
                            .source(CopySource.builder()
                                    .bucket(srcBucket)
                                    .object(srcKey)
                                    .build())
                            .build());
            log.debug("MinIO 复制成功: {}/{} -> {}/{}", srcBucket, srcKey, dstBucket, dstKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 复制失败: " + e.getMessage(), e);
        }
    }
}
