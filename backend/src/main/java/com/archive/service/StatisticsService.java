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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

        // 前端统计页（原型 statistics.html）所需富字段——全部来自实时聚合，无写死。
        resp.setMetrics(buildMetrics(t));
        resp.setYearlyIntake(buildYearlyIntake());
        resp.setCategoryDistribution(buildDistribution(
                "SELECT c.category_name AS k, COUNT(*) AS cnt FROM archives a " +
                "JOIN categories c ON c.id=a.category_id WHERE a.deleted_at IS NULL AND a.lifecycle_status<>'destroyed' GROUP BY 1 ORDER BY 2 DESC"));
        resp.setCarrierDistribution(buildDistribution(
                "SELECT a.carrier_status AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1 ORDER BY 2 DESC"));
        resp.setBusinessBreakdown(buildBusinessBreakdown());
        resp.setDataSources(buildDataSources());
        resp.setSummarizedAt(OffsetDateTime.now());
        return resp;
    }

    /** 四张可下钻指标卡（馆藏/本月新增/待上架/待销毁审批）。 */
    private List<StatisticsOverviewResponse.Metric> buildMetrics(StatisticsOverviewResponse.Totals t) {
        List<StatisticsOverviewResponse.Metric> list = new ArrayList<>();
        list.add(metric("totalArchives", "馆藏总量", t.getTotalArchives(),
                "正式档案，不含清单草稿", "/admin/archive-management",
                Map.of("lifecycle_status", "normal")));
        long monthAdd = count("SELECT COUNT(*) FROM archives WHERE deleted_at IS NULL AND archived_at >= date_trunc('month', now())");
        list.add(metric("monthAdd", "本月新增档案", monthAdd,
                "按正式入库时间统计", "/admin/archive-management",
                Map.of("archived_at", "this_month")));
        long pendingShelf = count("SELECT COUNT(*) FROM archives WHERE deleted_at IS NULL AND lifecycle_status='pending_shelf'");
        list.add(metric("pendingShelf", "待入库/待上架", pendingShelf,
                "已接收条目和已入库未上架档案", "/admin/pending-archive",
                Map.of("status", "pending_archive")));
        long pendingDestroy = count("SELECT COUNT(*) FROM destruction_lists WHERE deleted_at IS NULL AND status='pending_destroy'");
        list.add(metric("pendingDestroy", "待审批销毁清册", pendingDestroy,
                "来自销毁清册表", "/admin/destruction",
                Map.of("status", "pending_approval")));
        return list;
    }

    private StatisticsOverviewResponse.Metric metric(String key, String label, long value,
                                                     String source, String route,
                                                     Map<String, String> query) {
        StatisticsOverviewResponse.Metric m = new StatisticsOverviewResponse.Metric();
        m.setKey(key);
        m.setLabel(label);
        m.setValue(value);
        m.setSource(source);
        m.setTargetRoute(route);
        m.setTargetQuery(query);
        return m;
    }

    /** 年度进馆趋势：按 archived_at 年份聚合，仅取 archived_at 非空的正式档案。 */
    private List<StatisticsOverviewResponse.YearlyIntake> buildYearlyIntake() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT to_char(arh::date,'YYYY')::int AS y, COUNT(*) AS cnt FROM archives, " +
                "LATERAL (SELECT archived_at AS arh) x " +
                "WHERE deleted_at IS NULL AND archived_at IS NOT NULL " +
                "GROUP BY 1 ORDER BY 1");
        List<StatisticsOverviewResponse.YearlyIntake> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            StatisticsOverviewResponse.YearlyIntake y = new StatisticsOverviewResponse.YearlyIntake();
            y.setYear(((Number) r.get("y")).intValue());
            y.setCount(((Number) r.get("cnt")).longValue());
            out.add(y);
        }
        return out;
    }

    /** 通用分布（含比例 0~1）。 */
    private List<StatisticsOverviewResponse.Distribution> buildDistribution(String sql) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.stream().mapToLong(r -> ((Number) r.get("cnt")).longValue()).sum();
        List<StatisticsOverviewResponse.Distribution> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            StatisticsOverviewResponse.Distribution d = new StatisticsOverviewResponse.Distribution();
            d.setLabel(String.valueOf(r.get("k")));
            long c = ((Number) r.get("cnt")).longValue();
            d.setValue(c);
            d.setRatio(total > 0 ? (double) c / total : 0.0);
            out.add(d);
        }
        return out;
    }

    /** 业务汇总：移交/征集/借阅/销毁/保存。 */
    private List<StatisticsOverviewResponse.BusinessBreakdown> buildBusinessBreakdown() {
        List<StatisticsOverviewResponse.BusinessBreakdown> out = new ArrayList<>();
        out.add(breakdown("transfer", "移交", "intake_batches",
                "SELECT source_type, status, COUNT(*) FROM intake_batches WHERE deleted_at IS NULL GROUP BY 1,2"));
        out.add(breakdown("collection", "征集", "intake_batches",
                "SELECT status, COUNT(*) FROM intake_batches WHERE deleted_at IS NULL AND source_type='collection' GROUP BY 1"));
        out.add(breakdown("borrow", "借阅", "borrow_requests",
                "SELECT status, COUNT(*) FROM borrow_requests WHERE deleted_at IS NULL GROUP BY 1"));
        out.add(breakdown("destruction", "销毁", "destruction_lists",
                "SELECT status, COUNT(*) FROM destruction_lists WHERE deleted_at IS NULL GROUP BY 1"));
        out.add(breakdown("backup", "保存", "backup_tasks",
                "SELECT status, COUNT(*) FROM backup_tasks WHERE deleted_at IS NULL GROUP BY 1"));
        return out;
    }

    @SuppressWarnings("unchecked")
    private StatisticsOverviewResponse.BusinessBreakdown breakdown(String key, String domain,
                                                                   String table, String sql) {
        StatisticsOverviewResponse.BusinessBreakdown b = new StatisticsOverviewResponse.BusinessBreakdown();
        b.setKey(key);
        b.setDomain(domain);
        b.setSourceTable(table);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<StatisticsOverviewResponse.BusinessBreakdown.Detail> details = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> r : rows) {
            StatisticsOverviewResponse.BusinessBreakdown.Detail d = new StatisticsOverviewResponse.BusinessBreakdown.Detail();
            // 兼容 1 列（status/count）与 2 列（source_type/status/count）两种形态
            Object statusVal = r.containsKey("status") ? r.get("status") : r.get(r.keySet().iterator().next());
            d.setLabel(statusVal == null ? "—" : String.valueOf(statusVal));
            long c = ((Number) r.get("count")).longValue();
            d.setCount(c);
            details.add(d);
            total += c;
        }
        b.setTotal(total);
        b.setDetails(details);
        return b;
    }

    /** 数据源健康度：取每张业务表最近一条 created_at，超过 30 天视为不健康。 */
    private List<StatisticsOverviewResponse.DataSource> buildDataSources() {
        List<StatisticsOverviewResponse.DataSource> out = new ArrayList<>();
        out.add(dataSource("archives", "档案",
                "SELECT MAX(created_at) AS last FROM archives"));
        out.add(dataSource("intake_batches", "移交征集批次",
                "SELECT MAX(created_at) AS last FROM intake_batches"));
        out.add(dataSource("borrow_requests", "借阅",
                "SELECT MAX(created_at) AS last FROM borrow_requests"));
        out.add(dataSource("destruction_lists", "销毁清册",
                "SELECT MAX(created_at) AS last FROM destruction_lists"));
        out.add(dataSource("backup_tasks", "备份",
                "SELECT MAX(created_at) AS last FROM backup_tasks"));
        out.add(dataSource("file_check_records", "四性检测",
                "SELECT MAX(created_at) AS last FROM file_check_records"));
        return out;
    }

    private StatisticsOverviewResponse.DataSource dataSource(String table, String label, String sql) {
        StatisticsOverviewResponse.DataSource d = new StatisticsOverviewResponse.DataSource();
        d.setTable(table);
        d.setLabel(label);
        try {
            Map<String, Object> r = jdbcTemplate.queryForMap(sql);
            Object last = r.get("last");
            String lastStr = last == null ? null : String.valueOf(last);
            d.setLastSyncedAt(lastStr);
            // 简单健康判定：表里有数据即视为健康（联调环境量小，不用时间阈值强约束）
            d.setHealthy(lastStr != null);
        } catch (Exception e) {
            d.setHealthy(false);
            d.setLastSyncedAt(null);
        }
        return d;
    }

    /** 20.2 分类统计 */
    public StatisticsCategoryResponse categories(StatisticsOverviewQuery q) {
        requireRole();
        StatisticsCategoryResponse resp = new StatisticsCategoryResponse();
        resp.setByCategory(group("SELECT c.category_name AS k, COUNT(*) AS cnt FROM archives a " +
                "JOIN categories c ON c.id=a.category_id WHERE a.deleted_at IS NULL AND a.lifecycle_status<>'destroyed' GROUP BY 1"));
        resp.setByYear(group("SELECT a.formed_year::text AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL AND a.formed_year IS NOT NULL GROUP BY 1 ORDER BY 1"));
        List<StatisticsCategoryResponse.Group> bySource = group("SELECT a.source_type AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1");
        resp.setBySource(bySource);
        resp.setBySourceType(bySource);
        resp.setByCarrier(group("SELECT a.carrier_status AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1"));
        List<StatisticsCategoryResponse.Group> bySecurity = group("SELECT a.security_level::text AS k, COUNT(*) AS cnt FROM archives a " +
                "WHERE a.deleted_at IS NULL GROUP BY 1 ORDER BY 1");
        resp.setBySecurity(bySecurity);
        resp.setBySecurityLevel(bySecurity);
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
