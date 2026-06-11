package com.archive.controller;

import com.archive.common.ErrorCode;
import com.archive.common.R;
import com.archive.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
@Tag(name = "字典接口", description = "前端初始化枚举和配置字典")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping
    @Operation(summary = "获取全部字典")
    public R<Map<String, Object>> getAll() {
        return R.ok(dictionaryService.getAllDictionaries());
    }

    @GetMapping("/{dictCode}")
    @Operation(summary = "获取单个字典")
    public R<Object> getOne(@PathVariable String dictCode) {
        Object dict = dictionaryService.getDictionary(dictCode);
        if (dict == null) {
            return R.fail(ErrorCode.NOT_FOUND, "字典不存在: " + dictCode);
        }
        return R.ok(dict);
    }
}
