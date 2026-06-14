package com.archive.service;

import com.archive.common.AuthContext;
import com.archive.common.ErrorCode;
import com.archive.dto.request.StatisticsOverviewQuery;
import com.archive.dto.response.StatisticsCategoryResponse;
import com.archive.dto.response.StatisticsOverviewResponse;
import com.archive.enums.RoleCode;
import com.archive.exception.BusinessException;
import com.archive.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 统计服务（M14）。实时聚合业务表，无事实表；支持 xlsx/pdf 报表导出。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;
    private final PdfGenerator pdfGenerator;

    private void requireRole() {
        if (!(AuthContext.hasRole(RoleCode.back_archivist) || AuthContext.hasRole(RoleCode.director))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /** 20.1 统计总览 */
    public StatisticsOverviewResponse overview(StatisticsOverviewQuery q) {
        requireRole();
        StatisticsOverviewResponse resp = new StatisticsOverviewResponse();
        StatisticsOverviewResponse.Totals t = new StatisticsOverviewResponse.Totals();
        t.setTotalArchives(count("SELECT COUNT(*) FROM archives WHERE lifecycle_status<>'destroyed' AND deleted_at IS NULL"));
        t.setOpenArchives(count("SELECT COUNT(*) FROM archives WHERE open_status='open' AND lifecycle_status<>'destroyed' AND deleted_at IS NULL"));
        t.setBorrowCount(count("SELECT COUNT(*) FROM borrow_requests WHERE deleted_at IS NULL"));
        t.setDestroyedCount(count("SELECT COUNT(*) FROM archives WHERE lifecycle_status='destroyed' AND deleted_at IS NULL"));
        resp.setTotals(t);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT to_char(created_at,'YYYY-MM') AS month, source_type, COUNT(*) AS cnt " +
                "FROM intake_batches WHERE deleted_at IS NULL GROUP BY 1,2 ORDER BY 1 DESC LIMIT 12");
        resp.setTrends(rows.stream().map(r -> {
            StatisticsOverviewResponse.Trend tr = new StatisticsOverviewResponse.Trend();
            tr.setMonth(String.valueOf(r.get("month")));
            tr.setSourceType(String.valueOf(r.get("source_type")));
            tr.setCount(((Number) r.get("cnt")).longValue());
            return tr;
        }).toList());

        List<Map<String, Object>> su = jdbcTemplate.queryForList(
                "SELECT sl.id AS id, sl.location_code AS code, COALESCE(ab.used_count,0) AS used, COALESCE(ab.capacity,0) AS cap " +
                "FROM storage_locations sl LEFT JOIN archive_boxes ab ON ab.location_id=sl.id " +
                "WHERE sl.deleted_at IS NULL");
        resp.setStorageUsage(su.stream().map(r -> {
            StatisticsOverviewResponse.StorageUsage u = new StatisticsOverviewResponse.StorageUsage();
            u.setLocationId(((Number) r.get("id")).longValue());
            u.setLocationCode(String.valueOf(r.get("code")));
            int used = ((Number) r.get("used")).intValue();
            int cap = ((Number) r.get("cap")).intValue();
            u.setUsed(used);
            u.setCapacity(cap);
            u.setRate(cap > 0 ? used * 100.0 / cap : 0.0);
            return u;
        }).toList());
        return resp;
    }

    /** 20.2 分类统计 */
    public StatisticsCategoryResponse categories(StatisticsOverviewQuery q) {
        requireRole();
        StatisticsCategoryResponse resp = new StatisticsCategoryResponse();
        resp.setByCategory(group("SELECT c.category_name AS k, COUNT(*) AS cnt FROM archives a " +
                "JOIN categories c ON c.id=a.category_id WHERE a.deleted_at IS NULL AND a.lifecycle_status<>'destroyed' GROUP BY 1"));
        resp.setByYear(group("SELECT a.formed_year::text AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL AND a.formed_year IS NOT NULL GROUP BY 1 ORDER BY 1"));
        resp.setBySource(group("SELECT a.source_type AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1"));
        resp.setByCarrier(group("SELECT a.carrier_status AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1"));
        resp.setBySecurity(group("SELECT a.security_level::text AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1 ORDER BY 1"));
        resp.setByOpenStatus(group("SELECT a.open_status AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1"));
        return resp;
    }

    /** 20.3 导出 xlsx / pdf */
    public byte[] export(StatisticsOverviewQuery q) {
        requireRole();
        String format = q.getFormat() == null ? "xlsx" : q.getFormat().toLowerCase();
        StatisticsOverviewResponse o = overview(q);
        StatisticsCategoryResponse c = categories(q);
        try {
            if ("pdf".equals(format)) {
                return pdfGenerator.generateStatisticsPdf(o, c);
            }
            if ("xlsx".equals(format)) {
                return buildXlsx(o, c);
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的导出格式: " + format);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private byte[] buildXlsx(StatisticsOverviewResponse o, StatisticsCategoryResponse c) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet s1 = wb.createSheet("总览");
            int r = 0;
            createRow(s1, r++, "馆藏总量", o.getTotals().getTotalArchives());
            createRow(s1, r++, "公开数量", o.getTotals().getOpenArchives());
            createRow(s1, r++, "借阅量", o.getTotals().getBorrowCount());
            createRow(s1, r, "销毁量", o.getTotals().getDestroyedCount());

            Sheet s2 = wb.createSheet("分类");
            int rr = 0;
            rr = writeGroup(s2, rr, "按门类", c.getByCategory());
            rr = writeGroup(s2, rr, "按年度", c.getByYear());
            rr = writeGroup(s2, rr, "按来源", c.getBySource());
            rr = writeGroup(s2, rr, "按载体", c.getByCarrier());
            rr = writeGroup(s2, rr, "按密级", c.getBySecurity());
            writeGroup(s2, rr, "按公开状态", c.getByOpenStatus());

            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private int writeGroup(Sheet s, int row, String title, List<StatisticsCategoryResponse.Group> g) {
        createRow(s, row++, title, "");
        if (g != null) {
            for (StatisticsCategoryResponse.Group x : g) {
                createRow(s, row++, x.getLabel(), x.getCount());
            }
        }
        return row + 1;
    }

    private void createRow(Sheet s, int row, String k, Object v) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(k);
        Cell c = r.createCell(1);
        if (v instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(String.valueOf(v));
        }
    }

    private List<StatisticsCategoryResponse.Group> group(String sql) {
        return jdbcTemplate.queryForList(sql).stream().map(row -> new StatisticsCategoryResponse.Group(
                String.valueOf(row.get("k")),
                ((Number) row.get("cnt")).longValue())).toList();
    }

    private long count(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v != null ? v : 0L;
    }
}
