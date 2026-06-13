package com.archive.util;

import org.apache.commons.io.FilenameUtils;

/**
 * 文件类型校验工具类。
 */
public final class FileTypeUtil {

    private FileTypeUtil() {}

    /**
     * 获取文件扩展名（小写）。
     *
     * @param filename 文件名
     * @return 小写扩展名，无扩展名返回空字符串
     */
    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename).toLowerCase();
    }

    /**
     * 校验文件扩展名是否在白名单内。
     *
     * @param filename           文件名
     * @param allowedExtensions  允许的扩展名列表
     * @return true 如果允许
     */
    public static boolean isAllowedExtension(String filename, java.util.List<String> allowedExtensions) {
        String ext = getExtension(filename);
        return !ext.isEmpty() && allowedExtensions.contains(ext);
    }
}
