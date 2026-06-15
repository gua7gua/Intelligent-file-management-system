package com.archive.controller;

import com.archive.common.R;
import com.archive.dto.request.StatisticsOverviewQuery;
import com.archive.dto.response.StatisticsCategoryResponse;
import com.archive.dto.response.StatisticsOverviewResponse;
import com.archive.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 数据统计接口（M14）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "数据统计", description = "统计总览、分类统计、报表导出")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/api/admin/statistics/overview")
    @Operation(summary = "20.1 统计总览")
    public R<StatisticsOverviewResponse> overview(@ModelAttribute StatisticsOverviewQuery query) {
        return R.ok(statisticsService.overview(query));
    }

    @GetMapping("/api/admin/statistics/categories")
    @Operation(summary = "20.2 分类统计")
    public R<StatisticsCategoryResponse> categories(@ModelAttribute StatisticsOverviewQuery query) {
        return R.ok(statisticsService.categories(query));
    }

    @GetMapping("/api/admin/statistics/export")
    @Operation(summary = "20.3 导出统计报表")
    public ResponseEntity<byte[]> export(@ModelAttribute StatisticsOverviewQuery query) {
        byte[] data = statisticsService.export(query);
        boolean pdf = "pdf".equalsIgnoreCase(query.getFormat());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(pdf ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String name = URLEncoder.encode("档案统计报表." + (pdf ? "pdf" : "xlsx"), StandardCharsets.UTF_8);
        h.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + name);
        h.setContentLength(data.length);
        return ResponseEntity.ok().headers(h).body(data);
    }
}
