package com.archive.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件哈希工具类。
 */
public final class HashUtil {

    private HashUtil() {}

    /**
     * 计算 SHA-256 哈希，返回 64 位小写十六进制字符串。
     *
     * @param data 文件字节数组
     * @return SHA-256 哈希值
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
