package com.archive.service;

import com.archive.config.FileProperties;
import com.archive.enums.ScanResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * ClamAV clamd TCP 扫描封装。
 * 通过 INSTREAM 命令将文件流传给 clamd，读取响应判断是否安全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClamAvScanner {

    private final FileProperties fileProperties;

    /**
     * 扫描输入流。
     * 当 file.scan.enabled=false 时直接返回 safe。
     *
     * @param data 待扫描的数据流
     * @return 扫描结果枚举
     */
    public ScanResult scan(InputStream data) {
        if (!fileProperties.getScan().isEnabled()) {
            return ScanResult.safe;
        }

        FileProperties.Scan scanConfig = fileProperties.getScan();
        try (Socket socket = new Socket(scanConfig.getHost(), scanConfig.getPort())) {
            socket.setSoTimeout(scanConfig.getTimeout() * 1000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 发送 zINSTREAM 命令（以 \0 结尾）
            out.write("zINSTREAM\0".getBytes("US-ASCII"));

            // 分块传输数据，每块前缀 4 字节大端长度
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = data.read(chunk)) != -1) {
                out.write(ByteBuffer.allocate(4).putInt(bytesRead).array());
                out.write(chunk, 0, bytesRead);
            }

            // 发送终止标记（0 长度块）
            out.write(ByteBuffer.allocate(4).putInt(0).array());
            out.flush();

            // 读取响应
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] respBuffer = new byte[4096];
            int respRead;
            while ((respRead = in.read(respBuffer)) != -1) {
                response.write(respBuffer, 0, respRead);
            }

            String result = response.toString("US-ASCII").trim();
            return parseResponse(result);

        } catch (IOException e) {
            log.error("ClamAV 连接失败 {}:{}", scanConfig.getHost(), scanConfig.getPort(), e);
            return ScanResult.failed;
        }
    }

    private ScanResult parseResponse(String response) {
        if (response.endsWith("OK")) {
            return ScanResult.safe;
        } else if (response.contains("FOUND")) {
            log.warn("ClamAV 检测到病毒: {}", response);
            return ScanResult.infected;
        } else {
            log.error("ClamAV 返回异常响应: {}", response);
            return ScanResult.failed;
        }
    }
}
