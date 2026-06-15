package com.archive.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 日志导出工具：SXSSF 流式写 xlsx，避免大数据量内存膨胀。
 */
public final class LogExcelExporter {

    private LogExcelExporter() {}

    public static void write(HttpServletResponse response, String filename,
                             String[] headers, List<String[]> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (Workbook wb = new SXSSFWorkbook(100); OutputStream out = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("logs");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (String[] row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.length; i++) {
                    r.createCell(i).setCellValue(row[i] != null ? row[i] : "");
                }
            }
            wb.write(out);
        }
    }
}
